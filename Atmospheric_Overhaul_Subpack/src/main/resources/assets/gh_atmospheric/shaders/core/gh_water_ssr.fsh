#version 150

uniform sampler2D DiffuseSampler;   // main color copy (read-only)
uniform sampler2D MainDepth;        // main depth buffer (all geometry)
uniform sampler2D MaskSampler;      // our water mask color (white = water)
uniform sampler2D MaskDepth;        // our water mask depth (the displaced surface)

// Ray matrices: named Ray* because ModelViewMat/ProjMat are auto-pushed
// with the quad's identity matrices by drawWithShader.
uniform mat4 RayProjMat;    // view -> clip (the frame's real projection)
uniform mat4 RayViewMat;    // camera-relative world -> view (pure rotation)
uniform mat4 RayInvProjMat; // NDC -> view
uniform mat4 RayInvViewMat; // view -> camera-relative world (rotation only)

uniform vec3 CameraPos;
uniform vec4 WavePhase;
uniform vec2 WindVec;
uniform vec4 WaterParams;   // x: amplitude (mirrors the surface render)

in vec2 texCoord;

out vec4 fragColor;

/**
 * gh_water_ssr -- HALF-RESOLUTION ray-marched reflections on water.
 *
 * For every screen pixel the MASK pass marked as water:
 *  1. reconstruct the visual water surface in world space from the
 *     mask's own depth (the displaced surface the eye actually sees);
 *  2. build the wave normal from the SAME wave field the surface
 *     renders from (ghWave below, verbatim from the surface shaders),
 *     so reflections ripple exactly in step with the visible waves;
 *  3. reflect the view ray and march it through the world in ~1.7-block
 *     steps, projecting each step back to screen and comparing against
 *     the MAIN depth buffer: terrain, buildings and entities cut the
 *     reflection for free;
 *  4. on a hit: 4-step binary refinement, then sample the main color
 *     at the hit pixel. On a sky-depth sample: the ray escaped upward,
 *     so the hit pixel's color IS the sky (+ our voxel clouds + god
 *     rays) -- the ocean reflects the sky for free. On a miss: alpha 0,
 *     the composite keeps the surface's own look there.
 *
 * Output: reflection color, alpha = 1 while a reflection was found.
 */

// wave model (identical to the surface shaders) -----------------------
float ghWave(vec2 wp) {
    float windLen = clamp(length(WindVec), 0.0, 1.0);
    vec2 dir = windLen > 0.01 ? normalize(WindVec) : vec2(0.7071, 0.7071);
    float choppy = 0.75 + 0.9 * windLen;                // storm = choppier
    mat2 rot = mat2(0.8, -0.6, 0.6, 0.8);               // ~37 deg
    vec2 d1 = dir;
    vec2 d2 = rot * dir;
    vec2 d3 = rot * rot * dir;
    vec2 d4 = transpose(rot) * dir;
    // Phases arrive pre-wrapped from Java (continuous accumulation, no
    // vanilla GameTime day-wrap jump); spatial args are mod-wrapped to
    // 2*pi so sin() never sees a huge argument (GPU range-reduction
    // precision loss at large world coordinates reads as crest/valley
    // twitching).
    float TAU = 6.28318530718;
    float h = 0.0;
    h += 0.050 * sin(mod(dot(wp, d1) * 0.55 + WavePhase.x, TAU));
    h += 0.030 * sin(mod(dot(wp, d2) * 1.10 + WavePhase.y, TAU));
    h += 0.014 * sin(mod(dot(wp, d3) * 2.10 + WavePhase.z, TAU));
    h += 0.006 * sin(mod(dot(wp, d4) * 4.20 + WavePhase.w, TAU));
    return h * choppy * WaterParams.x;
}

// wave normal via finite differences (same as the surface fsh) ---------
vec3 ghNormal(vec2 wp) {
    float eps = 0.10;
    float hL = ghWave(wp - vec2(eps, 0.0));
    float hR = ghWave(wp + vec2(eps, 0.0));
    float hD = ghWave(wp - vec2(0.0, eps));
    float hU = ghWave(wp + vec2(0.0, eps));
    return normalize(vec3(hL - hR, 2.0 * eps, hD - hU));
}

// view-space position of a screen point (from its depth) --------------
vec3 viewPos(vec2 uv, float depth) {
    vec4 ndc = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 v = RayInvProjMat * ndc;
    return v.xyz / v.w;
}

// march tuning ----------------------------------------------------------
const int   MARCH_STEPS   = 24;     // steps across the reach
const int   REFINE_STEPS  = 4;      // binary refinement after a hit
const float MARCH_REACH   = 40.0;   // blocks the reflection ray travels
const float DEPTH_BIAS    = 0.00035;// "in front of the ray" margin

void main() {
    // The mask's value, sampled with LINEAR filtering so the edge of
    // the water fades across the pixel rather than cutting.
    float mask = texture(MaskSampler, texCoord).r;
    if (mask < 0.02) {
        fragColor = vec4(0.0);       // not water: no reflection here
        return;
    }

    // the visual water surface, world space (mask depth = displaced Y)
    vec3 vSurf = viewPos(texCoord, texture(MaskDepth, texCoord).x);
    vec3 wSurf = CameraPos + (RayInvViewMat * vec4(vSurf, 0.0)).xyz;

    vec3 n = ghNormal(wSurf.xz);
    vec3 v = normalize(wSurf - CameraPos);   // camera -> pixel
    vec3 r = reflect(v, n);                  // world-space reflection

    // march in world space, project each step back to the screen
    vec3 p = wSurf + n * 0.10 + r * 0.15;    // start off the surface

    vec2 prevUv = texCoord;
    vec3 prevP = p;
    float prevRayDepth = 0.0;
    bool hit = false;
    vec2 hitUv = texCoord;

    // The step GROWS with the distance from the camera. A constant
    // step cannot resolve anything thin and near: a player standing
    // in a puddle is 0.6 blocks wide, and a 1.7 block step steps
    // straight over them without ever noticing they were there. Near
    // geometry is exactly what a puddle reflects.
    float travelled = 0.0;
    for (int i = 0; i < MARCH_STEPS; i++) {
        float t = float(i + 1) / float(MARCH_STEPS);
        float next = MARCH_REACH * t * t;      // quadratic: fine near, coarse far
        p += r * (next - travelled);
        travelled = next;
        vec4 vClip = RayProjMat * (RayViewMat * vec4(p - CameraPos, 1.0));
        if (vClip.w <= 0.0) continue;        // behind the camera
        vec2 uv = vClip.xy / vClip.w * 0.5 + 0.5;
        float rayDepth = vClip.z / vClip.w * 0.5 + 0.5;

        if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
            // left the screen while heading up: reflect the horizon edge
            if (r.y > 0.0) {
                hitUv = clamp(prevUv, 0.001, 0.999);
                hit = true;
            }
            break;
        }

        float sampleDepth = texture(MainDepth, uv).x;
        if (sampleDepth >= 0.9999) {
            // sky: the ray escaped upward -- that pixel's color is the
            // sky itself (plus our voxel clouds and god rays)
            hitUv = uv;
            hit = true;
            break;
        }
        if (sampleDepth < rayDepth - DEPTH_BIAS) {
            // Geometry in front of the ray point: a candidate hit.
            //
            // The refinement bisects the DISTANCE ALONG THE RAY, in
            // world space. Bisecting the screen position and mixing
            // the two depths is not the same thing: the two ends of
            // the interval are at different distances, and at grazing
            // angles the screen space and the world space have wildly
            // different scales, so mixing the depths lands somewhere
            // that is not on the ray at all. That is what made the
            // reflections look broken and left hard, block-shaped
            // edges.
            vec3 a = prevP;
            vec3 b = p;
            vec2 hitPoint = uv;
            for (int j = 0; j < REFINE_STEPS; j++) {
                vec3 m = (a + b) * 0.5;
                vec4 mc = RayProjMat * (RayViewMat * vec4(m - CameraPos, 1.0));
                if (mc.w <= 0.0) { a = m; continue; }
                vec2 muv = mc.xy / mc.w * 0.5 + 0.5;
                float mrayDepth = mc.z / mc.w * 0.5 + 0.5;
                if (texture(MainDepth, muv).x < mrayDepth - DEPTH_BIAS) {
                    b = m;
                    hitPoint = muv;
                } else {
                    a = m;
                }
            }
            hitUv = hitPoint;
            hit = true;
            break;
        }
        prevUv = uv;
        prevP = p;
        prevRayDepth = rayDepth;
    }

    if (!hit) {
        fragColor = vec4(0.0);       // nothing found: composite keeps the look
        return;
    }
    // The alpha is the mask's own value, not a hard 1. This pass runs
    // at half resolution, so the full-resolution mask is sampled
    // across a pixel and a half: a hard < 0.5 test made the puddle's
    // edge a jagged, aliased line, and the reflection stopped dead
    // along it. Fading out over the edge instead is what makes the
    // borders of a puddle read as a boundary rather than a glitch.
    fragColor = vec4(texture(DiffuseSampler, hitUv).rgb, mask);
}

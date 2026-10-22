#version 150

uniform sampler2D DiffuseSampler;   // main color (read-only: writing goes to scratch)
uniform sampler2D SSRSampler;       // half-res reflection result (alpha = found)
uniform sampler2D MaskSampler;      // water mask color (white = water)
uniform sampler2D MaskDepth;         // water mask depth (displaced surface)

uniform mat4 RayInvProjMat;  // NDC -> view
uniform mat4 RayInvViewMat;  // view -> camera-relative world (rotation only)

uniform vec3 CameraPos;
uniform vec4 WavePhase;
uniform vec2 WindVec;
uniform vec4 WaterParams;    // x: amplitude (mirrors the surface render)
uniform vec4 RefractParams;  // x: refraction px, y: reflection strength
uniform vec2 ScreenSize;

/**
 * How much of the scene the water reflects when you look straight
 * down at it. Pure water is 2%, which is physically right and reads
 * as no reflection at all; this is the wet-surface sheen that keeps a
 * puddle looking like a puddle from above, and it has to be high
 * enough to carry a player.
 */
const float MIN_REFLECTANCE = 0.38;

in vec2 texCoord;

out vec4 fragColor;

/**
 * gh_water_composite -- FULL-RESOLUTION water composite.
 *
 * Runs into a scratch copy of the frame (never read+write the same
 * framebuffer). Non-water pixels pass through untouched; water pixels:
 *
 *  1. REFRACTION (looking down into water): the water pixel's color
 *     (tint + bed beneath) is resampled through the wave-normal tilt --
 *     the bed wobbles through the surface, in step with the waves. This
 *     is the above-water half promised with the wobble round.
 *  2. REFLECTION: the ray-marched SSR result (SSRSampler) is mixed in
 *     with the SAME fresnel the surface shader computes (grazing =
 *     mirror, straight down = clear water showing the refracted bed).
 *
 * Both use the same wave field (ghWave verbatim) and the same fresnel
 * curve as the surface render, so surface and post effects can never
 * disagree.
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

void main() {
    vec3 base = texture(DiffuseSampler, texCoord).rgb;

    // The mask's own value, not a hard test. This is a FULL
    // resolution sample, so the water's edge fades over a texel
    // instead of stopping dead along a hard line.
    float mask = texture(MaskSampler, texCoord).r;
    if (mask < 0.02) {
        fragColor = vec4(base, 1.0);   // not water: untouched copy
        return;
    }

    // water surface, world space (mask depth = the displaced surface)
    vec3 vSurf = viewPos(texCoord, texture(MaskDepth, texCoord).x);
    vec3 wSurf = CameraPos + (RayInvViewMat * vec4(vSurf, 0.0)).xyz;

    vec3 n = ghNormal(wSurf.xz);
    vec3 v = normalize(wSurf - CameraPos);   // camera -> pixel

    // fresnel (same curve as the surface shader)
    float ndv = max(dot(n, -v), 0.0);
    // A wet surface keeps a sheen even when you look straight down at
    // it. Pure water reflects 2% at normal incidence, which is
    // physically right and reads as no reflection at all: from above,
    // the reflection was a tenth of a tenth of the sky and the puddle
    // simply vanished into the ground.
    //
    // The floor is high on purpose. The reflection has to be strong
    // enough to carry a PLAYER -- a dark object in a bright sky at
    // ten percent is nothing at all. Wet asphalt from above is
    // genuinely reflective, so this is not as far from the truth as
    // two percent is.
    float fresnel = MIN_REFLECTANCE + (1.0 - MIN_REFLECTANCE) * pow(1.0 - ndv, 3.0);

    // 1. refraction: the bed (and tint) resampled through the wave tilt
    vec2 refractPx = n.xz * RefractParams.x;
    vec3 body = texture(DiffuseSampler, texCoord + refractPx / ScreenSize).rgb;

    // 2. reflection: the ray-marched result, fresnel-weighted
    vec4 ssr = texture(SSRSampler, texCoord);

    // The mask's value is folded in here as well, at full resolution,
    // so the water's edge fades over a texel rather than cutting.
    vec3 col = mix(body, ssr.rgb, fresnel * RefractParams.y * ssr.a * mask);
    fragColor = vec4(col, 1.0);
}

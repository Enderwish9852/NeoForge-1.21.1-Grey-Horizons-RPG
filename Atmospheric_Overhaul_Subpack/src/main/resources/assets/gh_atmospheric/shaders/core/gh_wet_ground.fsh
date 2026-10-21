#version 150

uniform sampler2D MainDepth;      // the main target's depth
uniform sampler2D ghShadowMap;    // the sun's shadow map, for "is the sky open"

uniform mat4 RayProjMat;
uniform mat4 RayViewMat;
uniform mat4 RayInvProjMat;
uniform mat4 RayInvViewMat;
uniform mat4 SunClipMat;         // world -> the sun's clip space

uniform vec3 CameraPos;
uniform vec2 ScreenSize;
uniform float Time;
uniform float RainWetness;       // 0..1, how hard it is raining
uniform float ShadowValid;       // 1 when the sun's shadow map is usable

in vec2 texCoord;

out vec4 fragColor;

const int   MARCH_STEPS  = 20;
const int   REFINE_STEPS = 4;
const float MARCH_REACH  = 26.0;
const float DEPTH_BIAS   = 0.0004;

/**
 * Where the water gathers.
 *
 * Two octaves, so there are big pools and small ones. The shape is
 * used as a HARD threshold and never as a gradient: a soft gradient
 * spread over a wide area is precisely what reads as a FLOOD rather
 * than as puddles.
 */
float ghPool(vec3 p) {
    float big = sin(p.x * 0.21 + 1.7) * sin(p.z * 0.25 - 0.6) * sin(p.y * 0.15 + 0.9);
    float small = sin(p.x * 0.55 - 2.1) * sin(p.z * 0.49 + 1.3) * sin(p.y * 0.37 - 0.4);
    return big * 0.68 + small * 0.32;
}

/** The world position of a pixel, from the depth buffer. */
vec3 worldPos(vec2 uv, float depth) {
    vec4 clip = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 view = RayInvProjMat * clip;
    view /= view.w;
    return CameraPos + (RayInvViewMat * vec4(view.xyz, 0.0)).xyz;
}

/** The surface normal, from the depth buffer's gradients. */
vec3 normalFromDepth(vec2 uv) {
    vec2 texel = 1.0 / ScreenSize;
    vec3 pL = worldPos(uv - vec2(texel.x, 0.0), texture(MainDepth, uv - vec2(texel.x, 0.0)).x);
    vec3 pR = worldPos(uv + vec2(texel.x, 0.0), texture(MainDepth, uv + vec2(texel.x, 0.0)).x);
    vec3 pD = worldPos(uv - vec2(0.0, texel.y), texture(MainDepth, uv - vec2(0.0, texel.y)).x);
    vec3 pU = worldPos(uv + vec2(0.0, texel.y), texture(MainDepth, uv + vec2(0.0, texel.y)).x);
    return normalize(cross(pU - pD, pR - pL));
}

/**
 * gh_wet_ground -- the puddles on the ground.
 *
 * A puddle is a shallow pool of water standing on the surface, and
 * what you mostly see in it is not the ground but the REFLECTION of
 * everything around it -- the trees above you, the sky, the light. So
 * this marches the reflected view ray against the depth buffer, the
 * same way the water's own reflection pass does, and finds what the
 * puddle is showing.
 *
 * The output is the screen position of the reflection in xy, and how
 * strongly it should show in z. The composite pass mixes it in, so
 * this pass never has to read the frame it is helping to build.
 */
void main() {
    fragColor = vec4(0.0);
    if (RainWetness <= 0.001) return;

    float depth = texture(MainDepth, texCoord).x;
    if (depth >= 0.9999) return;          // the sky: nothing stands on it

    vec3 p = worldPos(texCoord, depth);
    vec3 n = normalFromDepth(texCoord);
    if (n.y < 0.55) return;               // only the faces that point up

    // the pools, and how deep the water stands in them
    float pool = ghPool(p);
    float inPool = smoothstep(0.42, 0.47, pool);
    if (inPool <= 0.001) return;
    float depthOfWater = smoothstep(0.42, 0.62, pool);

    // is the sky open over here? the sun's shadow map answers that: a
    // roof or a canopy puts the ground in shadow in exactly the same
    // way it keeps the rain off it
    if (ShadowValid > 0.5) {
        vec4 clip = SunClipMat * vec4(p, 1.0);
        vec3 ndc = clip.xyz / clip.w;
        vec2 uv = ndc.xy * 0.5 + 0.5;
        float d = ndc.z * 0.5 + 0.5;
        if (uv.x > 0.0 && uv.x < 1.0 && uv.y > 0.0 && uv.y < 1.0 && d > 0.0 && d < 1.0) {
            if (d > texture(ghShadowMap, uv).r + 0.0008) return;
        }
    }

    // the surface of the water is not perfectly flat: a slow wobble,
    // which is what makes the reflection breathe
    vec2 wobble = vec2(
        sin(p.x * 0.9 + p.z * 0.4 + Time * 0.7),
        cos(p.z * 0.8 - p.x * 0.3 - Time * 0.55)) * 0.012;
    n = normalize(n + vec3(wobble.x, 0.0, wobble.y) * 0.25);

    vec3 v = normalize(p - CameraPos);
    vec3 r = reflect(v, n);

    // march the reflected ray through the world
    vec3 q = p + n * 0.08 + r * 0.12;
    float stepLen = MARCH_REACH / float(MARCH_STEPS);
    vec2 prevUv = texCoord;
    float prevRayDepth = 0.0;
    vec2 hitUv = texCoord;
    bool hit = false;

    for (int i = 0; i < MARCH_STEPS; i++) {
        q += r * stepLen;
        vec4 vClip = RayProjMat * (RayViewMat * vec4(q - CameraPos, 1.0));
        if (vClip.w <= 0.0) continue;
        vec2 uv = vClip.xy / vClip.w * 0.5 + 0.5;
        float rayDepth = vClip.z / vClip.w * 0.5 + 0.5;

        if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
            if (r.y > 0.0) { hitUv = clamp(prevUv, 0.001, 0.999); hit = true; }
            break;
        }
        float sampleDepth = texture(MainDepth, uv).x;
        if (sampleDepth >= 0.9999) {
            hitUv = uv; hit = true; break;      // the sky
        }
        if (sampleDepth < rayDepth - DEPTH_BIAS) {
            vec2 a = prevUv; float da = prevRayDepth;
            vec2 b = uv;    float db = rayDepth;
            for (int j = 0; j < REFINE_STEPS; j++) {
                vec2 m = (a + b) * 0.5;
                float dm = mix(da, db, 0.5);
                if (texture(MainDepth, m).x < dm - DEPTH_BIAS) { b = m; db = dm; }
                else { a = m; da = dm; }
            }
            hitUv = b; hit = true; break;
        }
        prevUv = uv;
        prevRayDepth = rayDepth;
    }

    if (!hit) return;

    // how much the reflection shows: a puddle is almost transparent
    // looking straight down, and a mirror at a grazing angle
    float fres = pow(1.0 - max(dot(n, v), 0.0), 4.0);
    float strength = (0.10 + 0.62 * fres) * inPool * RainWetness;
    strength *= mix(0.55, 1.0, depthOfWater);

    fragColor = vec4(hitUv, strength, 1.0);
}

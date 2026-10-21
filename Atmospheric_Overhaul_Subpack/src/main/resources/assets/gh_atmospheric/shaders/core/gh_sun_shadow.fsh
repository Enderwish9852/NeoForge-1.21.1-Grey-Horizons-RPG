#version 150

uniform sampler2D ShadowMap;      // the sun-space depth map
uniform sampler2D MainDepth;      // the main target's depth, to clip the march
uniform mat4 SunViewProjMat;      // world -> sun clip space
uniform mat4 RayInvProjMat;
uniform mat4 RayInvViewMat;
uniform vec3 CameraPos;
uniform vec3 SunDir;              // towards the sun, world space
uniform vec3 ShaftColor;
uniform vec2 DepthRange;          // near, far of the main projection
uniform float ShaftStrength;
uniform float MaxDistance;
uniform float PhaseG;
uniform float FrameJitter;

in vec2 texCoord;

out vec4 fragColor;

const int STEPS = 16;

/**
 * gh_sun_shadow -- TRUE volumetric god rays.
 *
 * The screen-space pass (gh_ray_blur) smears the sun's glow across the
 * screen, which is a 2D trick: it only works where the sun is in frame
 * and what it draws is a smear. This marches the actual view ray
 * through the air and asks, at every step, whether that bit of air is
 * lit or shadowed -- by sampling the sun-space depth map. The result is
 * real light: shafts that are correct from any angle, with the sun
 * behind you, and with any occluder casting a real shadow.
 *
 * The scattering is weighted by the Henyey-Greenstein phase function,
 * which is why the shafts blaze when you look towards the sun and are
 * a soft glow when you look away from it.
 */
float hg(float c, float g) {
    float g2 = g * g;
    float denom = pow(max(1.0 + g2 - 2.0 * g * c, 1.0e-4), 1.5);
    return (1.0 - g2) / (4.0 * 3.14159265 * denom);
}

void main() {
    // the view ray for this pixel
    vec4 near = RayInvProjMat * vec4(texCoord * 2.0 - 1.0, -1.0, 1.0);
    vec3 viewDir = normalize(near.xyz / near.w);
    vec3 ray = normalize(mat3(RayInvViewMat) * viewDir);

    float phase = hg(dot(ray, SunDir), PhaseG);

    // how far the scene actually is along this ray: a wall in front
    // of the camera must not end up with shafts drawn on it
    float sceneDepth = texture(MainDepth, texCoord).r;
    float n = DepthRange.x;
    float f = DepthRange.y;
    float z = sceneDepth * 2.0 - 1.0;
    float sceneDist = (2.0 * n * f) / max(1.0e-4, (f + n - z * (f - n)));

    float reach = min(MaxDistance, max(0.0, sceneDist - 0.25));
    float stepLen = reach / float(STEPS);
    // a per-pixel offset, changed every frame, so the discrete march
    // turns into fine noise instead of concentric rings
    float jitter = fract(sin(dot(texCoord, vec2(12.9898, 78.233))) * 43758.5453 + FrameJitter);

    float lit = 0.0;
    for (int i = 0; i < STEPS; i++) {
        float t = stepLen * (float(i) + jitter);
        vec3 p = CameraPos + ray * t;

        // nudge the sample towards the sun: a normal offset kills the
        // self-shadowing acne without the peter-panning a big depth
        // bias would cause
        vec4 clip = SunViewProjMat * vec4(p + SunDir * 0.25, 1.0);
        vec3 ndc = clip.xyz / clip.w;
        vec2 uv = ndc.xy * 0.5 + 0.5;
        float depth = ndc.z * 0.5 + 0.5;

        if (uv.x > 0.0 && uv.x < 1.0 && uv.y > 0.0 && uv.y < 1.0 && depth > 0.0 && depth < 1.0) {
            float stored = texture(ShadowMap, uv).r;
            if (depth <= stored + 0.0004) {
                lit += 1.0;      // this bit of air sees the sun
            }
        } else {
            lit += 1.0;          // outside the map: no shadow to cast
        }
    }
    lit /= float(STEPS);

    vec3 col = ShaftColor * (lit * phase * ShaftStrength);
    fragColor = vec4(col, 0.0);
}

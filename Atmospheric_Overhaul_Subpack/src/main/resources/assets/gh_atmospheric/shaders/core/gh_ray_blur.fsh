#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 SunScreen;
uniform float RayDensity;
uniform float RayDecay;
uniform float RayExposure;
uniform float ScreenAspect;

in vec2 texCoord;

out vec4 fragColor;

const int STEPS = 48;

/**
 * gh_ray_blur -- the radial accumulation that smears the sun's glow
 * into shafts (GPU Gems 3 style).
 *
 * Each step walks TOWARD the sun and adds what it finds, attenuated by
 * a per-step decay. The step length is a fraction of the distance to
 * the sun, capped, so the sampling stays dense enough not to band when
 * the sun is far off-screen (which is the normal case for most of the
 * day: the sun is only in frame for a short while).
 *
 * 48 steps, not 24: the long-reach pass covers the whole distance to
 * the sun, so with 24 steps the samples were 18 pixels apart and a
 * 30 pixel wide source was hit six times over. The result was moire
 * banding that read as strange parallel streaks across the sky.
 */
void main() {
    // toward the sun, in a pixel-isotropic space
    vec2 delta = (texCoord - SunScreen) * vec2(ScreenAspect, 1.0);
    float dist = length(delta);

    float stepLen = min(dist, 1.5) * (RayDensity / float(STEPS));
    vec2 dir = dist > 1.0e-5 ? -delta / dist : vec2(0.0);
    // back into texCoord space
    vec2 stepVec = vec2(dir.x / ScreenAspect, dir.y) * stepLen;

    vec2 coord = texCoord;
    float illum = 1.0;
    vec3 acc = vec3(0.0);
    for (int i = 0; i < STEPS; i++) {
        coord += stepVec;
        acc += texture(DiffuseSampler, coord).rgb * illum;
        illum *= RayDecay;
    }
    fragColor = vec4(acc * RayExposure, 1.0);
}

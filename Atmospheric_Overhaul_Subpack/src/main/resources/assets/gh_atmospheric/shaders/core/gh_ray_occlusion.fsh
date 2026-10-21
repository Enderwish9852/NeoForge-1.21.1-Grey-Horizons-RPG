#version 150

uniform sampler2D DiffuseSampler;   // the main target's DEPTH texture
uniform sampler2D CloudMap;
uniform vec2 SunScreen;       // the sun's screen position, in texCoord space
uniform float SeedRadius;     // the light source's radius, in screen-height units
uniform float ScreenAspect;
uniform float SeedStrength;   // 1.0 for the real sun, higher when the disc is clipped

in vec2 texCoord;

out vec4 fragColor;

/**
 * gh_ray_occlusion -- builds the light source buffer the radial blur
 * smears into shafts.
 *
 * THE SEED MUST BE SMALL AND BRIGHT. That is what makes the blurred
 * result a set of distinct rays instead of a smooth wash:
 *
 *   - a big, soft source fills the whole buffer evenly, and blurring
 *     an even field gives an even field back: a bright haze over half
 *     the screen, with no rays in it at all;
 *   - a small, bright source is a point, and blurring a point along
 *     the rays towards it draws the rays.
 *
 * So the seed is the sun's own disc (SeedRadius, derived from the
 * projection matrix so it matches the drawn sun exactly) plus a very
 * small, dim halo. The old version had a halo reaching 2.6x the disc
 * and that was what washed the sky out.
 *
 * The seed is masked to pixels whose depth is exactly sky, so terrain
 * and tree canopies cut the light: that is what makes the shafts pass
 * through the gaps between branches.
 */
void main() {
    float depth = texture(DiffuseSampler, texCoord).r;
    vec2 delta = (texCoord - SunScreen) * vec2(ScreenAspect, 1.0);
    float dist = length(delta);

    // the disc: a hard-ish core with a one-pixel-wide soft edge
    float core = 1.0 - smoothstep(SeedRadius * 0.70, SeedRadius * 1.05, dist);
    // a tight, dim halo so the disc is not a hard cut-out
    float halo = 1.0 - smoothstep(SeedRadius, SeedRadius * 1.8, dist);
    float glow = max(core, 0.12 * halo * halo * halo);

    // only sky pixels: terrain and leaves cut the light
    float sky = step(0.99999, depth);
    // our own voxel clouds cut it too
    float open = smoothstep(0.25, 0.75, texture(CloudMap, texCoord).r);

    float lum = glow * sky * open * SeedStrength;
    fragColor = vec4(vec3(lum), lum);
}

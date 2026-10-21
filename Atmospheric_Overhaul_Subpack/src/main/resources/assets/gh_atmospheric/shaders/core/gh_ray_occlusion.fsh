#version 150

uniform sampler2D DiffuseSampler;   // the main target's DEPTH texture
uniform sampler2D CloudMap;
uniform vec2 SunScreen;       // the sun's screen position, in texCoord space
uniform float SeedRadius;     // the light source's radius, in screen-height units
uniform float ScreenAspect;
uniform float SeedStrength;   // 1.0 for the real sun, lower for the anti-solar seed

in vec2 texCoord;

out vec4 fragColor;

/**
 * gh_ray_occlusion -- builds the light source buffer the radial blur
 * smears into shafts.
 *
 * The old version stamped a "glare" disc reaching SunRadius * 10.0,
 * which with the old 0.045 constant meant a blob 45% of the screen
 * height across -- a 90%-of-the-screen white cover that the blur then
 * smeared over everything. That was the "flash bang".
 *
 * Now the seed is the sun's ACTUAL disc (SeedRadius, derived from the
 * projection matrix so it matches the drawn sun exactly) plus a small
 * halo a few times its radius, and nothing more.
 *
 * The seed is masked to pixels whose depth is exactly sky, so terrain
 * and tree canopies cut the light: that is what makes the shafts pass
 * through the gaps between branches.
 *
 * When the sun is behind the camera there is no disc on screen at all,
 * so the Java side switches to a broad, weak seed at the anti-solar
 * point: the shafts then stream in from beyond the screen edge, which
 * is what real crepuscular rays do.
 */
void main() {
    float depth = texture(DiffuseSampler, texCoord).r;
    vec2 delta = (texCoord - SunScreen) * vec2(ScreenAspect, 1.0);
    float dist = length(delta);

    float core = 1.0 - smoothstep(SeedRadius * 0.55, SeedRadius * 1.15, dist);
    float halo = 1.0 - smoothstep(SeedRadius, SeedRadius * 2.6, dist);
    float glow = max(core, 0.30 * halo * halo);

    // only sky pixels: terrain and leaves cut the light
    float sky = step(0.99999, depth);
    // our own voxel clouds cut it too
    float open = smoothstep(0.25, 0.75, texture(CloudMap, texCoord).r);

    float lum = glow * sky * open * SeedStrength;
    fragColor = vec4(vec3(lum), lum);
}

#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 ScreenSize;
uniform float Time;
uniform float DropletAmount;   // 0..1, how much water is on the lens
uniform float DropletSize;     // 0..1, how big the droplets are
uniform float StreamBias;      // 1 = the wind blows into the player, 0 = splashes
uniform float Slush;           // 0..1, the water running off when you surface
uniform float Aspect;

in vec2 texCoord;

out vec4 fragColor;

/** The droplets live on a grid of this many cells across the screen. */
const float GRID = 7.0;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

/**
 * One droplet.
 *
 * Returns: xy = the refraction offset, in screen units; z = how much
 * of the screen the water covers.
 *
 * A DROP IS A LITTLE BALL LENS. The background inside it appears
 * magnified and INVERTED, so the sample point has to move INWARD, and
 * in proportion to how far the pixel is from the centre. The first
 * version normalised the offset instead, so every pixel in the drop
 * shifted by the same amount and the drop turned into a spiky star.
 *
 * The drops sit in a grid, but each one is jittered inside its cell,
 * varies in size, and some cells are empty, so they do not line up
 * into a visible lattice.
 */
vec4 oneDrop(vec2 p, float time, float amount, float size, float stream) {
    vec2 cell = floor(p);
    vec2 f = fract(p) - 0.5;

    float h1 = hash21(cell);
    float h2 = hash21(cell + 17.3);
    float h3 = hash21(cell + 41.7);

    // Most cells have no drop at all, and the fraction that does
    // grows with how wet the lens is: a nearly dry lens has a few
    // scattered drops, a soaked one has a lot.
    if (h3 > 0.85 - 0.6 * amount) return vec4(0.0);

    // how far it has run down, and how fast
    float run = fract(h1 * 7.7 + time * (0.05 + 0.10 * h2) * mix(0.35, 1.7, stream));
    // where it sits inside its cell
    vec2 c = vec2((h2 - 0.5) * 0.8, 0.5 - run);

    // the radius, in cell units: a drop is a few pixels across, not a
    // quarter of the screen
    float r = size * (0.09 + 0.15 * h1) * mix(0.65, 1.0, amount);
    if (r < 0.0015) return vec4(0.0);

    vec2 d = f - c;
    float dist = length(d);

    float inside = smoothstep(r, r * 0.30, dist);
    if (inside <= 0.0) {
        // the trail it leaves running down
        float trail = smoothstep(r * 0.55, 0.0, abs(d.x))
                    * smoothstep(0.0, 0.18, c.y)
                    * (1.0 - smoothstep(0.0, 0.8, c.y))
                    * stream * 0.5;
        return vec4(0.0, 0.0, trail, 0.0);
    }

    // the sample point moves inward, in proportion to the distance
    // from the centre: the background is magnified and flipped
    vec2 offset = -d * 0.55;

    return vec4(offset, inside, 0.0);
}

/**
 * gh_lens_rain -- water on the camera lens.
 *
 * The rain that blows into the player lands on the lens and runs down
 * it, and the world bends behind each drop. When the player comes up
 * out of the water there is a sheet of it running off, which then
 * settles into the ordinary drops.
 */
void main() {
    vec2 uv = texCoord;
    vec2 offset = vec2(0.0);
    float cover = 0.0;

    if (DropletAmount > 0.001) {
        vec2 p = vec2(uv.x * Aspect, uv.y) * GRID;
        vec4 d = oneDrop(p, Time, DropletAmount, DropletSize, StreamBias);
        // the offset is in cell units: back to screen units
        offset += vec2(d.x / Aspect, d.y) / GRID;
        cover = max(cover, d.z);
    }

    if (Slush > 0.001) {
        // the water coming off when you surface: the same drops, but
        // running fast over a screen that is still wet all over
        vec2 p = vec2(uv.x * Aspect, uv.y) * (GRID * 0.65);
        vec4 s = oneDrop(p, Time * 3.4, 1.0, DropletSize * 1.2, 1.0);
        offset += vec2(s.x / Aspect, s.y) / (GRID * 0.65) * Slush;
        cover = max(cover, s.z * Slush);

        // the whole lens is running with water
        offset += vec2(sin(uv.y * 55.0 + Time * 11.0) * 0.005,
                       sin(uv.x * 31.0 - Time * 8.0) * 0.004) * Slush;
        cover = max(cover, Slush * 0.55);
    }

    if (cover < 0.002) {
        fragColor = texture(DiffuseSampler, uv);
        return;
    }

    vec3 col = texture(DiffuseSampler, uv + offset).rgb;
    // a little sheen over the water
    col += cover * 0.05;
    fragColor = vec4(col, 1.0);
}

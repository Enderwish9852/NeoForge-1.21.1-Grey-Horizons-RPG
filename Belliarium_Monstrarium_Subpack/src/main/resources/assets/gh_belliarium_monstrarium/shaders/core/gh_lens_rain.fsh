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

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

/**
 * The water on the lens.
 *
 * Returns the refraction offset in xy and how much of the screen the
 * water covers in z.
 *
 * Each droplet sits in a cell of a grid and slides down it, on its own
 * clock, leaving a trail behind it. The wind decides what happens:
 * when it blows into the player the rain hits the lens and runs DOWN,
 * so the droplets stream and leave long trails; when it blows the other
 * way the rain never reaches the face and all you get is the odd
 * small splash.
 */
vec3 droplets(vec2 uv, float time, float amount, float size, float stream) {
    vec2 p = vec2(uv.x * Aspect, uv.y) * 7.0;
    vec2 cell = floor(p);
    vec2 f = fract(p) - 0.5;

    float h1 = hash21(cell);
    float h2 = hash21(cell + 17.3);
    float h3 = hash21(cell + 41.7);

    // how far it has run down, and how fast
    float run = fract(h1 * 7.7 + time * (0.05 + 0.10 * h2) * mix(0.35, 1.7, stream));
    vec2 c = vec2((h2 - 0.5) * 0.6, 0.5 - run);

    // the size of the droplet, and how much of it there is
    float r = size * (0.16 + 0.24 * h3) * mix(0.6, 1.0, amount);
    if (r < 0.002) return vec3(0.0);

    vec2 d = f - c;
    float dist = length(d);

    // a droplet is a tiny lens: it bends what is behind it
    float inside = smoothstep(r, r * 0.25, dist);
    vec2 offset = normalize(d + 1.0e-4) * inside * 0.022;

    // the trail it leaves running down
    float trail = smoothstep(r * 0.45, 0.0, abs(d.x))
                * smoothstep(0.0, 0.22, c.y)
                * (1.0 - smoothstep(0.0, 0.85, c.y))
                * stream * 0.45;

    return vec3(offset, max(inside, trail));
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
        vec3 d = droplets(uv, Time, DropletAmount, DropletSize, StreamBias);
        offset += d.xy;
        cover = max(cover, d.z);
    }

    if (Slush > 0.001) {
        // the water coming off when you surface: the same drops, but
        // bigger, denser and running down fast, over a screen that is
        // still wet all over
        vec3 s = droplets(uv, Time * 3.2, 1.0, DropletSize * 1.9 + 0.35, 1.0);
        offset += s.xy * Slush;
        cover = max(cover, s.z * Slush);
        // the whole lens is running
        offset += vec2(sin(uv.y * 70.0 + Time * 14.0) * 0.005, 0.0) * Slush;
        cover = max(cover, Slush * 0.55);
    }

    if (cover < 0.002) {
        fragColor = texture(DiffuseSampler, uv);
        return;
    }

    vec3 col = texture(DiffuseSampler, uv + offset).rgb;
    // the water picks up a little light
    col += cover * 0.05;
    fragColor = vec4(col, 1.0);
}

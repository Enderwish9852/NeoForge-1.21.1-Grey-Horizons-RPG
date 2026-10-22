#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 ScreenSize;
uniform float Time;
uniform float SnowAmount;    // 0..1, how much snow is stuck on the lens
uniform float Melt;          // 0..1, how quickly it is melting off
uniform float Aspect;

in vec2 texCoord;

out vec4 fragColor;

/** The flakes live on a grid of this many cells across the screen. */
const float GRID = 11.0;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

/**
 * gh_lens_snow -- snow on the camera lens.
 *
 * The companion to the rain on the lens: in the snow, flakes land on
 * the glass and sit there, and when the snow stops they melt off.
 *
 * Each cell of the grid holds one flake, and it has its own life: it
 * sticks at its own moment as the snow builds up, and it melts at its
 * own moment as the snow lets up. They are not all the same size and
 * they are not all in the middle of their cell, so they do not line up
 * into a visible lattice.
 */
void main() {
    vec4 scene = texture(DiffuseSampler, texCoord);
    if (SnowAmount < 0.002) {
        fragColor = scene;
        return;
    }

    vec2 p = vec2(texCoord.x * Aspect, texCoord.y) * GRID;
    vec2 cell = floor(p);
    vec2 f = fract(p) - 0.5;

    float h1 = hash21(cell);
    float h2 = hash21(cell + 7.3);
    float h3 = hash21(cell + 19.7);

    // it sticks as the snow builds up...
    float stick = smoothstep(h1 * 0.75, h1 * 0.75 + 0.3, SnowAmount);
    // ...and melts off as it lets up
    float melt = smoothstep(h2 * 0.55, h2 * 0.55 + 0.35, 1.0 - SnowAmount + Melt * 0.25);
    float alive = stick * (1.0 - melt);
    if (alive < 0.003) {
        fragColor = scene;
        return;
    }

    // where in its cell it sits, and how big it is
    vec2 c = vec2((h2 - 0.5) * 0.7, (h3 - 0.5) * 0.7);
    // the flakes gather a little towards the edges of the lens
    float edge = smoothstep(0.15, 0.5, length(texCoord - 0.5));
    float r = (0.030 + 0.050 * h3) * alive * (0.75 + 0.5 * edge);
    if (r < 0.0015) {
        fragColor = scene;
        return;
    }

    float d = length(f - c);
    // a soft white blob, a little brighter at its middle
    float flake = smoothstep(r, r * 0.15, d) * alive;
    vec3 snow = vec3(0.93, 0.95, 0.99) * (0.80 + 0.20 * (1.0 - d / max(r, 0.001)));

    fragColor = vec4(mix(scene.rgb, snow, flake), 1.0);
}

#version 150

uniform float DiscRatio;      // sun disc radius / corona radius, in the quad's d-space
uniform vec3  SunColor;       // overall tint (warm red at the horizon, white high up)
uniform float SunBrightness;  // overall strength (rain dim, horizon fade)

in vec2 texCoord;

out vec4 fragColor;

/**
 * gh_sun -- the sun as a real sun, not a white square.
 *
 * The quad is sized so the disc's ANGULAR size is fixed (a few degrees
 * across) no matter the FOV, the aspect ratio or the window size.
 * Inside the quad the shader draws:
 *
 *   - the disc, with a soft limb and limb darkening (the edge of a
 *     real sun is dimmer and redder than its centre);
 *   - a corona, an exponential halo that fades out over a few times
 *     the disc's radius.
 *
 * The old vanilla sun was a hard-edged white square 15 degrees across;
 * this is a round, glowing body.
 */
void main() {
    // d-space: -1..1 across the quad, isotropic in pixels
    vec2 d = (texCoord - 0.5) * 2.0;
    float dist = length(d);

    float x = dist / DiscRatio;                      // 1.0 at the sun's limb
    float core = 1.0 - smoothstep(0.86, 1.03, x);   // the disc, soft edge
    float limb = mix(1.0, 0.48, pow(clamp(x, 0.0, 1.0), 2.0));  // limb darkening

    // corona: exponential halo around the disc
    float corona = 0.28 * exp(-max(dist - DiscRatio, 0.0) * 7.0);

    float light = core * limb + corona * (1.0 - core);

    vec3 hot  = vec3(1.00, 0.97, 0.90);
    vec3 warm = vec3(1.00, 0.66, 0.30);
    vec3 col = mix(warm, hot, core) * SunColor;

    fragColor = vec4(col * light * SunBrightness, 1.0);
}

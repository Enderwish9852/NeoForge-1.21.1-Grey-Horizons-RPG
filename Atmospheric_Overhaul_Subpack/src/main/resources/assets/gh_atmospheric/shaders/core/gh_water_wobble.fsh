#version 150

uniform sampler2D DiffuseSampler;
uniform vec4 WavePhase;
uniform vec2 WindVec;
uniform vec4 WobbleParams;   // x: distortion strength in pixels
uniform vec2 ScreenSize;     // main target size in pixels

in vec2 texCoord;

out vec4 fragColor;

/**
 * gh_water_wobble -- underwater refraction. Resamples the frame through
 * a UV distortion driven by the SAME four wave trains the water surface
 * renders from (WavePhase, pushed by WaterShaderUniforms; spatial
 * rhythms matched to the surface's 0.55/1.10/2.10/4.20 lattice), so the
 * wobble travels with the wind and storms exactly like the surface
 * does. All sin() arguments are mod-wrapped to 2*pi (GPU
 * range-reduction precision). A 3-tap soften kills the hard shimmer
 * edges a single-tap distortion produces, and gives the slight
 * soft-focus real water has.
 *
 * WobbleParams.x = 0.0 makes this a plain copy pass (used for the
 * copy-back onto the main target, so no second shader is needed).
 */
void main() {
    float TAU = 6.28318530718;
    float amp = WobbleParams.x;

    if (amp < 0.001) {
        // plain copy (pass 2): no distortion, no soften
        fragColor = vec4(texture(DiffuseSampler, texCoord).rgb, 1.0);
        return;
    }

    // pixel coordinates keep the ripple isotropic at any aspect ratio
    vec2 px = texCoord * ScreenSize;

    // wind direction in screen space; same default diagonal as the surface
    float windLen = clamp(length(WindVec), 0.0, 1.0);
    vec2 dir = windLen > 0.01 ? normalize(WindVec) : vec2(0.7071, 0.7071);
    mat2 rot = mat2(0.8, -0.6, 0.6, 0.8);               // ~37 deg, same lattice as the surface
    vec2 d1 = dir;
    vec2 d2 = rot * dir;
    vec2 d3 = rot * rot * dir;

    float k1 = dot(px, d1) * 0.055 + WavePhase.x;
    float k2 = dot(px, d2) * 0.110 + WavePhase.y;
    float k3 = dot(px, d3) * 0.210 + WavePhase.z;

    float a1 = sin(mod(k1, TAU));
    float a2 = sin(mod(k2, TAU));
    float a3 = sin(mod(k3, TAU));

    // travel along the wind plus a cross-axis component = ripple, not slide
    vec2 offPx = (d1 * (a1 * 0.60 + a2 * 0.28) + d3 * (a2 * 0.22 + a3 * 0.10)) * amp;

    vec2 uvD  = texCoord + offPx / ScreenSize;
    vec2 uvD2 = texCoord + offPx * 0.55 / ScreenSize;
    vec2 uvD3 = texCoord + offPx * 0.25 / ScreenSize;

    vec3 c = texture(DiffuseSampler, uvD).rgb * 0.60
           + texture(DiffuseSampler, uvD2).rgb * 0.25
           + texture(DiffuseSampler, uvD3).rgb * 0.15;

    fragColor = vec4(c, 1.0);
}

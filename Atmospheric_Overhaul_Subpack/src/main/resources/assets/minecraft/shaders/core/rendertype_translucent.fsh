#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

// gh_atmospheric water uniforms (mirrors of the vertex shader's)
uniform vec4 WavePhase;
uniform vec3 CameraPos;
uniform vec2 WindVec;
uniform vec3 SunDir;
uniform vec4 WaterStillUV;
uniform vec4 WaterFlowUV;
uniform vec4 WaterParams;
uniform float MaskMode;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec3 ghWorldPos;
in float ghWater;

out vec4 fragColor;

// shared wave model (identical to the vertex shader's) ----------------
float ghWave(vec2 wp) {
    float windLen = clamp(length(WindVec), 0.0, 1.0);
    vec2 dir = windLen > 0.01 ? normalize(WindVec) : vec2(0.7071, 0.7071);
    float choppy = 0.75 + 0.9 * windLen;                // storm = choppier
    mat2 rot = mat2(0.8, -0.6, 0.6, 0.8);               // ~37 deg
    vec2 d1 = dir;
    vec2 d2 = rot * dir;
    vec2 d3 = rot * rot * dir;
    vec2 d4 = transpose(rot) * dir;
    // Phases arrive pre-wrapped from Java (continuous accumulation, no
    // vanilla GameTime day-wrap jump); spatial args are mod-wrapped to
    // 2*pi so sin() never sees a huge argument (GPU range-reduction
    // precision loss at large world coordinates reads as crest/valley
    // twitching).
    float TAU = 6.28318530718;
    float h = 0.0;
    h += 0.050 * sin(mod(dot(wp, d1) * 0.55 + WavePhase.x, TAU));
    h += 0.030 * sin(mod(dot(wp, d2) * 1.10 + WavePhase.y, TAU));
    h += 0.014 * sin(mod(dot(wp, d3) * 2.10 + WavePhase.z, TAU));
    h += 0.006 * sin(mod(dot(wp, d4) * 4.20 + WavePhase.w, TAU));
    return h * choppy * WaterParams.x;                  // WaterParams.x = amplitude scale
}

void main() {
    // MASK MODE (WaterReflectionRenderer): the translucent layer is
    // re-rendered as a full-screen water mask -- water faces (with the
    // wave displacement, so mask depth matches the visual surface) draw
    // flat opaque white; every other translucent block (glass, ice,
    // portals) discards. Bypasses color grading AND fog.
    if (MaskMode > 0.5) {
        if (ghWater < 0.5) discard;
        fragColor = vec4(1.0);
        return;
    }

    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;

    if (ghWater > 0.5) {
        // wave normal via finite differences of the same height field
        float eps = 0.10;
        float hL = ghWave(ghWorldPos.xz - vec2(eps, 0.0));
        float hR = ghWave(ghWorldPos.xz + vec2(eps, 0.0));
        float hD = ghWave(ghWorldPos.xz - vec2(0.0, eps));
        float hU = ghWave(ghWorldPos.xz + vec2(0.0, eps));
        vec3 n = normalize(vec3(hL - hR, 2.0 * eps, hD - hU));
        vec3 v = normalize(ghWorldPos - CameraPos);     // toward the pixel

        // fresnel: grazing water turns sky-mirror, top-down stays clear
        float ndv = max(dot(n, -v), 0.0);
        float fresnel = 0.025 + 0.975 * pow(1.0 - ndv, 5.0);

        // sun / moon glint along the wave-normal reflection
        vec3 r = reflect(v, n);
        float g = pow(max(dot(r, SunDir), 0.0), 350.0);
        bool sunUp = SunDir.y > -0.02;
        vec3 glintCol = sunUp
            ? mix(vec3(1.0, 0.60, 0.32), vec3(1.0, 0.98, 0.92),
                  clamp(SunDir.y * 2.5, 0.0, 1.0))
            : vec3(0.62, 0.72, 1.0);
        float glintPow = sunUp ? clamp((SunDir.y + 0.05) / 0.15, 0.0, 1.0) : 0.16;

        // realistic cold slate-grey water: drop the MC blue entirely,
        // keep the texture's luminance as natural shading variation
        float lum = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
        color.rgb = vec3(0.29, 0.35, 0.40) * (0.30 + 0.80 * lum);

        // sky-colored sheen at grazing angles (fog color ~ horizon sky)
        vec3 col = mix(color.rgb, FogColor.rgb, fresnel * 0.62);
        col += glintCol * g * glintPow * (0.30 + 0.70 * fresnel) * WaterParams.y;
        // semi-transparent: the bed shows through looking down,
        // mirror-like at grazing angles (fresnel), never fully opaque
        color = vec4(col, min((0.55 + 0.35 * fresnel) * WaterParams.z, 0.95));
    }

    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}

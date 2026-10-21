#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform sampler2D ghShadowMap;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

uniform float ghRainWetness;   // 0..1, how hard it is raining
uniform float ghShadowValid;   // 1 when the sun's shadow map is usable
uniform mat4 ghSunClip;        // world -> the sun's clip space
uniform vec3 ghSunDir;         // towards the sun
uniform vec3 ghCameraPos;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec3 ghNormal;
in vec3 ghWorldPos;

out vec4 fragColor;

/** A smooth, low frequency variation, so the water pools unevenly. */
float ghPool(vec3 p) {
    return sin(p.x * 0.7) * sin(p.z * 0.9 + 1.3) * sin(p.y * 0.5 + 0.7);
}
void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;

    // ---- the wet look --------------------------------------------------
    // Only the faces that point up collect the rain, and only where
    // the sky is actually open over them. The sun's shadow map says
    // that: a roof or a canopy puts the ground in shadow in exactly
    // the same way it keeps the rain off it.
    float wet = ghRainWetness * step(0.5, ghNormal.y);
    if (wet > 0.001 && ghShadowValid > 0.5) {
        vec4 clip = ghSunClip * vec4(ghWorldPos, 1.0);
        vec3 ndc = clip.xyz / clip.w;
        vec2 uv = ndc.xy * 0.5 + 0.5;
        float d = ndc.z * 0.5 + 0.5;
        if (uv.x > 0.0 && uv.x < 1.0 && uv.y > 0.0 && uv.y < 1.0 && d > 0.0 && d < 1.0) {
            float stored = texture(ghShadowMap, uv).r;
            if (d > stored + 0.0008) {
                wet = 0.0;   // the sun cannot see it, so the rain cannot reach it
            }
        }
    }

    wet *= 0.72 + 0.42 * (ghPool(ghWorldPos) * 0.5 + 0.5);

    if (wet > 0.001) {
        // Water is not a mirror. A wet surface is DARKER and a little
        // deeper in colour, with a broad soft sheen -- not the hard
        // rim of light a mirror throws at grazing angles.
        vec3 base = color.rgb;
        float lum = dot(base, vec3(0.299, 0.587, 0.114));
        base = mix(vec3(lum), base, 1.15);          // a touch richer
        color.rgb = base * mix(1.0, 0.74, wet);

        vec3 n = normalize(ghNormal);
        vec3 v = normalize(ghCameraPos - ghWorldPos);
        vec3 h = normalize(ghSunDir + v);
        // a broad, soft highlight rather than a tight hot spot
        float spec = pow(max(dot(n, h), 0.0), 18.0) * wet;
        color.rgb += spec * vec3(1.0, 0.98, 0.93) * 0.28;
        // and only a whisper of the grazing-angle sheen a mirror has
        float fres = pow(1.0 - max(dot(n, v), 0.0), 6.0);
        color.rgb += fres * wet * 0.022;
    }

    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}

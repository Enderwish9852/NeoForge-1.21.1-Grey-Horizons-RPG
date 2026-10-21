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

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    if (color.a < 0.5) {
        discard;
    }

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

    if (wet > 0.001) {
        // a wet surface is darker than a dry one
        color.rgb *= mix(1.0, 0.70, wet);

        vec3 n = normalize(ghNormal);
        vec3 v = normalize(ghCameraPos - ghWorldPos);
        vec3 h = normalize(ghSunDir + v);
        float spec = pow(max(dot(n, h), 0.0), 64.0) * wet;
        color.rgb += spec * vec3(1.0, 0.98, 0.93) * 0.85;

        // and a sheen at grazing angles, where a wet surface mirrors
        // the whole sky
        float fres = pow(1.0 - max(dot(n, v), 0.0), 5.0);
        color.rgb += fres * wet * 0.10;
    }

    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}

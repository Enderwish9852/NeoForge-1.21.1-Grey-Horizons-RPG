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
uniform float ghTime;          // seconds, for the ripples on the water
uniform mat4 ghSunClip;        // world -> the sun's clip space
uniform vec3 ghSunDir;         // towards the sun
uniform vec3 ghCameraPos;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec3 ghNormal;
in vec3 ghWorldPos;

out vec4 fragColor;

/**
 * A slow, smooth variation across the world, so the water gathers in
 * puddles instead of covering everything evenly.
 */
float ghPool(vec3 p) {
    return sin(p.x * 0.27 + 1.7) * sin(p.z * 0.31 - 0.6) * sin(p.y * 0.19 + 0.9);
}

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    if (color.a < 0.5) {
        discard;
    }

    // ---- the wet look --------------------------------------------------
    // A PUDDLE OF WATER STANDING ON THE BLOCK, not a wet block. It
    // has a surface of its own, which is why the block underneath is
    // seen slightly bent and the sky slides over the top of it.
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

    // the water does not lie evenly: it gathers, and the edges of a
    // puddle are where it thins out. That is what breaks the one
    // perfect sheet, and the depth of the pool is what gives the
    // layer its thickness.
    float pool = ghPool(ghWorldPos) * 0.5 + 0.5;
    float depth = smoothstep(0.30, 0.62, pool);
    wet *= depth;

    if (wet > 0.001) {
        // the surface of the water is not flat: small ripples, which
        // are what make it read as a layer of water
        vec2 ripple = vec2(
            sin(ghWorldPos.x * 2.6 + ghWorldPos.z * 1.1 + ghTime * 1.6),
            cos(ghWorldPos.z * 2.3 - ghWorldPos.x * 0.9 - ghTime * 1.2));

        // The block is seen THROUGH the water, so its texture is looked
        // up slightly off. THE OFFSET MUST BE SUB-TEXEL: the block
        // atlas is one enormous texture, so a whole percent of a UV is
        // dozens of texels and the shader ends up sampling a different
        // block's texture entirely. A fraction of a texel is all the
        // bend a real puddle gives anyway.
        vec3 base = texture(Sampler0, texCoord0 + ripple * 0.0004).rgb
                * vertexColor.rgb * ColorModulator.rgb;

        // deeper water is darker and glossier than the thin edges
        float lum = dot(base, vec3(0.299, 0.587, 0.114));
        base = mix(vec3(lum), base, 1.12);
        base *= mix(1.0, mix(0.86, 0.68, depth), wet);

        vec3 n = normalize(ghNormal + vec3(ripple.x, 0.0, ripple.y) * 0.35);
        vec3 v = normalize(ghCameraPos - ghWorldPos);

        // the sky slides over the top of the water
        float fres = pow(1.0 - max(dot(n, v), 0.0), 4.0);
        base += FogColor.rgb * fres * wet * 0.14;

        // and the sun glints off the ripples
        vec3 h = normalize(ghSunDir + v);
        float spec = pow(max(dot(n, h), 0.0), 40.0) * wet;
        base += spec * vec3(1.0, 0.98, 0.93) * 0.40;

        color.rgb = base;
    }

    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}

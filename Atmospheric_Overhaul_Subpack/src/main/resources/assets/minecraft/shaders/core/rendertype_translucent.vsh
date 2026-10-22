#version 150

#moj_import <light.glsl>
#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 ChunkOffset;
uniform int FogShape;

// gh_atmospheric water uniforms (set by WaterShaderUniforms each frame)
uniform vec4 WavePhase;
uniform vec3 CameraPos;
uniform vec2 WindVec;
uniform vec3 SunDir;
uniform vec4 WaterStillUV;
uniform vec4 WaterFlowUV;
uniform vec4 IceUV;
uniform vec4 WaterParams;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;
out vec3 ghWorldPos;
out float ghWater;
out float ghIce;

// shared wave model (identical in the fragment shader) ---------------
bool ghWaterSprite(vec2 uv) {
    return (uv.x > WaterStillUV.x && uv.x < WaterStillUV.z
         && uv.y > WaterStillUV.y && uv.y < WaterStillUV.w)
        || (uv.x > WaterFlowUV.x && uv.x < WaterFlowUV.z
         && uv.y > WaterFlowUV.y && uv.y < WaterFlowUV.w);
}

// the ice is the other block that wants its own surface treatment
bool ghIceSprite(vec2 uv) {
    return uv.x > IceUV.x && uv.x < IceUV.z
        && uv.y > IceUV.y && uv.y < IceUV.w;
}

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
    vec3 pos = Position + ChunkOffset;
    ghWorldPos = pos + CameraPos;                       // world-space anchor
    ghWater = ghWaterSprite(UV0) ? 1.0 : 0.0;
    ghIce = ghIceSprite(UV0) ? 1.0 : 0.0;
    if (ghWater > 0.5) {
        pos.y += ghWave(ghWorldPos.xz);                // vertical displacement
    }
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    vertexDistance = fog_distance(pos, FogShape);
    vertexColor = Color * minecraft_sample_lightmap(Sampler2, UV2);
    texCoord0 = UV0;
}

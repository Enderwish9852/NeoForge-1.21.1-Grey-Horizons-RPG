#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 SunScreen;
uniform float RayDensity;
uniform float RayDecay;
uniform float RayExposure;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec2 stepVec = (SunScreen - texCoord) * (RayDensity / 24.0);
    vec2 coord = texCoord;
    float illum = 1.0;
    vec3 acc = vec3(0.0);
    for (int i = 0; i < 24; i++) {
        coord += stepVec;
        acc += texture(DiffuseSampler, coord).rgb * illum;
        illum *= RayDecay;
    }
    fragColor = vec4(acc * RayExposure, 1.0);
}

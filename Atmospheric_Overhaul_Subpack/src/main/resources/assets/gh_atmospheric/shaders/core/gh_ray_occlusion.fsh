#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D CloudMap;
uniform vec2 SunScreen;
uniform float SunRadius;
uniform float ScreenAspect;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    float depth = texture(DiffuseSampler, texCoord).r;
    vec2 delta = (texCoord - SunScreen) * vec2(ScreenAspect, 1.0);
    float dist = length(delta);
    float core = 1.0 - smoothstep(SunRadius * 0.35, SunRadius, dist);
    float glare = 1.0 - smoothstep(SunRadius, SunRadius * 10.0, dist);
    float glow = max(core, 0.55 * glare * glare);
    float sky = step(1.0, depth);
    float open = smoothstep(0.25, 0.75, texture(CloudMap, texCoord).r);
    float lum = glow * sky * open;
    fragColor = vec4(vec3(lum), lum);
}

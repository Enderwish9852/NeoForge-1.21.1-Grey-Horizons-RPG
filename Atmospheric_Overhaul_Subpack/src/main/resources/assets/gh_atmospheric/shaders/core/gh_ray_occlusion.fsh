#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 SunScreen;
uniform float SunRadius;
uniform float ScreenAspect;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    float depth = texture(DiffuseSampler, texCoord).r;
    vec2 delta = (texCoord - SunScreen) * vec2(ScreenAspect, 1.0);
    float disc = 1.0 - smoothstep(SunRadius * 0.45, SunRadius, length(delta));
    float sky = step(1.0, depth);
    float lum = disc * sky;
    fragColor = vec4(vec3(lum), lum);
}

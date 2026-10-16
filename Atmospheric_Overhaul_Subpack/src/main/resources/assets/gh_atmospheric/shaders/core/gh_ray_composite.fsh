#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 SunScreen;
uniform vec3 RayColor;
uniform float RayIntensity;
uniform float ScreenAspect;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec3 lum = texture(DiffuseSampler, texCoord).rgb;
    vec2 delta = (texCoord - SunScreen) * vec2(ScreenAspect, 1.0);
    float falloff = 1.0 - smoothstep(0.35, 1.15, length(delta));
    fragColor = vec4(lum * RayColor * RayIntensity * falloff, 0.0);
}

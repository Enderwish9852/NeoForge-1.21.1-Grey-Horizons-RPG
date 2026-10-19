#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 SunScreen;
uniform vec3 RayColor;
uniform float RayIntensity;
uniform float ScreenAspect;
uniform float DebugMode;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    if (DebugMode > 1.5) {
        fragColor = vec4(texture(DiffuseSampler, texCoord).rgb * 2.0, 1.0);
        return;
    }
    if (DebugMode > 0.5) {
        fragColor = vec4(texture(DiffuseSampler, texCoord).rgb, 1.0);
        return;
    }
    vec3 lum = texture(DiffuseSampler, texCoord).rgb;
    vec2 delta = (texCoord - SunScreen) * vec2(ScreenAspect, 1.0);
    float falloff = 1.0 - smoothstep(0.45, 1.35, length(delta));
    vec3 lit = lum * RayColor * RayIntensity * falloff;
    lit = lit / (1.0 + 0.22 * lit);
    fragColor = vec4(lit, 0.0);
}

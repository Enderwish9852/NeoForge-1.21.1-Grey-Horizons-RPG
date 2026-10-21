#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 SunScreen;
uniform vec3 RayColor;
uniform float RayIntensity;
uniform float ScreenAspect;
uniform vec2 FalloffRange;   // x: full strength inside, y: gone beyond
uniform float DebugMode;

in vec2 texCoord;

out vec4 fragColor;

/**
 * gh_ray_composite -- adds the shafts onto the scene.
 *
 * The old version kept FULL strength for everything within 0.45 of the
 * sun in screen-height units -- a 90%-of-the-screen hot area, which is
 * what read as a flash bang. The falloff now starts right at the sun
 * and reaches across the whole screen, and the soft knee keeps the
 * brightest part from clipping to white.
 *
 * FalloffRange is widened by the Java side when the sun is off-screen:
 * the shafts then stay visible right across the frame instead of
 * fading out, which is what "the rays converge off-screen" needs.
 */
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
    float falloff = 1.0 - smoothstep(FalloffRange.x, FalloffRange.y, length(delta));

    vec3 lit = lum * RayColor * RayIntensity * falloff;
    lit = lit / (1.0 + 0.35 * lit);
    fragColor = vec4(lit, 0.0);
}

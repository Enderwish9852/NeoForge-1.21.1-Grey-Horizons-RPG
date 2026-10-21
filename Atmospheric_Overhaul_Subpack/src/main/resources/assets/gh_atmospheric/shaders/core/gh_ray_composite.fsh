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
 * The tone curve is 1 - exp(-lit), which is BOUNDED at 1.0: however
 * bright the shafts get, they can never push more than a full white
 * onto the sky. The old curve (lit / (1 + 0.35 * lit)) topped out at
 * 2.86, so a bright seed could add nearly three times white across a
 * large area -- the "massive light" the user saw.
 *
 * FalloffRange is set by the Java side per case: tighter when the sun
 * is in frame, wider when it is off-screen, so the shafts keep
 * reaching across the frame instead of dying.
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
    // bounded soft knee: can never exceed a full white
    lit = vec3(1.0) - exp(-lit);
    fragColor = vec4(lit, 0.0);
}

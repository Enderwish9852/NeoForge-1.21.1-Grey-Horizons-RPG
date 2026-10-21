#version 150

uniform sampler2D MainDepth;      // the main target's depth
uniform sampler2D ShadowMap;      // the sun-space depth map
uniform mat4 SunViewProjMat;      // world -> sun clip space
uniform mat4 RayInvProjMat;
uniform mat4 RayInvViewMat;
uniform vec3 CameraPos;
uniform vec2 DepthRange;          // near, far of the main projection
uniform float ShadowStrength;     // how much the sun's light is worth
uniform float ShadowTexel;        // 1 / the shadow map's size

in vec2 texCoord;

out vec4 fragColor;

/**
 * gh_sun_ground_shadow -- the sun's shadow on the surfaces.
 *
 * Minecraft's lighting has no sun in it: the sky light is the same
 * whether the sun is shining or not. This puts the sun back, in the
 * only way that can be done to an already-rendered frame: find the
 * world position of each pixel, ask the shadow map whether the sun
 * reaches it, and darken what it does not.
 *
 * One map serves all of it. The map holds the terrain, the leaves AND
 * the clouds, so this is at once the shadow of a tree, the shadow of
 * a roof, and the shadow of a CLOUD -- and the gaps in the clouds are
 * what break the shafts into beams.
 *
 * The output is a factor, and the blend does the multiplying
 * (DEST_COLOR, ZERO), so the frame is never read back.
 */
void main() {
    float depth = texture(MainDepth, texCoord).r;
    if (depth >= 0.99999) {
        // the sky: nothing to shadow
        fragColor = vec4(1.0);
        return;
    }

    // the world position of this pixel
    vec4 near = RayInvProjMat * vec4(texCoord * 2.0 - 1.0, -1.0, 1.0);
    vec3 viewDir = normalize(near.xyz / near.w);
    vec3 ray = normalize(mat3(RayInvViewMat) * viewDir);
    float n = DepthRange.x;
    float f = DepthRange.y;
    float z = depth * 2.0 - 1.0;
    float dist = (2.0 * n * f) / max(1.0e-4, (f + n - z * (f - n)));
    vec3 p = CameraPos + ray * dist;

    // is the sun blocked here? Four taps, so the edge of a shadow is
    // soft rather than stepped.
    float lit = 0.0;
    vec2 offs[4];
    offs[0] = vec2(0.0, 0.0);
    offs[1] = vec2( ShadowTexel,  ShadowTexel);
    offs[2] = vec2(-ShadowTexel,  ShadowTexel);
    offs[3] = vec2( ShadowTexel, -ShadowTexel);
    for (int i = 0; i < 4; i++) {
        vec4 clip = SunViewProjMat * vec4(p, 1.0);
        vec3 ndc = clip.xyz / clip.w;
        vec2 uv = ndc.xy * 0.5 + 0.5 + offs[i];
        float d = ndc.z * 0.5 + 0.5;
        if (uv.x > 0.0 && uv.x < 1.0 && uv.y > 0.0 && uv.y < 1.0 && d > 0.0 && d < 1.0) {
            float stored = texture(ShadowMap, uv).r;
            lit += (d <= stored + 0.0008) ? 1.0 : 0.0;
        } else {
            lit += 1.0;      // outside the map: no shadow to cast
        }
    }
    lit *= 0.25;

    float factor = 1.0 - ShadowStrength * (1.0 - lit);
    fragColor = vec4(vec3(factor), 1.0);
}

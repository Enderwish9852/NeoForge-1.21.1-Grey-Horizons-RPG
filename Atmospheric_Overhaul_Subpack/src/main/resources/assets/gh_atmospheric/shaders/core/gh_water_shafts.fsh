#version 150

uniform mat4 RayInvProjMat;
uniform mat4 RayInvViewMat;
uniform vec3 CameraPos;
uniform vec2 SunScreen;       // the sun's position on screen
uniform float ScreenAspect;
uniform vec3 SunDir;          // the REFRACTED light direction, pointing down into the water
uniform vec3 SunColor;
uniform float SunStrength; // 0 at night, 1 in full sun
uniform float SurfaceY;    // the water surface height
uniform float Time;

in vec2 texCoord;

out vec4 fragColor;

/**
 * The ridges on the surface.
 *
 * The surface is a field of long swells, so the ridges run ACROSS the
 * light's travel, and each one sweeps a sheet of light down through
 * the water. The ridges meander slowly along their length, which is
 * what makes the beams drift instead of standing still.
 */
float ridges(vec2 p, float t, vec2 alongDir) {
    vec2 across = vec2(-alongDir.y, alongDir.x);
    float a = dot(p, across);
    float b = dot(p, alongDir);
    float wander = sin(b * 0.11 + t * 0.40) * 2.0;
    return sin(a * 0.85 + wander + t * 0.28);
}

/**
 * gh_water_shafts -- sunlight coming down through the water surface.
 *
 * THE FIRST TWO VERSIONS OF THIS WERE WRONG, and both failed in the
 * same way: a large patch of light filling the screen.
 *
 * The mistake was integrating a pattern along the view ray. The
 * samples only cover the few blocks of water between the camera and
 * the surface, so every pixel on screen was reading almost the same
 * part of the pattern, and the result was flat. Aiming at the surface
 * point instead gives the surface's own pattern, smeared diagonally
 * -- still not beams.
 *
 * Beams are rays RADIATING FROM THE SUN, and that is a screen-space
 * fact, not a volumetric one: the shafts converge on the sun's
 * position however you look at them. So the rays are built here from
 * the angle around the sun's screen position, and the surface's ridge
 * pattern is what breaks them up and makes them shimmer.
 *
 * The whole effect is one fullscreen pass, with no blur and no
 * marching.
 */
void main() {
    if (SunStrength <= 0.001) {
        fragColor = vec4(0.0);
        return;
    }

    // the view ray, and the point where it reaches the surface
    vec4 near = RayInvProjMat * vec4(texCoord * 2.0 - 1.0, -1.0, 1.0);
    vec3 viewDir = normalize(near.xyz / near.w);
    vec3 ray = normalize(mat3(RayInvViewMat) * viewDir);

    vec2 s = CameraPos.xz;
    if (ray.y > 0.02) {
        float t = (SurfaceY - CameraPos.y) / ray.y;
        s = CameraPos.xz + ray.xz * t;
    }

    // the horizontal direction the light travels in
    vec2 light = SunDir.xz;
    float len = length(light);
    vec2 alongDir = len > 0.001 ? light / len : vec2(1.0, 0.0);

    float ripple = ridges(s, Time, alongDir);

    // the rays radiate from the sun's position on screen
    vec2 d = (texCoord - SunScreen) * vec2(ScreenAspect, 1.0);
    float r = length(d);
    float ang = atan(d.y, d.x);

    // A fan of soft wedges, not a star filter. Five cycles around the
    // circle rather than nine, and squared off so each one is narrow
    // but has no hard edge.
    float spokes = 0.5 + 0.5 * sin(ang * 5.0 + ripple * 2.6 + Time * 0.22);
    spokes *= spokes;

    // The surface decides how much light comes through each gap. This
    // is what makes the fan look like it came through a rippled
    // surface rather than off a lens: whole wedges go dark, and the
    // ones that survive are uneven.
    float gate = smoothstep(-0.35, 0.85, ripple);

    // They fade out fast. The old falloff still had a quarter of its
    // strength at the far corner of the frame, which is what filled
    // the screen.
    float falloff = exp(-r * 3.2) * smoothstep(0.02, 0.10, r);

    float shaft = spokes * gate * falloff;

    // The sun through the water is a SOFT glow. The old version had a
    // hard white core the size of a coin, and that reads as a lens
    // flare rather than as a sun under a surface.
    float glow = exp(-(r * r) / 0.020);
    float core = exp(-(r * r) / 0.0022);
    float sunThroughWater = glow * 0.55 + core * 0.30;

    // the deeper the camera is, the less of the sun gets through
    float depthBelow = max(0.0, SurfaceY - CameraPos.y);
    float depthFade = exp(-depthBelow * 0.16);

    fragColor = vec4(SunColor * ((shaft * 1.1 + sunThroughWater) * depthFade * SunStrength * 1.4), 0.0);
}

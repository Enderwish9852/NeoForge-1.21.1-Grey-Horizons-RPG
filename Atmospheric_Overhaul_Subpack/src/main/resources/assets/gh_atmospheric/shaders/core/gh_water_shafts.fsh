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

    // the wedges, wobbled by the surface's ridges so the fan shimmers
    float wedges = 0.5 + 0.5 * sin(ang * 9.0 + ripple * 3.4 + Time * 0.25);

    // brightest at the sun, gone by the edge of the frame. The little
    // hole at the centre is deliberate: the wedges would otherwise
    // converge to nothing right on the sun and alias.
    float falloff = exp(-r * 1.5) * smoothstep(1.7, 0.15, r) * smoothstep(0.0, 0.05, r);

    // the deeper the camera is, the less of the sun gets through
    float depthBelow = max(0.0, SurfaceY - CameraPos.y);
    float depthFade = exp(-depthBelow * 0.10);

    // the ridges decide how much light comes through each wedge
    float shaft = wedges * falloff * (0.28 + 0.72 * (ripple * 0.5 + 0.5)) * depthFade;

    // THE SUN ITSELF, seen through the water. Underwater the surface
    // is drawn over the sky, so the sun drawn in the sky pass is
    // completely hidden: without this the rays would radiate from a
    // point where nothing is, which is what made the first version of
    // this look wrong. The disc is soft and dimmed, the way a sun
    // through water actually is.
    float glow = exp(-(r * r) / 0.0060);
    float core = exp(-(r * r) / 0.0006);
    float sunThroughWater = (glow * 0.42 + core * 0.85) * depthFade;

    fragColor = vec4(SunColor * ((shaft * 0.9 + sunThroughWater) * SunStrength * 1.5), 0.0);
}

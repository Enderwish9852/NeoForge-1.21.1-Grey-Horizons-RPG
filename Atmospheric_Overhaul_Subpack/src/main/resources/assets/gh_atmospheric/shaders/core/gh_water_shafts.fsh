#version 150

uniform mat4 RayInvProjMat;
uniform mat4 RayInvViewMat;
uniform vec3 CameraPos;
uniform vec3 SunDir;       // the REFRACTED light direction, pointing down into the water
uniform vec3 SunColor;
uniform float SunStrength; // 0 at night, 1 in full sun
uniform float SurfaceY;    // the water surface height
uniform float Time;

in vec2 texCoord;

out vec4 fragColor;

const int STEPS = 14;

/**
 * The ridges on the surface.
 *
 * The first version made a field of blobs, and the result was patches
 * of light on the surface and nothing else: a blob is not a beam.
 *
 * What makes a beam is a RIDGE -- a long line on the surface -- with
 * the light travelling along a direction perpendicular to it. The
 * surface is a field of long swells, so the ridges run across the
 * light's travel, and each one sweeps a sheet of light down through
 * the water. The ridges meander slowly along their length, which is
 * what makes the beams wobble instead of standing still.
 */
float ridges(vec2 p, float t, vec2 alongDir) {
    vec2 across = vec2(-alongDir.y, alongDir.x);
    float a = dot(p, across);
    float b = dot(p, alongDir);
    // a slow meander along the ridge, so the beams drift
    float wander = sin(b * 0.11 + t * 0.40) * 2.0;
    return sin(a * 0.85 + wander + t * 0.28);
}

/**
 * gh_water_shafts -- sunlight coming down through the water surface.
 *
 * Real underwater light shafts are the sun's light refracted at the
 * surface, broken into bands by the ripples on it, and scattered by
 * the water itself. This does the same thing, cheaply:
 *
 *   - the view ray is rebuilt from the inverse projection and view
 *     matrices;
 *   - a handful of samples are taken along it through the water;
 *   - for each sample the shader walks BACK UP the refracted sun ray
 *     to find the point on the surface where that light entered, and
 *     reads the ridge pattern there;
 *   - the deeper the water the light had to travel, the more of it
 *     the water absorbed, so looking down fades out and looking up
 *     towards the surface glows.
 *
 * THE PATTERN HAS TO BE COARSE ENOUGH FOR THE SAMPLING TO RESOLVE.
 * The samples are spread along the view ray, and each one lands on a
 * different point of the surface, so a pattern with fine features is
 * averaged away into a flat screen-wide glow. The ridges are about
 * seven blocks apart here, and there are 14 samples along the ray, so
 * the structure survives.
 */
void main() {
    if (SunStrength <= 0.001) {
        fragColor = vec4(0.0);
        return;
    }

    // the view ray for this pixel
    vec4 near = RayInvProjMat * vec4(texCoord * 2.0 - 1.0, -1.0, 1.0);
    vec3 viewDir = normalize(near.xyz / near.w);
    vec3 dir = normalize(mat3(RayInvViewMat) * viewDir);

    // the horizontal direction the light travels in
    vec2 light = SunDir.xz;
    float len = length(light);
    vec2 alongDir = len > 0.001 ? light / len : vec2(1.0, 0.0);

    float depth = max(0.0, SurfaceY - CameraPos.y);
    float reach = min(depth, 16.0);
    float stepLen = reach / float(STEPS);

    float acc = 0.0;
    for (int i = 0; i < STEPS; i++) {
        vec3 p = CameraPos + dir * (stepLen * (float(i) + 0.5));
        float below = SurfaceY - p.y;
        if (below <= 0.0) continue;

        // back up the refracted sun ray to the surface point that lit
        // this bit of water
        float u = (p.y - SurfaceY) / min(SunDir.y, -0.05);
        vec2 s = p.xz - SunDir.xz * u;

        float v = ridges(s, Time, alongDir);
        // narrow, bright ridges with dark gaps between them: that is
        // what reads as a beam rather than a patch
        float bands = pow(clamp(v * 0.5 + 0.5, 0.0, 1.0), 4.0);
        // the water absorbs what it scatters on the way down
        acc += bands * exp(-u * 0.10);
    }
    acc /= float(STEPS);

    vec3 lit = SunColor * (acc * SunStrength * 1.6);
    fragColor = vec4(lit, 0.0);
}

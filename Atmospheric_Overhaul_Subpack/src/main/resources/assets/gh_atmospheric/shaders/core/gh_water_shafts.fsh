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

const int STEPS = 12;

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
 *     reads the caustic pattern there;
 *   - the deeper the water the light had to travel, the more of it
 *     the water absorbed, so looking down fades out and looking up
 *     towards the surface glows.
 *
 * The bands come from the interference pattern of three sine layers
 * (a stand-in for the real caustic network) evaluated in WORLD space,
 * so the pattern is locked to the water and moves with the swell.
 *
 * THE PATTERN HAS TO BE COARSE ENOUGH FOR THE SAMPLING TO RESOLVE.
 * The samples are spread along the view ray, and each one lands on a
 * different point of the surface, so a pattern with fine features is
 * averaged away into a flat screen-wide glow -- which is what the
 * first version did. The wavelengths here are 5 to 10 blocks, and
 * there are 12 samples along the ray, so the structure survives.
 */
/**
 * The interference pattern standing in for the surface caustics.
 *
 * Time is in SECONDS, not ticks. With the game clock in ticks the
 * phase advanced by a radian every tick, which dragged the pattern
 * across the water at over ten blocks a second -- it read as random
 * light patches flashing on and off rather than as light through a
 * rippling surface.
 */
float caustic(vec2 p, float t) {
    float v = 0.0;
    v += sin(p.x * 0.85 + t * 1.00) * sin(p.y * 0.65 - t * 0.85);
    v += sin((p.x + p.y) * 0.45 - t * 0.65) * sin((p.x - p.y) * 0.55 + t * 0.45);
    v += sin(p.x * 1.15 - t * 1.30) * sin(p.y * 1.05 + t * 1.10);
    return v / 3.0;
}

void main() {
    if (SunStrength <= 0.001) {
        fragColor = vec4(0.0);
        return;
    }

    // the view ray for this pixel
    vec4 near = RayInvProjMat * vec4(texCoord * 2.0 - 1.0, -1.0, 1.0);
    vec3 viewDir = normalize(near.xyz / near.w);
    vec3 dir = normalize(mat3(RayInvViewMat) * viewDir);

    float depth = max(0.0, SurfaceY - CameraPos.y);
    float reach = min(depth, 14.0);
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

        float c = caustic(s, Time);
        float bands = pow(clamp(c * 0.5 + 0.5, 0.0, 1.0), 3.0);
        // the water absorbs what it scatters on the way down
        acc += bands * exp(-u * 0.10);
    }
    acc /= float(STEPS);

    vec3 lit = SunColor * (acc * SunStrength * 1.4);
    fragColor = vec4(lit, 0.0);
}

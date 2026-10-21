#version 150

uniform mat4 RayInvProjMat;
uniform mat4 RayInvViewMat;
uniform vec3 CameraPos;
uniform vec3 SunDir;       // the REFRACTED light direction, pointing down into the water
uniform vec3 SunColor;
uniform float SunStrength; // 0 at night, 1 in full sun
uniform float SurfaceY;    // the water surface height
uniform vec4 WavePhase;    // the surface's wave phases, from the water shader
uniform vec2 WindVec;      // the wind the waves are made of
uniform float WaterParams; // 1 when the surface is water

in vec2 texCoord;

out vec4 fragColor;

const int STEPS = 16;
const float TAU = 6.28318530718;

/**
 * The wave field, copied VERBATIM from the surface shaders
 * (gh_water_ssr.fsh), so the caustics belong to the waves the eye
 * can actually see. The same phases, the same wind, the same chop.
 */
void waveField(vec2 wp, out float h, out float lap) {
    float windLen = clamp(length(WindVec), 0.0, 1.0);
    vec2 dir = windLen > 0.01 ? normalize(WindVec) : vec2(0.7071, 0.7071);
    float choppy = 0.75 + 0.9 * windLen;
    mat2 rot = mat2(0.8, -0.6, 0.6, 0.8);
    vec2 d1 = dir;
    vec2 d2 = rot * dir;
    vec2 d3 = rot * rot * dir;
    vec2 d4 = transpose(rot) * dir;

    h = 0.0;
    lap = 0.0;
    // each train: a sine, and the same sine's Laplacian (-A k^2 sin)
    float a = 0.050, k = 0.55, ph = WavePhase.x;
    h += a * sin(mod(dot(wp, d1) * k + ph, TAU));
    lap -= a * k * k * sin(mod(dot(wp, d1) * k + ph, TAU));
    a = 0.030; k = 1.10; ph = WavePhase.y;
    h += a * sin(mod(dot(wp, d2) * k + ph, TAU));
    lap -= a * k * k * sin(mod(dot(wp, d2) * k + ph, TAU));
    a = 0.014; k = 2.10; ph = WavePhase.z;
    h += a * sin(mod(dot(wp, d3) * k + ph, TAU));
    lap -= a * k * k * sin(mod(dot(wp, d3) * k + ph, TAU));
    a = 0.006; k = 4.20; ph = WavePhase.w;
    h += a * sin(mod(dot(wp, d4) * k + ph, TAU));
    lap -= a * k * k * sin(mod(dot(wp, d4) * k + ph, TAU));

    h *= choppy * WaterParams;
    lap *= choppy * WaterParams;
}

/**
 * The brightness of the light coming through the surface at a point.
 *
 * A rippled surface focuses light: where the surface is CONCAVE the
 * refracted rays converge and it is bright, where it is convex they
 * spread and it is dim. The Laplacian of the height field is exactly
 * that measure, so the caustics fall out of the wave field rather
 * than being painted on.
 */
float ghFocus(vec2 wp) {
    float h, lap;
    waveField(wp, h, lap);
    // The Laplacian of this wave field peaks at about 0.22, so a scale
    // of 1.5 swings the brightness by roughly a third: bright in the
    // troughs where the surface gathers the light, dim on the crests
    // where it spreads out again.
    return max(0.0, 1.0 - lap * 1.5);
}

/** The Henyey-Greenstein phase: light scatters much more towards the eye. */
float hg(float c, float g) {
    float g2 = g * g;
    float denom = pow(max(1.0 + g2 - 2.0 * g * c, 1.0e-4), 1.5);
    return (1.0 - g2) / (4.0 * 3.14159265 * denom);
}

/**
 * gh_water_shafts -- sunlight coming down through the water surface.
 *
 * This is the honest version: it marches the water and works out
 * where the light in each bit of it came from.
 *
 *   - the eye's ray is rebuilt from the inverse projection and view
 *     matrices;
 *   - where that ray meets the surface, the light coming through is
 *     read from the surface's own focusing, and dimmed by the water
 *     between the surface and the eye;
 *   - the water column in front of the eye is marched, and at every
 *     step the shader walks BACK UP the refracted sun ray to find the
 *     point on the surface whose light is in that bit of water, and
 *     reads the focusing there.
 *
 * The beams are not drawn. They are what falls out of the second
 * term: when the eye looks towards the sun, the surface point that
 * feeds a given bit of water barely moves along the ray, so the
 * focusing is coherent and the ray picks up a whole streak of it at
 * once. Look away from the sun and the same point smears out into a
 * smooth glow. That is the difference between beams and a wash, and
 * it is the reason the earlier attempts failed: they integrated a
 * pattern along the ray that had nothing to do with where the light
 * actually came from.
 *
 * The first term is what draws the fan. The focusing on the surface
 * is a set of streaks, and streaks on a plane seen in perspective
 * converge on the vanishing point, which is the sun's direction.
 */
void main() {
    if (SunStrength <= 0.001) {
        fragColor = vec4(0.0);
        return;
    }

    // the eye's ray
    vec4 near = RayInvProjMat * vec4(texCoord * 2.0 - 1.0, -1.0, 1.0);
    vec3 viewDir = normalize(near.xyz / near.w);
    vec3 ray = normalize(mat3(RayInvViewMat) * viewDir);

    // SunDir is the direction the light TRAVELS; the sun is the other
    // way round, and that is where the light comes from.
    vec3 toSun = -SunDir;

    // forward scattering: the shafts blaze when you look towards the
    // sun and are a soft glow when you look away
    float phase = hg(dot(ray, toSun), 0.55);

    const float ABSORB = 0.055;   // the water's absorption, per block
    const float REACH = 30.0;     // how far the march looks

    float acc = 0.0;

    // ---- the surface, seen from below ---------------------------------
    if (ray.y > 0.02) {
        float t = (SurfaceY - CameraPos.y) / ray.y;
        vec3 s = CameraPos + ray * t;

        // the point where the eye's line in the sun's direction meets
        // the surface: the sun's own patch of light
        float u = (SurfaceY - CameraPos.y) / max(-SunDir.y, 0.05);
        vec3 sunPatch = CameraPos + toSun * u;
        float d = length(s.xz - sunPatch.xz);
        // the sun through the surface is a soft glow, wider with the
        // distance, never a hard disc
        float radius = 0.045 * t + 0.04;
        float glow = exp(-(d * d) / (radius * radius));

        // the surface's own focusing, and the water in between
        acc += glow * ghFocus(s.xz) * exp(-t * ABSORB) * 0.55;
    }

    // ---- the water column in front of the eye -------------------------
    float reach = REACH;
    if (ray.y > 0.02) {
        reach = min(reach, (SurfaceY - CameraPos.y) / ray.y);
    }
    if (reach > 0.05) {
        float stepLen = reach / float(STEPS);
        for (int i = 0; i < STEPS; i++) {
            vec3 p = CameraPos + ray * (stepLen * (float(i) + 0.5));
            if (p.y > SurfaceY) continue;
            float below = SurfaceY - p.y;
            // the path the light travelled down through the water
            float pathLen = below / max(-SunDir.y, 0.05);
            // the surface point whose light is in this bit of water
            vec2 feed = p.xz - SunDir.xz * pathLen;
            acc += ghFocus(feed) * exp(-pathLen * ABSORB) * stepLen * 0.020;
        }
    }

    fragColor = vec4(SunColor * (acc * phase * SunStrength * 1.1), 0.0);
}

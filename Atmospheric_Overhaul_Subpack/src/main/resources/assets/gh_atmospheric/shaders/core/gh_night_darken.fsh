#version 150

uniform float NightFactor;   // 1 by day, less at night

in vec2 texCoord;

out vec4 fragColor;

/**
 * gh_night_darken -- the night, darker than vanilla.
 *
 * The world is multiplied by a factor that falls as the sun goes down.
 * The god rays are added AFTER this pass, so they keep their full
 * brightness: that is the whole point. A dark sky with cold shafts in
 * it reads as night; the same shafts over a vanilla-bright night read
 * as nothing at all.
 *
 * The output is the factor and the blend does the multiplying
 * (DST_COLOR, ZERO), so the frame is never read back.
 */
void main() {
    fragColor = vec4(vec3(NightFactor), 1.0);
}

#version 150

uniform sampler2D ReflectionSampler;   // the reflected screen position + strength
uniform sampler2D DiffuseSampler;      // the scene

in vec2 texCoord;

out vec4 fragColor;

/**
 * gh_wet_composite -- puts the puddle's reflection into the frame.
 *
 * The water is ALMOST TRANSPARENT: what you see in a puddle is mostly
 * the reflection of what is around it, and only a little of the
 * ground underneath. So this mixes the reflection over the scene
 * rather than replacing it, and the blend does the mixing -- the
 * shader only has to say WHERE the reflection is and HOW MUCH of it
 * to show.
 *
 * The reflection's screen position carries a slight wobble, which is
 * the surface of the water moving.
 */
void main() {
    vec4 refl = texture(ReflectionSampler, texCoord);
    if (refl.z <= 0.001) {
        fragColor = vec4(0.0);
        return;
    }
    vec3 mirrored = texture(DiffuseSampler, refl.xy).rgb;
    fragColor = vec4(mirrored, clamp(refl.z, 0.0, 1.0));
}

#version 150

uniform sampler2D ReflectionSampler;   // the reflected colour and its strength
uniform sampler2D DiffuseSampler;      // the scene
uniform float DebugMode;               // 0 off, 4 shows the reflection alone

in vec2 texCoord;

out vec4 fragColor;

/**
 * gh_wet_composite -- puts the puddle's reflection into the frame.
 *
 * The water is ALMOST TRANSPARENT: what you see in a puddle is mostly
 * the reflection of what is around it, and only a little of the
 * ground underneath. So this mixes the reflection over the scene
 * rather than replacing it, and the blend does the mixing -- the
 * shader only has to say HOW MUCH of the reflection to show.
 *
 * It never reads the frame: the march already sampled the scene into
 * the scratch, so there is no feedback of the buffer this writes to.
 */
void main() {
    vec4 refl = texture(ReflectionSampler, texCoord);
    if (DebugMode > 3.5) {
        fragColor = vec4(refl.rgb, 1.0);
        return;
    }
    if (refl.a <= 0.002) {
        fragColor = vec4(0.0);
        return;
    }
    fragColor = vec4(refl.rgb, clamp(refl.a, 0.0, 1.0));
}

#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 ScreenSize;

in vec2 texCoord;

out vec4 fragColor;

/**
 * gh_lens_copy -- a plain copy.
 *
 * The lens effect samples the frame and writes a distorted version of
 * it, and a fragment shader must never sample the buffer it writes to.
 * So the distorted frame goes into a scratch target and this puts it
 * back.
 */
void main() {
    fragColor = vec4(texture(DiffuseSampler, texCoord).rgb, 1.0);
}

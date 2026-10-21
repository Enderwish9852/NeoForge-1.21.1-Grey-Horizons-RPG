#version 150

in vec3 Position;
in vec2 UV0;

uniform vec2 QuadCenter;    // NDC centre of the quad (-1..1)
uniform vec2 QuadHalfSize; // NDC half-extents of the quad

out vec2 texCoord;

/**
 * gh_screen_quad -- a screen-space quad placed by NDC centre and
 * half-extents. The sun must stay a perfect CIRCLE on screen at any
 * FOV and aspect ratio, which a world-space quad (like vanilla's
 * 60x60 quad at distance 100) cannot do: perspective makes it an
 * ellipse and the size then depends on the projection. Drawing the
 * sun in NDC keeps it round and FOV-correct by construction.
 *
 * gl_Position.z = 0 -> window depth 0.5, and the caller runs the pass
 * with the depth write off, so the sun sits on the sky and the terrain
 * (drawn later) covers it.
 */
void main() {
    gl_Position = vec4(QuadCenter + Position.xy * QuadHalfSize, 0.0, 1.0);
    texCoord = UV0;
}

#version 150

#moj_import <light.glsl>
#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 ChunkOffset;
uniform int FogShape;
uniform vec3 ghCameraPos;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;
out vec3 ghNormal;
out vec3 ghWorldPos;

void main() {
    vec3 pos = Position + ChunkOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    vertexDistance = fog_distance(pos, FogShape);
    vertexColor = Color * minecraft_sample_lightmap(Sampler2, UV2);
    texCoord0 = UV0;

    // The face's normal and where this face is in the world. The wet
    // look needs both: the normal for the sheen, and the position to
    // ask the sun's shadow map whether the sky is open over it.
    //
    // pos is camera relative (ChunkOffset is the block's position minus
    // the camera's), so the world position is pos plus the camera.
    ghNormal = Normal;
    ghWorldPos = pos + ghCameraPos;
}

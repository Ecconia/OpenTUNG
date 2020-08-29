#version 330 core

layout(location = 0) in vec2 inPosition;
layout(location = 1) in vec2 textureCoords;

uniform mat4 projection;

uniform vec2 size;
uniform vec2 offset;

out vec2 tTextureCoords;

void main()
{
	tTextureCoords = textureCoords;
	gl_Position = projection * vec4(inPosition.x * size.x + offset.x, inPosition.y * size.y + offset.y, 0.0, 1.0);
}

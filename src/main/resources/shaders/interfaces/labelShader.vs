#version 330 core

layout(location = 0) in vec2 inPosition;
layout(location = 1) in vec2 textureCoords;

uniform mat4 projection;
uniform vec3 model;

out vec2 tTextureCoords;

void main()
{
	vec2 position = model.xy;
	float scale = model.z;
	
	tTextureCoords = textureCoords;
	gl_Position = projection * vec4(inPosition * scale + position, 0.0, 1.0);
}

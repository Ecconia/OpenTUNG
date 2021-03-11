#version 330 core

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec3 inNormal;
layout(location = 2) in vec2 inTextureCoord;
layout(location = 3) in vec3 inColor;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

out vec3 tPosition;
out vec3 tNormal;
out vec3 tColor;
out vec2 tTextureCoords;

void main()
{
	vec4 transformedPos = view * model * vec4(inPosition, 1.0);
	
	gl_Position = projection * transformedPos;
	
	tPosition = transformedPos.xyz;
	tNormal = normalize((inverse(transpose(view * model)) * vec4(inNormal, 0.0)).xyz);
	tColor = inColor;
	tTextureCoords = inTextureCoord;
}

#version 330 core

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec2 inTexture;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

out vec3 tPosition;
out vec3 tNormal;
out vec2 tTextureCoords;

void main()
{
	vec3 inNormal = vec3(0, 1, 0);
	vec4 pos = vec4(inPosition.x, inPosition.y + 0.001, inPosition.z, 1.0);
	
	mat4 transformMat = view * model;
	vec4 transformedPos = transformMat * pos;
	gl_Position = projection * transformedPos;//The position in projection system, to be use for placement
	tPosition = transformedPos.xyz;//The position in camera system, to be use for light calculation
	tNormal = normalize((inverse(transpose(transformMat)) * vec4(inNormal, 0.0)).xyz);
	
	tTextureCoords = inTexture;
}

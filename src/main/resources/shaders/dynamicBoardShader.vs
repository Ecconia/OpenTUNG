#version 330 core

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec3 inNormal;
layout(location = 2) in vec2 inTexture;
layout(location = 3) in float inIsSideMultiplicator;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

uniform vec2 size;
uniform vec4 color;

out vec4 tColor;
out vec3 tPosition;
out vec3 tNormal;
out vec2 tTextureCoords;

void main()
{
	vec4 pos = vec4(inPosition.x * size.x * 0.15, inPosition.y, inPosition.z * size.y * 0.15, 1.0);

	tColor = color;

	mat4 transformMat = view * model;
	vec4 transformedPos = transformMat * pos;
	gl_Position = projection * transformedPos; //The position in projection system, to be use for placement
	tPosition = transformedPos.xyz; //The position in camera system, to be use for light calculation
	tNormal = normalize((inverse(transpose(transformMat)) * vec4(inNormal, 0.0)).xyz);

	vec2 surfaceCoord = vec2(inTexture.x * size.x, inTexture.y * size.y);
	vec2 sideCoord = inTexture;

	tTextureCoords = (inIsSideMultiplicator) * sideCoord + (1 - inIsSideMultiplicator) * surfaceCoord;
}

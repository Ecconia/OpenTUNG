#version 330 core

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec3 inNormal;
layout(location = 2) in vec2 inTextureCoord;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

uniform vec2 boardSize;

out vec3 tPosition;
out vec3 tNormal;
out vec2 tTextureCoords;

void main()
{
	vec2 texCoord = inTextureCoord;
	if(texCoord.x == 1.0)
	{
		texCoord.x *= boardSize.x;
	}
	if(texCoord.y == 1.0)
	{
		texCoord.y *= boardSize.y;
	}
	tTextureCoords = texCoord;
	
	mat4 transformMat = view * model;
	vec4 transformedPos = transformMat * vec4(inPosition, 1.0);
	gl_Position = projection * transformedPos;
	tPosition = transformedPos.xyz;
	tNormal = normalize((inverse(transpose(transformMat)) * vec4(inNormal, 0.0)).xyz);
}

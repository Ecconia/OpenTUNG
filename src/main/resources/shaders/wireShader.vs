#version 330 core

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec3 inNormal;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

uniform float length;
uniform float color;

out vec4 tColor;
out vec3 tPosition;
out vec3 tNormal;

void main()
{
	tColor = vec4(color, 0, 0, 1);

	mat4 transformMat = view * model;
    	vec4 transformedPos = transformMat * vec4(inPosition.xy, inPosition.z * length, 1);
    	gl_Position = projection * transformedPos; //The position in projection system, to be use for placement
    	tPosition = transformedPos.xyz; //The position in camera system, to be use for light calculation
    	tNormal = normalize((inverse(transpose(transformMat)) * vec4(inNormal, 0.0)).xyz);
}

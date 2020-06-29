#version 330 core

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec3 inNormal;
layout(location = 2) in vec3 inColor;

uniform mat4 projection;
uniform mat4 view;

out vec4 tColor;
out vec3 tPosition;
out vec3 tNormal;

void main()
{
	tColor = vec4(inColor.rgb, 1.0);
	
	vec4 transformedPos = view * vec4(inPosition, 1.0);
	gl_Position = projection * transformedPos;//The position in projection system, to be use for placement
	tPosition = transformedPos.xyz;//The position in camera system, to be use for light calculation
	tNormal = normalize((inverse(transpose(view)) * vec4(inNormal, 0.0)).xyz);
}

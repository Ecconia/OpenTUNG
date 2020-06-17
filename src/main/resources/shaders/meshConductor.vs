#version 330 core

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec3 inNormal;
layout(location = 2) in uint inIndex;

uniform mat4 projection;
uniform mat4 view;

out vec4 tColor;
out vec3 tPosition;
out vec3 tNormal;

uniform uint states[16352];

void main()
{
	uint actualIndex = inIndex >> 5;
	uint bitmask = 1u << (inIndex & 31u);
	uint state = states[inIndex] & bitmask;
	if(state == 0u)
	{
		tColor = vec4(0.2, 0.2, 0.2, 1.0);
	}
	else
	{
		tColor = vec4(0.9, 0, 0, 1.0);
	}
	
	vec4 transformedPos = view * vec4(inPosition, 1.0);
	gl_Position = projection * transformedPos;//The position in projection system, to be use for placement
	tPosition = transformedPos.xyz;//The position in camera system, to be use for light calculation
	tNormal = normalize((inverse(transpose(view)) * vec4(inNormal, 0.0)).xyz);
}

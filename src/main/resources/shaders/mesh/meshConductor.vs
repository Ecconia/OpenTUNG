#version 330 core

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec3 inNormal;
layout(location = 2) in uint inIndex;

uniform mat4 projection;
uniform mat4 view;

out vec4 tColor;
out vec3 tPosition;
out vec3 tNormal;

uniform uvec4 states[1016]; //4064 16352

void main()
{
	//*** SS BBBBB
	//* - Actual index in array
	//S - xyzw selector of each vector in array
	//B - bitmask for selecting the bit of each value
	uint actualIndex = inIndex >> 7;
	uint partSelector = (inIndex >> 5) & 3u;
	uint bitmask = 1u << (inIndex & 31u);
	
	uvec4 vector = states[actualIndex];
	uint value;
	if(partSelector == 0u)
	{
		value = vector.x;
	}
	else if(partSelector == 1u)
	{
		value = vector.y;
	}
	else if(partSelector == 2u)
	{
		value = vector.z;
	}
	else
	{
		value = vector.w;
	}
	
	uint state = value & bitmask;
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

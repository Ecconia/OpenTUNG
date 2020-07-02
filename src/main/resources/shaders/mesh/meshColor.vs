#version 330 core

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec3 inNormal;
layout(location = 2) in uint inIndex;

uniform mat4 projection;
uniform mat4 view;

uniform uvec4 states[1016]; //(4064 - 32)/ 4 // (16384 - 32) / 4

out vec4 tColor;
out vec3 tPosition;
out vec3 tNormal;

void main()
{
	//******** SS
	//* - Actual index in array
	//S - xyzw selector of each vector in array
	uint actualIndex = inIndex >> 2;
	uint partSelector = inIndex & 3u;
	
	uvec4 vector = states[actualIndex];
	uint value;
	if(partSelector == 0u)
	{
		value = vector.r;
	}
	else if(partSelector == 1u)
	{
		value = vector.g;
	}
	else if(partSelector == 2u)
	{
		value = vector.z;
	}
	else
	{
		value = vector.w;
	}
	
	float r = float((value >> 24) & 255u);
	float g = float((value >> 16) & 255u);
	float b = float((value >> 8) & 255u);
	float a = float(value & 255u);
	
	tColor = vec4(r / 255.0, g / 255.0, b / 255.0, a / 255.0);
	
	vec4 transformedPos = view * vec4(inPosition, 1.0);
	gl_Position = projection * transformedPos; //The position in projection system, to be use for placement
	tPosition = transformedPos.xyz; //The position in camera system, to be use for light calculation
	tNormal = normalize((inverse(transpose(view)) * vec4(inNormal, 0.0)).xyz);
}

#version 330 core

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec3 inNormal;//Ignored

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

uniform float length;

void main()
{
	gl_Position = projection * view * model * vec4(inPosition.x, inPosition.y, inPosition.z * length, 1.0);
}

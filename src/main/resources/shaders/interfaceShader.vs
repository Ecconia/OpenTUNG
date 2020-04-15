#version 330 core

layout(location = 0) in vec2 inPosition;
layout(location = 1) in vec3 inColor;

uniform mat4 projection;
uniform mat4 model;

out vec4 tColor;

void main()
{
	tColor = vec4(inColor.rgb, 1.0);
	gl_Position = projection * model * vec4(inPosition, 0.0, 1.0);
}

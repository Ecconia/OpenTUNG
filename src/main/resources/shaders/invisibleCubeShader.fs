#version 330 core
precision mediump float;

uniform vec4 color;

out vec4 outColor;

void main()
{
	outColor = color;
}

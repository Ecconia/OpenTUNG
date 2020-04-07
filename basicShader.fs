#version 330 core
precision mediump float;

in vec4 transColor;

out vec4 outColor;

void main()
{
    outColor = transColor;
}
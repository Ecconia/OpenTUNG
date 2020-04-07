#version 330 core

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec3 inColor;
layout(location = 2) in vec3 inNormal;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

out vec4 transColor;

void main()
{
    transColor = vec4(inColor.rgb, 0.5);
    gl_Position = projection * view * model * vec4(inPosition, 1.0);
}

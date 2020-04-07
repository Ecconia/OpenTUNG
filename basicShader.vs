#version 330 core

layout(location = 0) in vec3 inPosition;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

out vec4 transColor;

void main()
{
    transColor = vec4(0.0, 0.0, 1.0, 0.5);
    gl_Position = projection * view * model * vec4(inPosition, 1.0);
}

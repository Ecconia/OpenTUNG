#version 330 core

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec3 inNormal;//Ignored
layout(location = 2) in vec2 inTexture;//Ignored
layout(location = 3) in float inIsSideMultiplicator;//Ignored

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

uniform vec2 size;

void main()
{
	vec4 pos = vec4(inPosition.x * size.x * 0.15, inPosition.y, inPosition.z * size.y * 0.15, 1.0);
	gl_Position = projection * view * model * pos;
}

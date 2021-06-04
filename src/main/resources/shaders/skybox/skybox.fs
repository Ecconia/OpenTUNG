#version 330 core

in vec3 tTextureCoordinates;

uniform samplerCube skybox;

out vec4 outColor;

void main()
{
	outColor = texture(skybox, tTextureCoordinates);
}

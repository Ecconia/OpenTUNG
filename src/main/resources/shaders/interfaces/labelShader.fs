#version 330 core
precision mediump float;

in vec2 tTextureCoords;

uniform sampler2D textureVar;

out vec4 outColor;

void main()
{
	float value = texture(textureVar, tTextureCoords).r;
	outColor = vec4(1, 1, 1, 1.0 - value);
}

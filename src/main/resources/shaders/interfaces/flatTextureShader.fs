#version 330 core
precision mediump float;

in vec2 tTextureCoords;

uniform sampler2D textureVar;

out vec4 outColor;

void main()
{
	outColor = texture(textureVar, tTextureCoords);
}

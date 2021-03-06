#version 330 core
precision mediump float;

in vec3 tPosition;
in vec3 tNormal;
in vec2 tTextureCoords;

uniform vec4 uColor;

uniform sampler2D boardTexture;

out vec4 outColor;

void main()
{
	vec3 tColor = uColor.rgb;
	
	vec3 normal = normalize(tNormal);
	
	vec3 vV = normalize(-tPosition);//Turn the vertex position into a vector to origin
	vec3 color = vec3(0.0, 0.0, 0.0);
	
	//vec3 sourcePosition = vec3(20.0, 20.0, 20.0);
	vec3 sourcePosition = vec3(0.0, 0.0, -2.0);
	vec3 sourceAmbient = vec3(1.0, 1.0, 1.0);
	vec3 sourceDiffuse = vec3(1.0, 1.0, 1.0);
	
	vec3 realColor = texture(boardTexture, tTextureCoords).rgb * tColor;
	vec3 materialAmbient = realColor * 0.6;
	vec3 materialDiffuse = realColor * 0.4;
	
	//Loop:
	vec3 lightPosition = (vec4(sourcePosition, 1.0)).xyz;//view2 *
	vec3 vL = lightPosition - tPosition;//Vector from Vertex to Lightsource
	float d = length(vL);//Get distance from the Vertex to the Lightsource
	//float fatt = min(1.0, 1.0 / (sourceC[i].x + sourceC[i].y * d + sourceC[i].z * d * d)); //Disabled.
	float fatt = 1.0;
	vL = normalize(vL);//Fix length of vector L
	
	color += sourceAmbient * materialAmbient
	+ fatt * (
	sourceDiffuse * materialDiffuse * max(0.0, dot(normal, vL))
	//+ sourceSpecular[i] * tMaterialSpecular * pow(max(0.0, dot(reflect(-vL, normal), vV)), tMaterialN)
	);
	
	outColor = vec4(color, 1.0);//Alpha
}

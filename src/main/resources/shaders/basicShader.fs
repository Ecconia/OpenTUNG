#version 330 core
precision mediump float;

in vec4 tColor; //Color (diffuse) of this fragment
in vec3 tPosition; //Camera position of this fragment
in vec3 tNormal; //Normal of this fragment

uniform mat4 view2;

out vec4 outColor;

void main()
{
	vec3 normal = normalize(tNormal);

	vec3 vV = normalize(-tPosition); //Turn the vertex position into a vector to origin
	vec3 color = vec3(0.0, 0.0, 0.0);

	//vec3 sourcePosition = vec3(20.0, 20.0, 20.0);
	vec3 sourcePosition = vec3(0.0, 0.0, -2.0);
	vec3 sourceAmbient = vec3(1.0, 1.0, 1.0);
	vec3 sourceDiffuse = vec3(1.0, 1.0, 1.0);

	vec3 materialAmbient = tColor.rgb * 0.6;
	vec3 materialDiffuse = tColor.rgb * 0.4;

	//Loop:
	vec3 lightPosition = (vec4(sourcePosition, 1.0)).xyz; //view2 *
	vec3 vL = lightPosition - tPosition; //Vector from Vertex to Lightsource
	float d = length(vL); //Get distance from the Vertex to the Lightsource
	//float fatt = min(1.0, 1.0 / (sourceC[i].x + sourceC[i].y * d + sourceC[i].z * d * d)); //Disabled.
	float fatt = 1.0;
	vL = normalize(vL); //Fix length of vector L

	color += sourceAmbient * materialAmbient
	 + fatt * (
		sourceDiffuse * materialDiffuse * max(0.0, dot(normal, vL))
		//+ sourceSpecular[i] * tMaterialSpecular * pow(max(0.0, dot(reflect(-vL, normal), vV)), tMaterialN)
	);

	outColor = vec4(color, tColor.a); //Alpha
}

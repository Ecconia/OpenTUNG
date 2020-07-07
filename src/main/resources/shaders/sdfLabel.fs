#version 330 core
precision mediump float;

in vec3 tPosition;
in vec3 tNormal;
in vec2 tTextureCoords;

uniform sampler2D textureVar;

out vec4 outColor;

vec4 getTextureAt(vec2 position)
{
	float alpha = texture(textureVar, position).r;
	return vec4(0, 0, 0, alpha);
}

void main()
{
	vec4 baseColor = getTextureAt(tTextureCoords);
	
	bool OUTLINE = false;
	float OUTLINEMINVALUE0 = 0.5 - 0.1;
	float OUTLINEMAXVALUE1 = 0.5 + 0.1;
	vec4 OUTLINECOLOR = vec4(0, 1, 1, 1);
	
	//Dunno how to get these guys working properly.
	bool SOFTEDGES = false;
	float SOFTEDGEMIN = 0.4;
	float SOFTEDGEMAX = 0.45;
	
//	bool OUTERGLOW = false;
//	vec2 GLOWUVOFFSET = vec2(0.0, 0.0);
//	vec4 OUTERGLOWCOLOR = vec4(1, 0, 1, 1);
//	float OUTERGLOWMINDVALUE = 0.0;
//	float OUTERGLOWMAXDVALUE = 0.0;
	
	float distAlphaMask = baseColor.a;
	if (OUTLINE && (distAlphaMask >= OUTLINEMINVALUE0) && (distAlphaMask <= OUTLINEMAXVALUE1))
	{
		float oFactor = 1.0;
		if (distAlphaMask <= OUTLINEMAXVALUE1)
		{
			oFactor = smoothstep(OUTLINEMINVALUE0, OUTLINEMAXVALUE1, distAlphaMask);
		}
		else
		{
			oFactor = smoothstep(OUTLINEMAXVALUE1, OUTLINEMINVALUE0, distAlphaMask);
		}
		baseColor = mix(baseColor, OUTLINECOLOR, oFactor);
	}
	
	if (SOFTEDGES)
	{
		baseColor.a *= smoothstep(SOFTEDGEMIN, SOFTEDGEMAX, distAlphaMask);
	}
	else
	{
		if(distAlphaMask >= 0.5)
		{
			baseColor.a = 1.0;
		}
		else
		{
			baseColor.a = 0.0;
		}
	}
	
//	if (OUTERGLOW)
//	{
//		vec4 glowTexel = getTextureAt(tTextureCoords.xy + GLOWUVOFFSET);
//		vec4 glowc = OUTERGLOWCOLOR * smoothstep(OUTERGLOWMINDVALUE, OUTERGLOWMAXDVALUE, glowTexel.a);
//		baseColor = mix(glowc, baseColor, mskUsed); //Where is mskUsed coming from?
//	}
	
//	if(baseColor.a > 0)
//	{
//		baseColor.a = 1;
//	}
	//float c = texture(textureVar, tTextureCoords).r;
	//outColor = vec4(c,c,c,1);
	outColor = baseColor;
}

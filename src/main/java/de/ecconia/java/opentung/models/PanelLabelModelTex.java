package de.ecconia.java.opentung.models;

public class PanelLabelModelTex extends GenericModel
{
	public PanelLabelModelTex()
	{
		//Create:
		vertices = new float[]{
				-0.15f, 0.1f + 0.075f, -0.15f, 1, 0,
				+0.15f, 0.1f + 0.075f, -0.15f, 0, 0,
				+0.15f, 0.1f + 0.075f, +0.15f, 0, 1,
				-0.15f, 0.1f + 0.075f, +0.15f, 1, 1,
		};
		
		indices = new short[]{
				0, 1, 2,
				0, 3, 2,
		};
		
		upload(ShaderType.LabelShader);
	}
}

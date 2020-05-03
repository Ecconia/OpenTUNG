package de.ecconia.java.opentung.models;

public class CrossyIndicatorModel extends GenericModel
{
	public CrossyIndicatorModel()
	{
		vertices = new float[]{
				-0.1f, +0.0f, +0.0f, 0.2f, 0.2f, 1,
				+0.1f, +0.0f, +0.0f, 0.2f, 0.2f, 1,
				+0.0f, -0.1f, +0.0f, 0.2f, 0.2f, 1,
				+0.0f, +0.1f, +0.0f, 0.2f, 0.2f, 1,
				+0.0f, +0.0f, -0.1f, 0.2f, 0.2f, 1,
				+0.0f, +0.0f, +0.1f, 0.2f, 0.2f, 1,
		};
		indices = new short[]{
				0, 1,
				2, 3,
				4, 5,
		};
		
		upload(ShaderType.SimpleLines);
	}
}

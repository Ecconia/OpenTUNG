package de.ecconia.java.opentung.models;

public class NormalIndicatorModel extends GenericModel
{
	public NormalIndicatorModel()
	{
		vertices = new float[]{
				0, 0, 0, 1, 0, 0,
				0, 0.3f, 0, 1, 0, 0,
		};
		indices = new short[]{
				0, 1,
		};
		
		upload(ShaderType.SimpleLines);
	}
}

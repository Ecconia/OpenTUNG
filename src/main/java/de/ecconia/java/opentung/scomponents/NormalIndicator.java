package de.ecconia.java.opentung.scomponents;

public class NormalIndicator extends SComponent
{
	public NormalIndicator()
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

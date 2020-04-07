package de.ecconia.java.opentung.scomponents;

public class SimpleInverterModel extends SComponent
{
	public SimpleInverterModel()
	{
		//Create:
		IntHolder offset = new IntHolder(); //Vertices array offset
		vertices = new float[3 * 8 * 6];
		placeCube(0, 0, 0, 0.15f, 0.15f, 0.15f, 1.0f, 1.0f, 1.0f, offset);
		placeCube(0, 0.15f + 0.12f, 0, 0.045f, 0.12f, 0.045f, 0.0f, 0.0f, 0.0f, offset);
		placeCube(0, 0, 0.15f + 0.06f, 0.075f, 0.075f, 0.06f, 1.0f, 0.0f, 0.0f, offset);
		
		offset = new IntHolder(); //Indices array offset
		indices = new short[3 * 6 * 6];
		placeCubeIndices(offset, 0, null);
		placeCubeIndices(offset, 8, null);
		placeCubeIndices(offset, 16, null);
		
		upload();
	}
}

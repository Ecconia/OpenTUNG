package de.ecconia.java.opentung.scomponents;

public class SimpleBlotterModel extends SComponent
{
	public SimpleBlotterModel()
	{
		//Create:
		IntHolder offset = new IntHolder(); //Vertices array offset
		vertices = new float[3 * 6 * 4 * 9];
		placeCube(0, 0, 0, 0.15f, 0.15f, 0.15f, 1.0f, 1.0f, 1.0f, offset); //Base
		placeCube(0, 0, -(0.15f + 0.15f), 0.045f, 0.045f, 0.15f, 0.0f, 0.0f, 0.0f, offset); //Input
		placeCube(0, 0, 0.15f + 0.06f, 0.075f, 0.075f, 0.06f, 1.0f, 0.0f, 0.0f, offset); //Output
		
		offset = new IntHolder(); //Indices array offset
		indices = new short[3 * 6 * 6];
		placeCubeIndices(offset, 0 * 24, null); //Base
		placeCubeIndices(offset, 1 * 24, null); //Input
		placeCubeIndices(offset, 2 * 24, null); //Output
		
		upload();
	}
}

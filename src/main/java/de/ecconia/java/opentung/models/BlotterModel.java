package de.ecconia.java.opentung.models;

public class BlotterModel extends GenericModel
{
	//2496 bytes
	public BlotterModel()
	{
		//Create:
		IntHolder offset = new IntHolder(); //Vertices array offset
		vertices = new float[3 * 6 * 4 * 9 - 2 * 4 * 9];
		placeCube(0, 0, 0, 0.15f, 0.15f, 0.15f, 1.0f, 1.0f, 1.0f, offset, null); //Base
		placeCube(0, 0, +(0.15f + 0.15f), 0.045f, 0.045f, 0.15f, 0.0f, 0.0f, 0.0f, offset, Direction.zN); //Input
		placeCube(0, 0, -(0.15f + 0.06f), 0.075f, 0.075f, 0.06f, 0.0f, 0.0f, 0.0f, offset, Direction.zP); //Output
		
		offset = new IntHolder(); //Indices array offset
		indices = new short[3 * 6 * 6 - 2 * 6];
		placeCubeIndices(offset, 0, null); //Base
		placeCubeIndices(offset, 24, Direction.zN); //Input
		placeCubeIndices(offset, 48 - 4, Direction.zP); //Output
		
		upload();
	}
}

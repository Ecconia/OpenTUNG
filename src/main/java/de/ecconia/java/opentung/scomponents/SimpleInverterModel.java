package de.ecconia.java.opentung.scomponents;

public class SimpleInverterModel extends SComponent
{
	//2496 bytes
	public SimpleInverterModel()
	{
		//Create:
		IntHolder offset = new IntHolder(); //Vertices array offset
		vertices = new float[3 * 6 * 4 * 9 - 2 * 4 * 9];
		placeCube(0, 0.15f + 0.075f, 0, 0.15f, 0.15f, 0.15f, 1.0f, 1.0f, 1.0f, offset, null); //Base
		placeCube(0, 0.15f + 0.075f + 0.15f + 0.12f, 0, 0.045f, 0.12f, 0.045f, 0.0f, 0.0f, 0.0f, offset, Direction.yN); //Input
		placeCube(0, 0.15f + 0.075f, -(0.15f + 0.06f), 0.075f, 0.075f, 0.06f, 1.0f, 0.0f, 0.0f, offset, Direction.zP); //Output Direction.xP
		
		offset = new IntHolder(); //Indices array offset
		indices = new short[3 * 6 * 6 - 2 * 6];
		placeCubeIndices(offset, 0 * 24, null); //Base
		placeCubeIndices(offset, 1 * 24, Direction.yN); //Input
		placeCubeIndices(offset, 2 * 24 - 4, Direction.zP); //Output Direction.xP
		
		upload();
	}
}

package de.ecconia.java.opentung.scomponents;

public class SimpleThroughPeg extends SComponent
{
	//2496 bytes
	public SimpleThroughPeg()
	{
		//Create:
		IntHolder offset = new IntHolder(); //Vertices array offset
		vertices = new float[3 * 6 * 4 * 9 - 2 * 4 * 9];
		placeCube(0, 0, 0, 0.1f, 0.075f + 0.0875f, 0.1f, 1.0f, 1.0f, 1.0f, offset, null); //Base
		placeCube(0, +(0.075f + 0.0875f + 0.15f), 0, 0.05f, 0.15f, 0.05f, 0.0f, 0.0f, 0.0f, offset, Direction.yN); //Up
		placeCube(0, -(0.075f + 0.0875f + 0.15f), 0, 0.05f, 0.15f, 0.05f, 0.0f, 0.0f, 0.0f, offset, Direction.yP); //Down
		
		offset = new IntHolder(); //Indices array offset
		indices = new short[3 * 6 * 6 - 2 * 6];
		placeCubeIndices(offset, 0, null); //Base
		placeCubeIndices(offset, 24, Direction.yN); //Up
		placeCubeIndices(offset, 48 - 4, Direction.yP); //Down
		
		upload();
	}
}

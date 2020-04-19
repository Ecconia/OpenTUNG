package de.ecconia.java.opentung.scomponents;

public class CoordIndicator extends SComponent
{
	public CoordIndicator()
	{
		IntHolder offset = new IntHolder(); //Vertices array offset
		vertices = new float[3 * 6 * 4 * 9];
		placeCube(0.225f, 0.00f, 0.00f, 0.45f, 0.01f, 0.01f, 1, 0, 0, offset, null); //X
		placeCube(0.00f, 0.225f, 0.00f, 0.01f, 0.45f, 0.01f, 0, 1, 0, offset, null); //Y
		placeCube(0.00f, 0.00f, 0.225f, 0.01f, 0.01f, 0.45f, 0, 0, 1, offset, null); //Z
		
		offset = new IntHolder(); //Indices array offset
		indices = new short[3 * 6 * 6];
		placeCubeIndices(offset, 0, null); //X
		placeCubeIndices(offset, 24, null); //Y
		placeCubeIndices(offset, 48, null); //Z
		
		upload();
	}
}

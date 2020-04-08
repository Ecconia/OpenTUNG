package de.ecconia.java.opentung.scomponents;

public class SimplePeg extends SComponent
{
	//936 bytes
	public SimplePeg()
	{
		//Create:
		IntHolder offset = new IntHolder(); //Vertices array offset
		vertices = new float[1 * 6 * 4 * 9];
		placeCube(0, 0, 0, 0.045f, 0.15f, 0.045f, 0.0f, 0.0f, 0.0f, offset, null);
		
		offset = new IntHolder(); //Indices array offset
		indices = new short[1 * 6 * 6];
		placeCubeIndices(offset, 0 * 24, null);
		
		upload();
	}
}

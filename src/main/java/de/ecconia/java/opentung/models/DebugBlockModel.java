package de.ecconia.java.opentung.models;

public class DebugBlockModel extends GenericModel
{
	public DebugBlockModel()
	{
		//Create:
		IntHolder offset = new IntHolder(); //Vertices array offset
		vertices = new float[6 * 4 * 9];
		placeCube(0, 0, 0, 0.15f, 0.15f, 0.15f, 1.0f, 1.0f, 1.0f, offset, null); //Base
		
		offset = new IntHolder(); //Indices array offset
		indices = new short[6 * 6];
		placeCubeIndices(offset, 0, null); //Base
		
		upload();
	}
}

package de.ecconia.java.opentung.models;

public class LabelModel extends GenericModel
{
	public LabelModel()
	{
		//Create:
		IntHolder offset = new IntHolder(); //Vertices array offset
		vertices = new float[6 * 4 * 9 - 1 * 4 * 9];
		placeCube(0, 0.15f + 0.075f, 0, 0.15f, 0.15f, 0.15f, 1.0f, 1.0f, 1.0f, offset, Direction.yP); //Base
		
		offset = new IntHolder(); //Indices array offset
		indices = new short[6 * 6 - 1 * 6];
		placeCubeIndices(offset, 0, Direction.yP); //Base
		
		upload();
	}
}

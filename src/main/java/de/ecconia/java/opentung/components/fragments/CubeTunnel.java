package de.ecconia.java.opentung.components.fragments;

import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.math.Vector3;

public class CubeTunnel extends CubeFull
{
	private final Direction openDirection;
	
	public CubeTunnel(Vector3 position, Vector3 size, Direction openDirection)
	{
		super(position, size, null);
		
		this.openDirection = openDirection;
	}
	
	@Override
	public int getVCount()
	{
		//Faces * Vertices * Data
		return 4 * 4 * 9;
	}
	
	@Override
	public int getICount()
	{
		//Faces * Triangles * Indices
		return 4 * 2 * 3;
	}
	
	@Override
	public void generateModel(float[] vertices, ModelHolder.IntHolder offsetV, short[] indices, ModelHolder.IntHolder offsetI, ModelHolder.IntHolder indexOffset, ModelHolder.TestModelType type, Vector3 offset)
	{
		Vector3 realPosition = position.add(offset);
		Vector3 min = realPosition.subtract(size);
		Vector3 max = realPosition.add(size);
		if(openDirection != Direction.YPos && openDirection != Direction.YNeg)
		{
			//Up:
			genVertex(vertices, offsetV, min.getX(), max.getY(), min.getZ(), 0, 1, 0);
			genVertex(vertices, offsetV, max.getX(), max.getY(), min.getZ(), 0, 1, 0);
			genVertex(vertices, offsetV, max.getX(), max.getY(), max.getZ(), 0, 1, 0);
			genVertex(vertices, offsetV, min.getX(), max.getY(), max.getZ(), 0, 1, 0);
			genIndex(indices, offsetI, indexOffset.getAndInc(4));
			//Down
			genVertex(vertices, offsetV, min.getX(), min.getY(), min.getZ(), 0, -1, 0);
			genVertex(vertices, offsetV, max.getX(), min.getY(), min.getZ(), 0, -1, 0);
			genVertex(vertices, offsetV, max.getX(), min.getY(), max.getZ(), 0, -1, 0);
			genVertex(vertices, offsetV, min.getX(), min.getY(), max.getZ(), 0, -1, 0);
			genIndex(indices, offsetI, indexOffset.getAndInc(4));
		}
		if(openDirection != Direction.XPos && openDirection != Direction.XNeg)
		{
			//Right:
			genVertex(vertices, offsetV, max.getX(), min.getY(), min.getZ(), 1, 0, 0);
			genVertex(vertices, offsetV, max.getX(), min.getY(), max.getZ(), 1, 0, 0);
			genVertex(vertices, offsetV, max.getX(), max.getY(), max.getZ(), 1, 0, 0);
			genVertex(vertices, offsetV, max.getX(), max.getY(), min.getZ(), 1, 0, 0);
			genIndex(indices, offsetI, indexOffset.getAndInc(4));
			//Left:
			genVertex(vertices, offsetV, min.getX(), min.getY(), max.getZ(), -1, 0, 0);
			genVertex(vertices, offsetV, min.getX(), min.getY(), min.getZ(), -1, 0, 0);
			genVertex(vertices, offsetV, min.getX(), max.getY(), min.getZ(), -1, 0, 0);
			genVertex(vertices, offsetV, min.getX(), max.getY(), max.getZ(), -1, 0, 0);
			genIndex(indices, offsetI, indexOffset.getAndInc(4));
		}
		if(openDirection != Direction.ZPos && openDirection != Direction.ZNeg)
		{
			//Forward:
			genVertex(vertices, offsetV, min.getX(), min.getY(), min.getZ(), 0, 0, 1);
			genVertex(vertices, offsetV, max.getX(), min.getY(), min.getZ(), 0, 0, 1);
			genVertex(vertices, offsetV, max.getX(), max.getY(), min.getZ(), 0, 0, 1);
			genVertex(vertices, offsetV, min.getX(), max.getY(), min.getZ(), 0, 0, 1);
			genIndex(indices, offsetI, indexOffset.getAndInc(4));
			//Back:
			genVertex(vertices, offsetV, max.getX(), min.getY(), max.getZ(), 0, 0, -1);
			genVertex(vertices, offsetV, min.getX(), min.getY(), max.getZ(), 0, 0, -1);
			genVertex(vertices, offsetV, min.getX(), max.getY(), max.getZ(), 0, 0, -1);
			genVertex(vertices, offsetV, max.getX(), max.getY(), max.getZ(), 0, 0, -1);
			genIndex(indices, offsetI, indexOffset.getAndInc(4));
		}
	}
}

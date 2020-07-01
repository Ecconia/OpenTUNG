package de.ecconia.java.opentung.components.fragments;

import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.math.Vector3;

public class TexturedFace extends CubeOpen
{
	public TexturedFace(Vector3 position, Vector3 size, Direction closedDirection)
	{
		super(position, size, closedDirection);
	}
	
	@Override
	public int getVCount()
	{
		return 4 * 8;
	}
	
	@Override
	public int getICount()
	{
		return 6;
	}
	
	public void generateModel(float[] vertices, ModelHolder.IntHolder offsetV, short[] indices, ModelHolder.IntHolder offsetI, ModelHolder.IntHolder indexOffset, Vector3 offset)
	{
		Vector3 realPosition = position.add(offset);
		Vector3 min = realPosition.subtract(size);
		Vector3 max = realPosition.add(size);
		if(direction == Direction.YPos)
		{
			//Up:
			genVertex(vertices, offsetV, min.getX(), max.getY(), min.getZ(), 0, 1, 0, 1, 0);
			genVertex(vertices, offsetV, max.getX(), max.getY(), min.getZ(), 0, 1, 0, 0, 0);
			genVertex(vertices, offsetV, max.getX(), max.getY(), max.getZ(), 0, 1, 0, 0, 1);
			genVertex(vertices, offsetV, min.getX(), max.getY(), max.getZ(), 0, 1, 0, 1, 1);
			genIndex(indices, offsetI, indexOffset.getAndInc(6));
		}
		//TODO: Check if all texture rotations are properly aligned from here on. Cause there is probs no need for them...
		if(direction == Direction.YNeg)
		{
			//Down
			genVertex(vertices, offsetV, max.getX(), min.getY(), min.getZ(), 0, -1, 0, 0, 0);
			genVertex(vertices, offsetV, min.getX(), min.getY(), min.getZ(), 0, -1, 0, 1, 0);
			genVertex(vertices, offsetV, min.getX(), min.getY(), max.getZ(), 0, -1, 0, 1, 1);
			genVertex(vertices, offsetV, max.getX(), min.getY(), max.getZ(), 0, -1, 0, 0, 1);
			genIndex(indices, offsetI, indexOffset.getAndInc(6));
		}
		if(direction == Direction.XPos)
		{
			//Right:
			genVertex(vertices, offsetV, max.getX(), min.getY(), min.getZ(), 1, 0, 0, 1, 0);
			genVertex(vertices, offsetV, max.getX(), min.getY(), max.getZ(), 1, 0, 0, 0, 0);
			genVertex(vertices, offsetV, max.getX(), max.getY(), max.getZ(), 1, 0, 0, 0, 1);
			genVertex(vertices, offsetV, max.getX(), max.getY(), min.getZ(), 1, 0, 0, 1, 1);
			genIndex(indices, offsetI, indexOffset.getAndInc(6));
		}
		if(direction == Direction.XNeg)
		{
			//Left:
			genVertex(vertices, offsetV, min.getX(), min.getY(), max.getZ(), -1, 0, 0, 1, 0);
			genVertex(vertices, offsetV, min.getX(), min.getY(), min.getZ(), -1, 0, 0, 0, 0);
			genVertex(vertices, offsetV, min.getX(), max.getY(), min.getZ(), -1, 0, 0, 0, 1);
			genVertex(vertices, offsetV, min.getX(), max.getY(), max.getZ(), -1, 0, 0, 1, 1);
			genIndex(indices, offsetI, indexOffset.getAndInc(6));
		}
		if(direction == Direction.ZNeg)
		{
			//Forward:
			genVertex(vertices, offsetV, min.getX(), min.getY(), min.getZ(), 0, 0, 1, 1, 0);
			genVertex(vertices, offsetV, max.getX(), min.getY(), min.getZ(), 0, 0, 1, 0, 0);
			genVertex(vertices, offsetV, max.getX(), max.getY(), min.getZ(), 0, 0, 1, 0, 1);
			genVertex(vertices, offsetV, min.getX(), max.getY(), min.getZ(), 0, 0, 1, 1, 1);
			genIndex(indices, offsetI, indexOffset.getAndInc(6));
		}
		if(direction == Direction.ZPos)
		{
			//Back:
			genVertex(vertices, offsetV, max.getX(), min.getY(), max.getZ(), 0, 0, -1, 1, 0);
			genVertex(vertices, offsetV, min.getX(), min.getY(), max.getZ(), 0, 0, -1, 0, 0);
			genVertex(vertices, offsetV, min.getX(), max.getY(), max.getZ(), 0, 0, -1, 0, 1);
			genVertex(vertices, offsetV, max.getX(), max.getY(), max.getZ(), 0, 0, -1, 1, 1);
			genIndex(indices, offsetI, indexOffset.getAndInc(6));
		}
	}
	
	private void genVertex(float[] vertices, ModelHolder.IntHolder offsetV,
	                       double x, double y, double z, int nx, int ny, int nz, int tx, int ty)
	{
		vertices[offsetV.getAndInc()] = (float) x;
		vertices[offsetV.getAndInc()] = (float) y;
		vertices[offsetV.getAndInc()] = (float) z;
//		vertices[offsetV.getAndInc()] = nx;
//		vertices[offsetV.getAndInc()] = ny;
//		vertices[offsetV.getAndInc()] = nz;
		vertices[offsetV.getAndInc()] = tx;
		vertices[offsetV.getAndInc()] = ty;
	}
	
	protected void genIndex(short[] indices, ModelHolder.IntHolder offsetI, int index)
	{
		indices[offsetI.getAndInc()] = (short) (index + 0);
		indices[offsetI.getAndInc()] = (short) (index + 1);
		indices[offsetI.getAndInc()] = (short) (index + 2);
		indices[offsetI.getAndInc()] = (short) (index + 0);
		indices[offsetI.getAndInc()] = (short) (index + 2);
		indices[offsetI.getAndInc()] = (short) (index + 3);
	}
}

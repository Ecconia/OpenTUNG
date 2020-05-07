package de.ecconia.java.opentung.components.fragments;

import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.math.Vector3;

public class CubeBoard extends CubeFull
{
	public CubeBoard(Vector3 position, Vector3 size)
	{
		super(position, size, null);
	}
	
	@Override
	public int getVCount()
	{
		//Faces * Vertices * Data
		return 6 * 4 * 9;
	}
	
	@Override
	public int getICount()
	{
		//Faces * Triangles * Indices
		return 6 * 2 * 3;
	}
	
	@Override
	public void generateModel(float[] vertices, ModelHolder.IntHolder offsetV, short[] indices, ModelHolder.IntHolder offsetI, ModelHolder.IntHolder indexOffset, ModelHolder.TestModelType type, Vector3 offset)
	{
		final float t = 0.01f;
		Vector3 realPosition = position.add(offset);
		Vector3 min = realPosition.subtract(size);
		Vector3 max = realPosition.add(size);
		//Up:
		genVertex(vertices, offsetV, min.getX(), max.getY(), min.getZ(), 0, 1, 0, 1, 0, 0);
		genVertex(vertices, offsetV, max.getX(), max.getY(), min.getZ(), 0, 1, 0, 0, 0, 0);
		genVertex(vertices, offsetV, max.getX(), max.getY(), max.getZ(), 0, 1, 0, 0, 1, 0);
		genVertex(vertices, offsetV, min.getX(), max.getY(), max.getZ(), 0, 1, 0, 1, 1, 0);
		genIndex(indices, offsetI, indexOffset.getAndInc(4));
		//Down
		genVertex(vertices, offsetV, min.getX(), min.getY(), min.getZ(), 0, -1, 0, 1, 0, 0);
		genVertex(vertices, offsetV, max.getX(), min.getY(), min.getZ(), 0, -1, 0, 0, 0, 0);
		genVertex(vertices, offsetV, max.getX(), min.getY(), max.getZ(), 0, -1, 0, 0, 1, 0);
		genVertex(vertices, offsetV, min.getX(), min.getY(), max.getZ(), 0, -1, 0, 1, 1, 0);
		genIndex(indices, offsetI, indexOffset.getAndInc(4));
		//Right:
		genVertex(vertices, offsetV, max.getX(), min.getY(), min.getZ(), 1, 0, 0, t, 0, 1);
		genVertex(vertices, offsetV, max.getX(), min.getY(), max.getZ(), 1, 0, 0, 0, 0, 1);
		genVertex(vertices, offsetV, max.getX(), max.getY(), max.getZ(), 1, 0, 0, 0, t, 1);
		genVertex(vertices, offsetV, max.getX(), max.getY(), min.getZ(), 1, 0, 0, t, t, 1);
		genIndex(indices, offsetI, indexOffset.getAndInc(4));
		//Left:
		genVertex(vertices, offsetV, min.getX(), min.getY(), max.getZ(), -1, 0, 0, t, 0, 1);
		genVertex(vertices, offsetV, min.getX(), min.getY(), min.getZ(), -1, 0, 0, 0, 0, 1);
		genVertex(vertices, offsetV, min.getX(), max.getY(), min.getZ(), -1, 0, 0, 0, t, 1);
		genVertex(vertices, offsetV, min.getX(), max.getY(), max.getZ(), -1, 0, 0, t, t, 1);
		genIndex(indices, offsetI, indexOffset.getAndInc(4));
		//Forward:
		genVertex(vertices, offsetV, min.getX(), min.getY(), min.getZ(), 0, 0, 1, t, 0, 1);
		genVertex(vertices, offsetV, max.getX(), min.getY(), min.getZ(), 0, 0, 1, 0, 0, 1);
		genVertex(vertices, offsetV, max.getX(), max.getY(), min.getZ(), 0, 0, 1, 0, t, 1);
		genVertex(vertices, offsetV, min.getX(), max.getY(), min.getZ(), 0, 0, 1, t, t, 1);
		genIndex(indices, offsetI, indexOffset.getAndInc(4));
		//Back:
		genVertex(vertices, offsetV, max.getX(), min.getY(), max.getZ(), 0, 0, -1, t, 0, 1);
		genVertex(vertices, offsetV, min.getX(), min.getY(), max.getZ(), 0, 0, -1, 0, 0, 1);
		genVertex(vertices, offsetV, min.getX(), max.getY(), max.getZ(), 0, 0, -1, 0, t, 1);
		genVertex(vertices, offsetV, max.getX(), max.getY(), max.getZ(), 0, 0, -1, t, t, 1);
		genIndex(indices, offsetI, indexOffset.getAndInc(4));
	}
	
	private void genVertex(float[] vertices, ModelHolder.IntHolder offsetV,
	                       double x, double y, double z, int nx, int ny, int nz, float tx, float ty, int side)
	{
		vertices[offsetV.getAndInc()] = (float) x;
		vertices[offsetV.getAndInc()] = (float) y;
		vertices[offsetV.getAndInc()] = (float) z;
		vertices[offsetV.getAndInc()] = nx;
		vertices[offsetV.getAndInc()] = ny;
		vertices[offsetV.getAndInc()] = nz;
		vertices[offsetV.getAndInc()] = tx;
		vertices[offsetV.getAndInc()] = ty;
		vertices[offsetV.getAndInc()] = side;
	}
}

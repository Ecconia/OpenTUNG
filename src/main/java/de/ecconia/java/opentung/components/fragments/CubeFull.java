package de.ecconia.java.opentung.components.fragments;

import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.math.Vector3;

public class CubeFull extends Meshable
{
	protected final Vector3 position;
	protected final Vector3 size;
	protected Color color;
	
	public CubeFull(Vector3 position, Vector3 size, Color color)
	{
		this.position = position;
		this.size = size.divide(2); //Collision as well as shader only use half the value.
		this.color = color;
	}
	
	public boolean contains(Vector3 probe)
	{
		return !(probe.getX() < position.getX() - size.getX()
				|| probe.getX() > position.getX() + size.getX()
				|| probe.getY() < position.getY() - size.getY()
				|| probe.getY() > position.getY() + size.getY()
				|| probe.getZ() < position.getZ() - size.getZ()
				|| probe.getZ() > position.getZ() + size.getZ());
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
		Vector3 realPosition = position.add(offset);
		Vector3 min = realPosition.subtract(size);
		Vector3 max = realPosition.add(size);
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
	
	protected void genIndex(short[] indices, ModelHolder.IntHolder offsetI, int index)
	{
		indices[offsetI.getAndInc()] = (short) (index + 0);
		indices[offsetI.getAndInc()] = (short) (index + 1);
		indices[offsetI.getAndInc()] = (short) (index + 2);
		indices[offsetI.getAndInc()] = (short) (index + 0);
		indices[offsetI.getAndInc()] = (short) (index + 3);
		indices[offsetI.getAndInc()] = (short) (index + 2);
	}
	
	protected void genVertex(float[] vertices, ModelHolder.IntHolder offsetV,
	                       double x, double y, double z, float nx, float ny, float nz)
	{
		vertices[offsetV.getAndInc()] = (float) x;
		vertices[offsetV.getAndInc()] = (float) y;
		vertices[offsetV.getAndInc()] = (float) z;
		vertices[offsetV.getAndInc()] = nx;
		vertices[offsetV.getAndInc()] = ny;
		vertices[offsetV.getAndInc()] = nz;
		if(color != null)
		{
			vertices[offsetV.getAndInc()] = (float) color.getR();
			vertices[offsetV.getAndInc()] = (float) color.getG();
			vertices[offsetV.getAndInc()] = (float) color.getB();
		}
	}
	
	public Color getColor()
	{
		return color;
	}
	
	public void setColor(Color color)
	{
		this.color = color;
	}
	
	public Vector3 getPosition()
	{
		return position;
	}
	
	public Vector3 getSize()
	{
		return size;
	}
}

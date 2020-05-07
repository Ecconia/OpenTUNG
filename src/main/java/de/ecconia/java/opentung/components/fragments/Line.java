package de.ecconia.java.opentung.components.fragments;

import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.math.Vector3;

public class Line extends Meshable
{
	private final Vector3 start;
	private final Vector3 end;
	private final Color color;
	
	public Line(Vector3 start, Vector3 end, Color color)
	{
		this.start = start;
		this.end = end;
		this.color = color;
	}
	
	@Override
	public int getVCount()
	{
		return 2 * 6;
	}
	
	@Override
	public int getICount()
	{
		return 2;
	}
	
	@Override
	public void generateModel(float[] vertices, ModelHolder.IntHolder offsetV, short[] indices, ModelHolder.IntHolder offsetI, ModelHolder.IntHolder indexOffset, ModelHolder.TestModelType type, Vector3 offset)
	{
		vertices[offsetV.getAndInc()] = (float) start.getX();
		vertices[offsetV.getAndInc()] = (float) start.getX();
		vertices[offsetV.getAndInc()] = (float) start.getX();
		vertices[offsetV.getAndInc()] = (float) color.getR();
		vertices[offsetV.getAndInc()] = (float) color.getG();
		vertices[offsetV.getAndInc()] = (float) color.getB();
		
		vertices[offsetV.getAndInc()] = (float) end.getX();
		vertices[offsetV.getAndInc()] = (float) end.getX();
		vertices[offsetV.getAndInc()] = (float) end.getX();
		vertices[offsetV.getAndInc()] = (float) color.getR();
		vertices[offsetV.getAndInc()] = (float) color.getG();
		vertices[offsetV.getAndInc()] = (float) color.getB();
		
		int index = indexOffset.getAndInc(2);
		indices[offsetI.getAndInc()] = (short) (index + 0);
		indices[offsetI.getAndInc()] = (short) (index + 1);
	}
}

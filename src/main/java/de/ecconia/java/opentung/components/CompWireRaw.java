package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.math.Vector3;
import de.ecconia.java.opentung.models.DynamicWireModel;
import de.ecconia.java.opentung.models.GenericModel;

public class CompWireRaw extends CompGeneric
{
	public static DynamicWireModel model;
	
	private float length;
	private boolean powered;
	
	public CompWireRaw(CompContainer parent)
	{
		super(parent);
	}
	
	public void setPowered(boolean powered)
	{
		this.powered = powered;
	}
	
	public boolean isPowered()
	{
		return powered;
	}
	
	public void setLength(float length)
	{
		this.length = length;
	}
	
	public float getLength()
	{
		return length;
	}
	
	@Override
	public GenericModel getModel()
	{
		return model;
	}
	
	public Vector3 getEnd1()
	{
		Vector3 endPointer = new Vector3(0, 0, length / 2f);
		endPointer = getRotation().inverse().multiply(endPointer);
		return endPointer.add(getPosition());
	}
	
	public Vector3 getEnd2()
	{
		Vector3 endPointer = new Vector3(0, 0, length / 2f);
		endPointer = getRotation().inverse().multiply(endPointer).invert();
		return endPointer.add(getPosition());
	}
}

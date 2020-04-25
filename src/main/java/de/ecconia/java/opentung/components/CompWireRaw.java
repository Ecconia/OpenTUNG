package de.ecconia.java.opentung.components;

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
}

package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.models.GenericModel;

public class CompWire extends CompGeneric
{
	private float length;
	private boolean powered;
	
	public CompWire(CompContainer parent)
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
		throw new RuntimeException("No Model for this component yet.");
	}
}

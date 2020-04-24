package de.ecconia.java.opentung.components;

public class CompWire extends CompGeneric
{
	private float length;
	private boolean powered;
	
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
}

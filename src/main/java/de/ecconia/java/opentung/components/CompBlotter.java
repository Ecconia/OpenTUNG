package de.ecconia.java.opentung.components;

public class CompBlotter extends CompGeneric
{
	//Logic:
	boolean isPowered;
	
	public CompBlotter(CompContainer parent)
	{
		super(parent);
	}
	
	public void setPowered(boolean powered)
	{
		isPowered = powered;
	}
	
	public boolean isPowered()
	{
		return isPowered;
	}
}

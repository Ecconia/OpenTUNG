package de.ecconia.java.opentung.components.conductor;

import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;

public class CompWire extends Component
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
	public ModelHolder getModelHolder()
	{
		throw new RuntimeException("No Model for this component yet.");
	}
}

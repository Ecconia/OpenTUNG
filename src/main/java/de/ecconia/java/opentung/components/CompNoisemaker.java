package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.models.GenericModel;

public class CompNoisemaker extends CompGeneric
{
	private float frequency;
	
	public CompNoisemaker(CompContainer parent)
	{
		super(parent);
	}
	
	public void setFrequency(float frequency)
	{
		this.frequency = frequency;
	}
	
	public float getFrequency()
	{
		return frequency;
	}
	
	@Override
	public GenericModel getModel()
	{
		throw new RuntimeException("No Model for this component yet.");
	}
}

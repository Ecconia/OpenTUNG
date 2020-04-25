package de.ecconia.java.opentung.components;

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
}

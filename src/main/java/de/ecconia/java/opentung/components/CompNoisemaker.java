package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;

public class CompNoisemaker extends Component
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
	public ModelHolder getModelHolder()
	{
		throw new RuntimeException("No Model for this component yet.");
	}
}

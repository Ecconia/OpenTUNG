package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.models.GenericModel;
import de.ecconia.java.opentung.models.ThroughPegModel;

public class CompThroughPeg extends CompGeneric
{
	public static ThroughPegModel model;
	
	public CompThroughPeg(CompContainer parent)
	{
		super(parent);
	}
	
	@Override
	public GenericModel getModel()
	{
		return model;
	}
}

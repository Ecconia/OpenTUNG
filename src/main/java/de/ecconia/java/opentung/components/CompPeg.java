package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.models.GenericModel;
import de.ecconia.java.opentung.models.PegModel;

public class CompPeg extends CompGeneric
{
	public static PegModel model;
	
	public CompPeg(CompContainer parent)
	{
		super(parent);
	}
	
	@Override
	public GenericModel getModel()
	{
		return model;
	}
}

package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.models.GenericModel;
import de.ecconia.java.opentung.models.InverterModel;

public class CompInverter extends CompGeneric
{
	public static InverterModel model;
	
	public CompInverter(CompContainer parent)
	{
		super(parent);
	}
	
	@Override
	public GenericModel getModel()
	{
		return model;
	}
}

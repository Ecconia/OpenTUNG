package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.models.GenericModel;
import de.ecconia.java.opentung.models.SnappingPegModel;

public class CompSnappingPeg extends CompGeneric
{
	public static SnappingPegModel model;
	
	public CompSnappingPeg(CompContainer parent)
	{
		super(parent);
	}
	
	@Override
	public GenericModel getModel()
	{
		return model;
	}
}

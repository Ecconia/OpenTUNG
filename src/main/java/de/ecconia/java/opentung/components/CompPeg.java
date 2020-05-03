package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.models.GenericModel;
import de.ecconia.java.opentung.models.PegModel;

public class CompPeg extends Component
{
	public static PegModel model;
	
	public static void initGL()
	{
		model = new PegModel();
	}
	
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

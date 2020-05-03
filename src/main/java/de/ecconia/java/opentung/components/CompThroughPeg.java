package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.models.GenericModel;
import de.ecconia.java.opentung.models.ThroughPegModel;

public class CompThroughPeg extends Component
{
	public static ThroughPegModel model;
	
	public static void initGL()
	{
		model = new ThroughPegModel();
	}
	
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

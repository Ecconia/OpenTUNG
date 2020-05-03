package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.models.GenericModel;
import de.ecconia.java.opentung.models.SnappingPegModel;

public class CompSnappingPeg extends Component
{
	public static SnappingPegModel model;
	
	public static void initGL()
	{
		model = new SnappingPegModel();
	}
	
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

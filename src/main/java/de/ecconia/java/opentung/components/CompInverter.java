package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.models.GenericModel;
import de.ecconia.java.opentung.models.InverterModel;

public class CompInverter extends Component
{
	public static InverterModel model;
	
	public static void initGL()
	{
		model = new InverterModel();
	}
	
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

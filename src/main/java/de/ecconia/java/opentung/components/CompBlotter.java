package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.models.BlotterModel;
import de.ecconia.java.opentung.models.GenericModel;

public class CompBlotter extends Component
{
	public static BlotterModel model;
	
	public static void initGL()
	{
		model = new BlotterModel();
	}
	
	//Logic:
	boolean isPowered;
	
	public CompBlotter(CompContainer parent)
	{
		super(parent);
	}
	
	public void setPowered(boolean powered)
	{
		isPowered = powered;
	}
	
	public boolean isPowered()
	{
		return isPowered;
	}
	
	@Override
	public GenericModel getModel()
	{
		return model;
	}
}

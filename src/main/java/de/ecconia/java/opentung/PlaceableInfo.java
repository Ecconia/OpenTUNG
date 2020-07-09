package de.ecconia.java.opentung;

import de.ecconia.java.opentung.components.meta.ModelHolder;

public class PlaceableInfo
{
	private ModelHolder model;
	private int index;
	
	public PlaceableInfo(ModelHolder model)
	{
		this.model = model;
	}
	
	public ModelHolder getModel()
	{
		return model;
	}
	
	public void setIndex(int index)
	{
		this.index = index;
	}
	
	public int getIndex()
	{
		return index;
	}
}

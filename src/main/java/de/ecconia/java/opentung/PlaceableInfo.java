package de.ecconia.java.opentung;

import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;

public class PlaceableInfo
{
	private final ModelHolder model;
	private final CompGenerator generator;
	
	private int index;
	
	public PlaceableInfo(ModelHolder model, CompGenerator generator)
	{
		this.model = model;
		this.generator = generator;
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
	
	public Component instance(CompContainer container)
	{
		return generator.generateComponent(container);
	}
	
	public interface CompGenerator
	{
		Component generateComponent(CompContainer parent);
	}
}

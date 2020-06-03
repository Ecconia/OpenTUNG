package de.ecconia.java.opentung;

import de.ecconia.java.opentung.components.meta.Component;

public class Port
{
	private final Component component;
	private final int index;
	
	public Port(Component component, int index)
	{
		this.component = component;
		this.index = index;
	}
	
	public Component getComponent()
	{
		return component;
	}
	
	public int getIndex()
	{
		return index;
	}
}

package de.ecconia.java.opentung.core;

import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.simulation.Wire;
import java.util.List;

public class GrabData
{
	private final CompContainer grabbedParent;
	private final Component grabbedComponent;
	private final List<Wire> grabbedWires;
	
	public GrabData(CompContainer grabbedParent, Component grabbedComponent, List<Wire> grabbedWires)
	{
		this.grabbedParent = grabbedParent;
		this.grabbedComponent = grabbedComponent;
		this.grabbedWires = grabbedWires;
	}
	
	public Component getComponent()
	{
		return grabbedComponent;
	}
	
	public CompContainer getParent()
	{
		return grabbedParent;
	}
	
	public List<Wire> getWires()
	{
		return grabbedWires;
	}
}

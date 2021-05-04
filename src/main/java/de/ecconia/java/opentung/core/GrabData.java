package de.ecconia.java.opentung.core;

import de.ecconia.java.opentung.components.CompLabel;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.simulation.Wire;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class GrabData
{
	private final CompContainer grabbedParent;
	private final Component grabbedComponent;
	private final List<Component> components = new ArrayList<>();
	//Currently wires are stored redundantly, but convenience. TODO to be improved
	private final List<WireContainer> grabbedWiresWithSide = new ArrayList<>();
	private final List<Wire> grabbedWires = new ArrayList<>();
	//Grabbed labels:
	private final LinkedList<CompLabel> labels = new LinkedList<>();
	
	public GrabData(CompContainer grabbedParent, Component grabbedComponent)
	{
		this.grabbedParent = grabbedParent;
		this.grabbedComponent = grabbedComponent;
	}
	
	public void addComponent(Component component)
	{
		components.add(component);
	}
	
	public List<Component> getComponents()
	{
		return components;
	}
	
	public void addWire(Wire wire, boolean isGrabSideA)
	{
		grabbedWiresWithSide.add(new WireContainer(wire, isGrabSideA));
		grabbedWires.add(wire);
	}
	
	public void addLabel(CompLabel label)
	{
		labels.addLast(label);
	}
	
	public LinkedList<CompLabel> getLabels()
	{
		return labels;
	}
	
	public boolean hasLabels()
	{
		return !labels.isEmpty();
	}
	
	public Component getComponent()
	{
		return grabbedComponent;
	}
	
	public CompContainer getParent()
	{
		return grabbedParent;
	}
	
	public List<WireContainer> getWiresWithSides()
	{
		return grabbedWiresWithSide;
	}
	
	public List<Wire> getWires()
	{
		return grabbedWires;
	}
	
	public static class WireContainer
	{
		public Wire wire;
		public boolean isGrabbedOnASide;
		
		public WireContainer(Wire wire, boolean isGrabbedOnASide)
		{
			this.wire = wire;
			this.isGrabbedOnASide = isGrabbedOnASide;
		}
	}
}

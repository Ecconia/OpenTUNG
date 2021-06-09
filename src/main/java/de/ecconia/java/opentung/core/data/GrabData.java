package de.ecconia.java.opentung.core.data;

import de.ecconia.java.opentung.components.CompLabel;
import de.ecconia.java.opentung.components.conductor.CompWireRaw;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class GrabData
{
	private Component grabbedComponent;
	
	private final CompContainer grabbedParent;
	private final List<Component> components = new ArrayList<>();
	//Currently wires are stored redundantly, but convenience. TODO to be improved
	private final List<WireContainer> grabbedWiresWithSide = new ArrayList<>();
	private final List<CompWireRaw> grabbedWires = new ArrayList<>();
	//Grabbed labels:
	private final LinkedList<CompLabel> labels = new LinkedList<>();
	
	private List<CompWireRaw> internalWires;
	
	public GrabData(CompContainer grabbedParent, Component grabbedComponent)
	{
		this.grabbedParent = grabbedParent;
		this.grabbedComponent = grabbedComponent;
	}
	
	public void overwriteGrabbedComponent(Component grabbedComponent)
	{
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
	
	public void addWire(CompWireRaw wire, boolean isGrabSideA)
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
	
	public List<WireContainer> getOutgoingWiresWithSides()
	{
		return grabbedWiresWithSide;
	}
	
	public List<CompWireRaw> getOutgoingWires()
	{
		return grabbedWires;
	}
	
	public void setInternalWires(List<CompWireRaw> internalWires)
	{
		this.internalWires = internalWires;
	}
	
	public List<CompWireRaw> getInternalWires()
	{
		return internalWires;
	}
	
	//Copy code:
	
	private boolean isCopy;
	
	public void setCopy()
	{
		isCopy = true;
	}
	
	public boolean isCopy()
	{
		return isCopy;
	}
	
	//Classes:
	
	public static class WireContainer
	{
		public CompWireRaw wire;
		public boolean isGrabbedOnASide;
		
		public WireContainer(CompWireRaw wire, boolean isGrabbedOnASide)
		{
			this.wire = wire;
			this.isGrabbedOnASide = isGrabbedOnASide;
		}
	}
}

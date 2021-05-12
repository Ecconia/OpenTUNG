package de.ecconia.java.opentung.core;

import de.ecconia.java.opentung.components.CompSnappingWire;
import de.ecconia.java.opentung.components.conductor.CompWireRaw;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.util.math.Quaternion;
import java.util.List;

public class GrabContainerData extends GrabData
{
	private List<CompWireRaw> internalWires;
	private List<CompSnappingWire> internalSnappingWires;
	private Quaternion grabRotation = null;
	
	public GrabContainerData(CompContainer parent, Component component)
	{
		super(parent, component);
	}
	
	public void setInternalWires(List<CompWireRaw> internalWires)
	{
		this.internalWires = internalWires;
	}
	
	public void setInternalSnappingWires(List<CompSnappingWire> internalSnappingWires)
	{
		this.internalSnappingWires = internalSnappingWires;
	}
	
	public List<CompWireRaw> getInternalWires()
	{
		return internalWires;
	}
	
	public List<CompSnappingWire> getInternalSnappingWires()
	{
		return internalSnappingWires;
	}
	
	public Quaternion getAlignment()
	{
		return grabRotation;
	}
	
	public void setAlignment(Quaternion grabRotation)
	{
		this.grabRotation = grabRotation;
	}
}

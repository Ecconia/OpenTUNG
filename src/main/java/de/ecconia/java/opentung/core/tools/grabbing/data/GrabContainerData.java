package de.ecconia.java.opentung.core.tools.grabbing.data;

import java.util.Collection;
import java.util.List;

import de.ecconia.java.opentung.components.CompSnappingPeg;
import de.ecconia.java.opentung.components.CompSnappingWire;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.util.math.Quaternion;

public class GrabContainerData extends GrabData
{
	private List<CompSnappingWire> internalSnappingWires;
	private Quaternion grabRotation = null;
	private Collection<CompSnappingPeg> unconnectedSnappingPegs;
	
	public GrabContainerData(CompContainer parent, Component component)
	{
		super(parent, component);
	}
	
	public void setInternalSnappingWires(List<CompSnappingWire> internalSnappingWires)
	{
		this.internalSnappingWires = internalSnappingWires;
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
	
	public Collection<CompSnappingPeg> getUnconnectedSnappingPegs()
	{
		return unconnectedSnappingPegs;
	}
	
	public void setUnconnectedSnappingPegs(Collection<CompSnappingPeg> unconnectedSnappingPegs)
	{
		this.unconnectedSnappingPegs = unconnectedSnappingPegs;
	}
}

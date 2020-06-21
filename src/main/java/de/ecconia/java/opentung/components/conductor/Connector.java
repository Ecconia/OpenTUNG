package de.ecconia.java.opentung.components.conductor;

import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.math.Vector3;
import de.ecconia.java.opentung.simulation.Cluster;
import java.util.ArrayList;
import java.util.List;

public abstract class Connector
{
	private final Component base;
	private final CubeFull model;
	private final List<CompWireRaw> wires = new ArrayList<>();
	
	private Cluster cluster;
	
	public Connector(Component base, CubeFull model)
	{
		this.base = base;
		this.model = model;
	}
	
	public void setCluster(Cluster cluster)
	{
		this.cluster = cluster;
	}
	
	public void addWire(CompWireRaw wire)
	{
		wires.add(wire);
	}
	
	public CubeFull getModel()
	{
		return model;
	}
	
	public Component getBase()
	{
		return base;
	}
	
	public boolean contains(Vector3 probe)
	{
		return model.contains(probe);
	}
}

package de.ecconia.java.opentung.components.conductor;

import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.math.Vector3;
import de.ecconia.java.opentung.simulation.Cluster;
import de.ecconia.java.opentung.simulation.Clusterable;
import de.ecconia.java.opentung.simulation.Wire;
import java.util.ArrayList;
import java.util.List;

public abstract class Connector implements Clusterable
{
	private final Component base;
	private final CubeFull model;
	private final List<Wire> wires = new ArrayList<>();
	
	private Cluster cluster;
	
	public Connector(Component base, CubeFull model)
	{
		this.base = base;
		this.model = model;
	}
	
	@Override
	public void setCluster(Cluster cluster)
	{
		this.cluster = cluster;
	}
	
	@Override
	public boolean hasCluster()
	{
		return cluster != null;
	}
	
	@Override
	public Cluster getCluster()
	{
		return cluster;
	}
	
	public void addWire(Wire wire)
	{
		wires.add(wire);
	}
	
	public List<Wire> getWires()
	{
		return wires;
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

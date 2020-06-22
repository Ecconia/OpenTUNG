package de.ecconia.java.opentung.simulation;

import de.ecconia.java.opentung.components.conductor.Connector;
import java.util.ArrayList;
import java.util.List;

public abstract class Cluster
{
	private final List<Connector> connectors = new ArrayList<>();
	private final List<Wire> wires = new ArrayList<>();
	private final int id;
	
	public Cluster(int id)
	{
		this.id = id;
	}
	
	public int getId()
	{
		return id;
	}
	
	public void addConnector(Connector connector)
	{
		connectors.add(connector);
	}
	
	public void addWire(Wire wire)
	{
		wires.add(wire);
	}
	
	public List<Wire> getWires()
	{
		return wires;
	}
	
	public abstract boolean isActive();
}

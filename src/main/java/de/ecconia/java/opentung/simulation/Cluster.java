package de.ecconia.java.opentung.simulation;

import de.ecconia.java.opentung.components.conductor.Connector;
import de.ecconia.java.opentung.components.conductor.Peg;
import de.ecconia.java.opentung.meshing.ConductorMeshBag;
import de.ecconia.java.opentung.meshing.ConductorMeshBagReference;
import java.util.ArrayList;
import java.util.List;

public abstract class Cluster implements Updateable
{
	private final List<Connector> connectors = new ArrayList<>();
	private final List<Wire> wires = new ArrayList<>();
	private final List<ConductorMeshBagReference> conductorMeshBags = new ArrayList<>();
	
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
	
	protected void updateContent(SimulationManager simulation)
	{
		for(Connector connector : connectors)
		{
			//TBI: Extra list?
			if(connector instanceof Peg)
			{
				Peg peg = (Peg) connector;
				if(peg.getParent() instanceof Updateable)
				{
					simulation.updateNextTick((Updateable) connector.getParent());
				}
			}
		}
	}
	
	public List<Connector> getConnectors()
	{
		return connectors;
	}
	
	public void updateState()
	{
		boolean active = isActive();
		for(ConductorMeshBagReference meshBag : conductorMeshBags)
		{
			meshBag.setActive(active);
		}
	}
	
	public void remove(Connector current)
	{
		connectors.remove(current);
	}
	
	public void remove(Wire wire)
	{
		wires.remove(wire);
	}
	
	public void addMeshReference(ConductorMeshBagReference meshBagReference)
	{
		conductorMeshBags.add(meshBagReference);
	}
	
	public void removeMeshReference(ConductorMeshBag conductorMeshBagOriginal)
	{
		//TODO: Store and remove the reference instead.
		conductorMeshBags.removeIf(meshBagReference -> meshBagReference.getConductorMeshBag() == conductorMeshBagOriginal);
	}
	
	//Is queued for update?
	//TODO: Implement this, if you have clusters with 10k and more components, it will be relevant. Smaller cases are not relevant. For testing purposes not yet implemented.
	
	@Override
	public boolean isQueuedForUpdate()
	{
		return false;
	}
	
	@Override
	public void setQueuedForUpdate(boolean state)
	{
	}
}

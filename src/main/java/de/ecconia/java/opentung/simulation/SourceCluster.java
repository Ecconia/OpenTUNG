package de.ecconia.java.opentung.simulation;

import de.ecconia.java.opentung.components.conductor.Blot;
import java.util.ArrayList;
import java.util.List;

public class SourceCluster extends Cluster
{
	private final Blot source;
	private List<InheritingCluster> drains = new ArrayList<>();
	private boolean active;
	
	public SourceCluster(int id, Blot source)
	{
		super(id);
		this.source = source;
	}
	
	@Override
	public boolean isActive()
	{
		return active;
	}
	
	public void addDrain(InheritingCluster cluster)
	{
		drains.add(cluster);
	}
	
	public void forceUpdateON()
	{
		active = true;
		for(InheritingCluster drain : drains)
		{
			drain.forceUpdateON();
		}
	}
	
	@Override
	public void update(SimulationManager simulation)
	{
		//This quite the ugly call.
		boolean sourceState = ((Powerable) source.getParent()).isPowered();
		if(sourceState != active)
		{
			active = sourceState; //Change the internal memory state, for next update.
			updateContent(simulation); //Update all the components connected to this cluster.
			simulation.changeState(getId(), active);
			if(active)
			{
				for(InheritingCluster drain : drains)
				{
					drain.oneIn(simulation);
				}
			}
			else
			{
				for(InheritingCluster drain : drains)
				{
					drain.oneOut(simulation);
				}
			}
		}
	}
	
	@Override
	public String toString()
	{
		return "cs" + getId();
	}
	
	public void remove(InheritingCluster drain)
	{
		drains.remove(drain);
	}
}

package de.ecconia.java.opentung.simulation;

import java.util.ArrayList;
import java.util.List;

import de.ecconia.java.opentung.components.conductor.Blot;

public class SourceCluster extends Cluster
{
	private final Blot source;
	private final List<InheritingCluster> drains = new ArrayList<>();
	private boolean active;
	
	public SourceCluster(Blot source)
	{
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
		boolean sourceState = source.isPowered();
		if(sourceState != active)
		{
			active = sourceState; //Change the internal memory state, for next update.
			updateContent(simulation); //Update all the components connected to this cluster.
			updateState();
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
		return "Source#" + hashCode();
	}
	
	public void remove(InheritingCluster drain)
	{
		drains.remove(drain);
	}
	
	public Blot getSource()
	{
		return source;
	}
	
	public List<InheritingCluster> getDrains()
	{
		return drains;
	}
}

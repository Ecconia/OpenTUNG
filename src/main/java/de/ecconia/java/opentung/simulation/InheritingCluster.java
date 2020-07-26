package de.ecconia.java.opentung.simulation;

import java.util.ArrayList;
import java.util.List;

public class InheritingCluster extends Cluster
{
	private List<SourceCluster> sources = new ArrayList<>();
	
	private int activeSources = 0;
	private boolean lastState;
	
	public InheritingCluster(int id)
	{
		super(id);
	}
	
	@Override
	public boolean isActive()
	{
		return activeSources != 0;
	}
	
	public void addSource(SourceCluster cluster)
	{
		sources.add(cluster);
	}
	
	public List<SourceCluster> getSources()
	{
		return sources;
	}
	
	public void forceUpdateON()
	{
		activeSources++;
		lastState = true;
	}
	
	@Override
	public void update(SimulationManager simulation)
	{
		if(isActive() != lastState)
		{
			//Yep time to update.
			updateContent(simulation);
			lastState = isActive();
			simulation.changeState(getId(), lastState);
		}
	}
	
	public void oneIn(SimulationManager simulation)
	{
		activeSources++;
		simulation.updateNextStage(this);
	}
	
	public void oneOut(SimulationManager simulation)
	{
		activeSources--;
		simulation.updateNextStage(this);
	}
	
	@Override
	public String toString()
	{
		return "ci" + getId();
	}
	
	public void remove(SourceCluster sourceCluster)
	{
		sources.remove(sourceCluster);
	}
}

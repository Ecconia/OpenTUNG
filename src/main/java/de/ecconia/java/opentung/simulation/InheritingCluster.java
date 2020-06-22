package de.ecconia.java.opentung.simulation;

import java.util.ArrayList;
import java.util.List;

public class InheritingCluster extends Cluster
{
	private List<SourceCluster> sources = new ArrayList<>();
	
	private int activeSources = 0;
	
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
	
	public void forceUpdateON()
	{
		activeSources++;
	}
}

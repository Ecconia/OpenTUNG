package de.ecconia.java.opentung.simulation;

import java.util.ArrayList;
import java.util.List;

public class SourceCluster extends Cluster
{
	private List<InheritingCluster> drains = new ArrayList<>();
	private boolean active;
	
	public SourceCluster(int id)
	{
		super(id);
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
}

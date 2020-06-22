package de.ecconia.java.opentung.simulation;

import java.util.ArrayList;
import java.util.List;

public class SourceCluster extends Cluster
{
	private List<InheritingCluster> drains = new ArrayList<>();
	
	public SourceCluster(int id)
	{
		super(id);
	}
	
	public void addDrain(InheritingCluster cluster)
	{
		drains.add(cluster);
	}
}

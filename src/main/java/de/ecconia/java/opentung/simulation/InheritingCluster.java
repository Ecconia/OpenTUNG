package de.ecconia.java.opentung.simulation;

import java.util.ArrayList;
import java.util.List;

public class InheritingCluster extends Cluster
{
	private List<SourceCluster> sources = new ArrayList<>();
	
	public InheritingCluster(int id)
	{
		super(id);
	}
	
	public void addSource(SourceCluster cluster)
	{
		sources.add(cluster);
	}
}

package de.ecconia.java.opentung.components.conductor;

import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.simulation.SourceCluster;

public class Blot extends Connector
{
	public Blot(Component base, CubeFull model)
	{
		super(base, model);
	}
	
	public void forceUpdateON()
	{
		SourceCluster cluster = (SourceCluster) getCluster();
		cluster.forceUpdateON();
	}
}

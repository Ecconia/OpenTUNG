package de.ecconia.java.opentung.components.conductor;

import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.simulation.Powerable;
import de.ecconia.java.opentung.simulation.SourceCluster;

public class Blot extends Connector
{
	private final int index;
	
	public Blot(Component base, int index, CubeFull model)
	{
		super(base, model);
		
		this.index = index;
	}
	
	public void forceUpdateON()
	{
		SourceCluster cluster = (SourceCluster) getCluster();
		cluster.forceUpdateON();
	}
	
	public boolean isPowered()
	{
		return ((Powerable) getParent()).isPowered(index);
	}
}

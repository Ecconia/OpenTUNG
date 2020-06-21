package de.ecconia.java.opentung.components.conductor;

import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.math.Vector3;

public class Blot
{
	private final Component base;
	private final CubeFull model;
	
	public Blot(Component base, CubeFull model)
	{
		this.base = base;
		this.model = model;
	}
	
	public CubeFull getModel()
	{
		return model;
	}
	
	public boolean contains(Vector3 probe)
	{
		return model.contains(probe);
	}
}

package de.ecconia.java.opentung.components.meta;

import de.ecconia.java.opentung.simulation.Updateable;

public abstract class LogicComponent extends ConnectedComponent implements Updateable
{
	public LogicComponent(Component parent)
	{
		super(parent);
	}
}

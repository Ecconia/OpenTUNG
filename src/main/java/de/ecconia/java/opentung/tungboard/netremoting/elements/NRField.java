package de.ecconia.java.opentung.tungboard.netremoting.elements;

import de.ecconia.java.opentung.tungboard.netremoting.NRParseBundle;

public abstract class NRField
{
	private String name;
	
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	public abstract NRField copy();
	
	public abstract void parseContent(NRParseBundle b);
}

package de.ecconia.java.opentung.tungboard.netremoting.elements;

import de.ecconia.java.opentung.tungboard.netremoting.ParseBundle;

public abstract class Field
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
	
	public abstract Field copy();
	
	public abstract void parseContent(ParseBundle b);
}

package de.ecconia.java.opentung.tungboard.netremoting.elements;

public abstract class Object
{
	protected int id;
	
	public Object()
	{
	}
	
	public Object(int id)
	{
		this.id = id;
	}
	
	public int getId()
	{
		return id;
	}
	
	public void setId(int id)
	{
		this.id = id;
	}
}

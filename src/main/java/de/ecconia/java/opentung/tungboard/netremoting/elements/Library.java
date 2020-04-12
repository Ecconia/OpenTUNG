package de.ecconia.java.opentung.tungboard.netremoting.elements;

import de.ecconia.java.opentung.tungboard.netremoting.ParseBundle;

public class Library extends Object
{
	private String name;
	
	public Library(ParseBundle b)
	{
		b.readAndStoreID(this);
		name = b.string();
		
//		System.out.println("Library: ID: " + id + " Name: " + name);
	}
	
	public Library(int id, String name)
	{
		super(id);
		this.name = name;
	}
	
	public String getName()
	{
		return name;
	}
}

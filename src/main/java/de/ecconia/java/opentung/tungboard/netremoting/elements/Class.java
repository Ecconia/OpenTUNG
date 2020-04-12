package de.ecconia.java.opentung.tungboard.netremoting.elements;

public abstract class Class extends Object
{
	protected Library library;
	protected Field[] fields;
	protected String name;
	
	public Library getLibrary()
	{
		return library;
	}
	
	public Field[] getFields()
	{
		return fields;
	}
	
	public String getName()
	{
		return name;
	}
	
	protected Field[] getFieldCopy()
	{
		if(fields == null)
		{
			throw new RuntimeException("Requested field copy of class " + this.getClass().getSimpleName() + ", but fields are not set yet.");
		}
		
		Field[] fieldsCopy = new Field[fields.length];
		for(int i = 0; i < fields.length; i++)
		{
			fieldsCopy[i] = fields[i].copy();
		}
		
		return fieldsCopy;
	}
}

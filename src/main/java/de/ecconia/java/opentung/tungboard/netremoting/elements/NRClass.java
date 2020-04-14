package de.ecconia.java.opentung.tungboard.netremoting.elements;

public abstract class NRClass extends NRObject
{
	protected NRLibrary library;
	protected NRField[] fields;
	protected String name;
	
	public NRLibrary getLibrary()
	{
		return library;
	}
	
	public NRField[] getFields()
	{
		return fields;
	}
	
	public String getName()
	{
		return name;
	}
	
	protected NRField[] getFieldCopy()
	{
		if(fields == null)
		{
			throw new RuntimeException("Requested field copy of class " + this.getClass().getSimpleName() + ", but fields are not set yet.");
		}
		
		NRField[] fieldsCopy = new NRField[fields.length];
		for(int i = 0; i < fields.length; i++)
		{
			fieldsCopy[i] = fields[i].copy();
		}
		
		return fieldsCopy;
	}
}

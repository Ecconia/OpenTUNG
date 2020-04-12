package de.ecconia.java.opentung.tungboard.netremoting.elements.fields;

import de.ecconia.java.opentung.tungboard.netremoting.ParseBundle;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Field;

public class Int32Field extends Field
{
	private int value;
	
	@Override
	public Field copy()
	{
		Field field = new Int32Field();
		field.setName(getName());
		return field;
	}
	
	@Override
	public void parseContent(ParseBundle b)
	{
		value = b.sInt();
		
//		System.out.println("Read int32: " + value);
	}
}

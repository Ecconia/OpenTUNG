package de.ecconia.java.opentung.tungboard.netremoting.elements.fields;

import de.ecconia.java.opentung.tungboard.netremoting.ParseBundle;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Field;

public class FloatField extends Field
{
	private float value;
	
	@Override
	public Field copy()
	{
		Field field = new FloatField();
		field.setName(getName());
		return field;
	}
	
	@Override
	public void parseContent(ParseBundle b)
	{
		value = b.reader.readFloatLE();
		
//		System.out.println("Read float: " + value);
	}
}

package de.ecconia.java.opentung.tungboard.netremoting.elements.fields;

import de.ecconia.java.opentung.tungboard.netremoting.ParseBundle;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Field;

public class BooleanField extends Field
{
	private boolean value;
	
	@Override
	public Field copy()
	{
		Field field = new BooleanField();
		field.setName(getName());
		return field;
	}
	
	@Override
	public void parseContent(ParseBundle b)
	{
		int val = b.uByte();
		if(val == 0)
		{
			value = false;
		}
		else if(val == 1)
		{
			value = true;
		}
		else
		{
			throw new RuntimeException("Non 0 or 1 boolean found: " + val);
		}
		
//		System.out.println("Read boolean: " + value);
	}
}

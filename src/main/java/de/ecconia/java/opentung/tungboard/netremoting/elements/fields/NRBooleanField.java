package de.ecconia.java.opentung.tungboard.netremoting.elements.fields;

import de.ecconia.java.opentung.tungboard.netremoting.NRParseBundle;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRField;

public class NRBooleanField extends NRField
{
	private boolean value;
	
	@Override
	public NRField copy()
	{
		NRField field = new NRBooleanField();
		field.setName(getName());
		return field;
	}
	
	@Override
	public void parseContent(NRParseBundle b)
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
	
	public boolean getValue()
	{
		return value;
	}
}

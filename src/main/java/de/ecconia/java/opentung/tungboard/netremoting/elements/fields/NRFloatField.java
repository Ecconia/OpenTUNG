package de.ecconia.java.opentung.tungboard.netremoting.elements.fields;

import de.ecconia.java.opentung.tungboard.netremoting.NRParseBundle;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRField;

public class NRFloatField extends NRField
{
	private float value;
	
	@Override
	public NRField copy()
	{
		NRField field = new NRFloatField();
		field.setName(getName());
		return field;
	}
	
	@Override
	public void parseContent(NRParseBundle b)
	{
		value = b.reader.readFloatLE();
		
//		System.out.println("Read float: " + value);
	}
	
	public float getValue()
	{
		return value;
	}
}

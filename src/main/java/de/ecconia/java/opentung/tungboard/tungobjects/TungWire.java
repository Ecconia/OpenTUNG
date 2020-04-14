package de.ecconia.java.opentung.tungboard.tungobjects;

import de.ecconia.java.opentung.tungboard.netremoting.elements.NRClass;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.NRBooleanField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.NRFloatField;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungObject;

public class TungWire extends TungObject
{
	private float length;
	private boolean inputInput;
	
	public TungWire(NRClass clazz)
	{
		for(NRField field : clazz.getFields())
		{
			if(checkField(field))
			{
				continue;
			}
			
			String name = field.getName();
			if("length".equals(name))
			{
				if(field instanceof NRFloatField)
				{
					length = ((NRFloatField) field).getValue();
				}
				else
				{
					throw new RuntimeException("Expected FloatField as inner value, but got " + field.getClass().getSimpleName());
				}
			}
			else if("InputInput".equals(name))
			{
				if(field instanceof NRBooleanField)
				{
					inputInput = ((NRBooleanField) field).getValue();
				}
				else
				{
					throw new RuntimeException("Expected BooleanField as inner value, but got " + field.getClass().getSimpleName());
				}
			}
		}
	}
	
	public float getLength()
	{
		return length;
	}
	
	public boolean isInputInput()
	{
		return inputInput;
	}
}

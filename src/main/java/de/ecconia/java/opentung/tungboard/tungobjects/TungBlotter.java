package de.ecconia.java.opentung.tungboard.tungobjects;

import de.ecconia.java.opentung.tungboard.netremoting.elements.NRClass;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.NRBooleanField;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungObject;

public class TungBlotter extends TungObject
{
	private boolean outputOn;
	
	public TungBlotter(NRClass clazz)
	{
		for(NRField field : clazz.getFields())
		{
			if(checkField(field))
			{
				continue;
			}
			
			String name = field.getName();
			if("OutputOn".equals(name))
			{
				if(field instanceof NRBooleanField)
				{
					outputOn = ((NRBooleanField) field).getValue();
				}
				else
				{
					throw new RuntimeException("Expected BooleanField as inner value, but got " + field.getClass().getSimpleName());
				}
			}
		}
	}
	
	public boolean isOutputOn()
	{
		return outputOn;
	}
}

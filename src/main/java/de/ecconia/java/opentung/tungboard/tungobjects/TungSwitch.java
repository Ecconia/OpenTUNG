package de.ecconia.java.opentung.tungboard.tungobjects;

import de.ecconia.java.opentung.tungboard.netremoting.elements.NRClass;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.NRBooleanField;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungObject;

public class TungSwitch extends TungObject
{
	private boolean isOn;
	
	public TungSwitch(NRClass clazz)
	{
		for(NRField field : clazz.getFields())
		{
			if(checkField(field))
			{
				continue;
			}
			
			String name = field.getName();
			if("on".equals(name))
			{
				if(field instanceof NRBooleanField)
				{
					isOn = ((NRBooleanField) field).getValue();
				}
				else
				{
					throw new RuntimeException("Expected BooleanField as inner value, but got " + field.getClass().getSimpleName());
				}
			}
		}
	}
	
	public boolean isOn()
	{
		return isOn;
	}
}

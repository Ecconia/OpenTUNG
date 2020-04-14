package de.ecconia.java.opentung.tungboard.tungobjects;

import de.ecconia.java.opentung.tungboard.netremoting.elements.Class;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Field;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.BooleanField;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungObject;

public class TungPanelSwitch extends TungObject
{
	private boolean isOn;
	
	public TungPanelSwitch(Class clazz)
	{
		for(Field field : clazz.getFields())
		{
			if(checkField(field))
			{
				continue;
			}
			
			String name = field.getName();
			if("on".equals(name))
			{
				if(field instanceof BooleanField)
				{
					isOn = ((BooleanField) field).getValue();
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

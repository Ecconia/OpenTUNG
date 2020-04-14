package de.ecconia.java.opentung.tungboard.tungobjects;

import de.ecconia.java.opentung.tungboard.netremoting.elements.Class;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Field;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.FloatField;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungObject;

public class TungNoisemaker extends TungObject
{
	private float frequency;
	
	public TungNoisemaker(Class clazz)
	{
		for(Field field : clazz.getFields())
		{
			if(checkField(field))
			{
				continue;
			}
			
			String name = field.getName();
			if("ToneFrequency".equals(name))
			{
				if(field instanceof FloatField)
				{
					frequency = ((FloatField) field).getValue();
				}
				else
				{
					throw new RuntimeException("Expected FloatField as inner value, but got " + field.getClass().getSimpleName());
				}
			}
		}
	}
	
	public float getFrequency()
	{
		return frequency;
	}
}

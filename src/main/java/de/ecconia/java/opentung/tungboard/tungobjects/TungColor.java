package de.ecconia.java.opentung.tungboard.tungobjects;

import de.ecconia.java.opentung.tungboard.netremoting.elements.Class;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Field;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.ClassField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.FloatField;

public class TungColor
{
	float r;
	float g;
	float b;
	
	public TungColor(Field field)
	{
		if(field instanceof ClassField)
		{
			Object value = ((ClassField) field).getValue();
			if(value == null)
			{
				throw new RuntimeException("Expected value, but got null");
			}
			
			if(value instanceof Class)
			{
				Class clazz = (Class) value;
				for(Field innerField : clazz.getFields())
				{
					String name = innerField.getName();
					if("r".equals(name))
					{
						if(innerField instanceof FloatField)
						{
							r = ((FloatField) innerField).getValue();
						}
						else
						{
							throw new RuntimeException("Expected FloatField as inner value, but got " + innerField.getClass().getSimpleName());
						}
					}
					else if("g".equals(name))
					{
						if(innerField instanceof FloatField)
						{
							g = ((FloatField) innerField).getValue();
						}
						else
						{
							throw new RuntimeException("Expected FloatField as inner value, but got " + innerField.getClass().getSimpleName());
						}
					}
					else if("b".equals(name))
					{
						if(innerField instanceof FloatField)
						{
							b = ((FloatField) innerField).getValue();
						}
						else
						{
							throw new RuntimeException("Expected FloatField as inner value, but got " + innerField.getClass().getSimpleName());
						}
					}
					else
					{
						//Ignore for now.
					}
				}
			}
			else
			{
				throw new RuntimeException("Expected Class as value, but got " + value.getClass().getSimpleName());
			}
		}
		else
		{
			throw new RuntimeException("Expected ClassField, but got " + field.getClass().getSimpleName());
		}
	}
}

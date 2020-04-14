package de.ecconia.java.opentung.tungboard.tungobjects;

import de.ecconia.java.opentung.tungboard.netremoting.elements.Class;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Field;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.ClassField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.FloatField;

public class TungPosition
{
	private float x;
	private float y;
	private float z;
	
	public TungPosition(Field field)
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
					if("x".equals(name))
					{
						if(innerField instanceof FloatField)
						{
							x = ((FloatField) innerField).getValue();
						}
						else
						{
							throw new RuntimeException("Expected FloatField as inner value, but got " + innerField.getClass().getSimpleName());
						}
					}
					else if("y".equals(name))
					{
						if(innerField instanceof FloatField)
						{
							y = ((FloatField) innerField).getValue();
						}
						else
						{
							throw new RuntimeException("Expected FloatField as inner value, but got " + innerField.getClass().getSimpleName());
						}
					}
					else if("z".equals(name))
					{
						if(innerField instanceof FloatField)
						{
							z = ((FloatField) innerField).getValue();
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
	
	public float getX()
	{
		return x;
	}
	
	public float getY()
	{
		return y;
	}
	
	public float getZ()
	{
		return z;
	}
}

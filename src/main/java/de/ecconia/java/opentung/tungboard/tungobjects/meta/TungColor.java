package de.ecconia.java.opentung.tungboard.tungobjects.meta;

import de.ecconia.java.opentung.tungboard.netremoting.elements.NRClass;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.NRClassField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.NRFloatField;

public class TungColor
{
	private float r;
	private float g;
	private float b;
	
	public TungColor(NRField field)
	{
		if(field instanceof NRClassField)
		{
			Object value = ((NRClassField) field).getValue();
			if(value == null)
			{
				throw new RuntimeException("Expected value, but got null");
			}
			
			if(value instanceof NRClass)
			{
				NRClass clazz = (NRClass) value;
				for(NRField innerField : clazz.getFields())
				{
					String name = innerField.getName();
					if("r".equals(name))
					{
						if(innerField instanceof NRFloatField)
						{
							r = ((NRFloatField) innerField).getValue();
						}
						else
						{
							throw new RuntimeException("Expected FloatField as inner value, but got " + innerField.getClass().getSimpleName());
						}
					}
					else if("g".equals(name))
					{
						if(innerField instanceof NRFloatField)
						{
							g = ((NRFloatField) innerField).getValue();
						}
						else
						{
							throw new RuntimeException("Expected FloatField as inner value, but got " + innerField.getClass().getSimpleName());
						}
					}
					else if("b".equals(name))
					{
						if(innerField instanceof NRFloatField)
						{
							b = ((NRFloatField) innerField).getValue();
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
	
	public float getR()
	{
		return r;
	}
	
	public float getG()
	{
		return g;
	}
	
	public float getB()
	{
		return b;
	}
}

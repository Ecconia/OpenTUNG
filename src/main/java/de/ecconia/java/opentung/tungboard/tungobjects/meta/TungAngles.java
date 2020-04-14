package de.ecconia.java.opentung.tungboard.tungobjects.meta;

import de.ecconia.java.opentung.tungboard.netremoting.elements.NRClass;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.NRClassField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.NRFloatField;
import de.ecconia.java.opentung.tungboard.tungobjects.common.Vector3;

public class TungAngles implements Vector3
{
	private float x;
	private float y;
	private float z;
	
	public TungAngles(NRField field)
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
					if("x".equals(name))
					{
						if(innerField instanceof NRFloatField)
						{
							x = ((NRFloatField) innerField).getValue();
						}
						else
						{
							throw new RuntimeException("Expected FloatField as inner value, but got " + innerField.getClass().getSimpleName());
						}
					}
					else if("y".equals(name))
					{
						if(innerField instanceof NRFloatField)
						{
							y = ((NRFloatField) innerField).getValue();
						}
						else
						{
							throw new RuntimeException("Expected FloatField as inner value, but got " + innerField.getClass().getSimpleName());
						}
					}
					else if("z".equals(name))
					{
						if(innerField instanceof NRFloatField)
						{
							z = ((NRFloatField) innerField).getValue();
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
	
	@Override
	public float getX()
	{
		return x;
	}
	
	@Override
	public float getY()
	{
		return y;
	}
	
	@Override
	public float getZ()
	{
		return z;
	}
	
	public void fix()
	{
		x = fix(x);
		y = fix(y);
		z = fix(z);
	}
	
	private float fix(float f)
	{
		f /= 22.5f;
		f = Math.round(f);
		f *= 22.5f;
		return f;
	}
}

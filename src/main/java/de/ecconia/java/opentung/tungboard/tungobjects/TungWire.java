package de.ecconia.java.opentung.tungboard.tungobjects;

import de.ecconia.java.opentung.tungboard.netremoting.elements.Class;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Field;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.BooleanField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.FloatField;

public class TungWire extends TungObject
{
	private TungAngles angle;
	private TungPosition position;
	
	private float length;
	private boolean inputInput;
	
	public TungWire(Class clazz)
	{
		for(Field field : clazz.getFields())
		{
			String name = field.getName();
			if("LocalEulerAngles".equals(name))
			{
				angle = new TungAngles(field);
			}
			else if("LocalPosition".equals(name))
			{
				position = new TungPosition(field);
			}
			else if("length".equals(name))
			{
				if(field instanceof FloatField)
				{
					length = ((FloatField) field).getValue();
				}
				else
				{
					throw new RuntimeException("Expected FloatField as inner value, but got " + field.getClass().getSimpleName());
				}
			}
			else if("InputInput".equals(name))
			{
				if(field instanceof BooleanField)
				{
					inputInput = ((BooleanField) field).getValue();
				}
				else
				{
					throw new RuntimeException("Expected BooleanField as inner value, but got " + field.getClass().getSimpleName());
				}
			}
		}
	}
	
	public TungAngles getAngle()
	{
		return angle;
	}
	
	public TungPosition getPosition()
	{
		return position;
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

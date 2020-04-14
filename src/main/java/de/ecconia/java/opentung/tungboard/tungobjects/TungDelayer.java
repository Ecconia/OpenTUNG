package de.ecconia.java.opentung.tungboard.tungobjects;

import de.ecconia.java.opentung.tungboard.netremoting.elements.Class;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Field;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.BooleanField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.Int32Field;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungObject;

public class TungDelayer extends TungObject
{
	private int delayCount;
	private boolean outputOn;
	
	public TungDelayer(Class clazz)
	{
		for(Field field : clazz.getFields())
		{
			if(checkField(field))
			{
				continue;
			}
			
			String name = field.getName();
			if("DelayCount".equals(name))
			{
				if(field instanceof Int32Field)
				{
					delayCount = ((Int32Field) field).getValue();
				}
				else
				{
					throw new RuntimeException("Expected Int32Field as inner value, but got " + field.getClass().getSimpleName());
				}
			}
			else if("OutputOn".equals(name))
			{
				if(field instanceof BooleanField)
				{
					outputOn = ((BooleanField) field).getValue();
				}
				else
				{
					throw new RuntimeException("Expected BooleanField as inner value, but got " + field.getClass().getSimpleName());
				}
			}
		}
	}
	
	public int getDelayCount()
	{
		return delayCount;
	}
	
	public boolean isOutputOn()
	{
		return outputOn;
	}
}

package de.ecconia.java.opentung.tungboard.tungobjects;

import de.ecconia.java.opentung.tungboard.netremoting.elements.NRClass;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.NRBooleanField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.NRInt32Field;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungObject;

public class TungDelayer extends TungObject
{
	private int delayCount;
	private boolean outputOn;
	
	public TungDelayer(NRClass clazz)
	{
		for(NRField field : clazz.getFields())
		{
			if(checkField(field))
			{
				continue;
			}
			
			String name = field.getName();
			if("DelayCount".equals(name))
			{
				if(field instanceof NRInt32Field)
				{
					delayCount = ((NRInt32Field) field).getValue();
				}
				else
				{
					throw new RuntimeException("Expected Int32Field as inner value, but got " + field.getClass().getSimpleName());
				}
			}
			else if("OutputOn".equals(name))
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
	
	public int getDelayCount()
	{
		return delayCount;
	}
	
	public boolean isOutputOn()
	{
		return outputOn;
	}
}

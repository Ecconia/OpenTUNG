package de.ecconia.java.opentung.tungboard.tungobjects;

import de.ecconia.java.opentung.tungboard.netremoting.elements.Class;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Field;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.Int32Field;
import de.ecconia.java.opentung.tungboard.tungobjects.common.Angles;
import de.ecconia.java.opentung.tungboard.tungobjects.common.TungChildable;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungColor;

public class TungBoard extends TungChildable implements Angles
{
	public static final String NAME = "SavedObjects.SavedCircuitBoard";
	
	private int x;
	private int z;
	private TungColor color;
	
	public TungBoard(Class clazz)
	{
		for(Field field : clazz.getFields())
		{
			if(checkField(field))
			{
				continue;
			}
			
			String name = field.getName();
			if("color".equals(name))
			{
				color = new TungColor(field);
			}
			else if("x".equals(name))
			{
				if(field instanceof Int32Field)
				{
					x = ((Int32Field) field).getValue();
				}
				else
				{
					throw new RuntimeException("Expected Int32Field as inner value, but got " + field.getClass().getSimpleName());
				}
			}
			else if("z".equals(name))
			{
				if(field instanceof Int32Field)
				{
					z = ((Int32Field) field).getValue();
				}
				else
				{
					throw new RuntimeException("Expected Int32Field as inner value, but got " + field.getClass().getSimpleName());
				}
			}
		}
	}
	
	public int getX()
	{
		return x;
	}
	
	public int getZ()
	{
		return z;
	}
	
	public TungColor getColor()
	{
		return color;
	}
}

package de.ecconia.java.opentung.tungboard.tungobjects;

import de.ecconia.java.opentung.tungboard.netremoting.elements.NRClass;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.NRInt32Field;
import de.ecconia.java.opentung.tungboard.tungobjects.common.Angles;
import de.ecconia.java.opentung.tungboard.tungobjects.common.TungChildable;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungColor;

public class TungBoard extends TungChildable implements Angles
{
	public static final String NAME = "SavedObjects.SavedCircuitBoard";
	
	private int x;
	private int z;
	private TungColor color;
	
	public TungBoard(int x, int z, TungColor color)
	{
		this.x = x;
		this.z = z;
		this.color = color;
	}
	
	public TungBoard(int x, int z)
	{
		this(x, z, null); //Assume they always have a color.
	}
	
	public TungBoard(NRClass clazz)
	{
		for(NRField field : clazz.getFields())
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
				if(field instanceof NRInt32Field)
				{
					x = ((NRInt32Field) field).getValue();
				}
				else
				{
					throw new RuntimeException("Expected Int32Field as inner value, but got " + field.getClass().getSimpleName());
				}
			}
			else if("z".equals(name))
			{
				if(field instanceof NRInt32Field)
				{
					z = ((NRInt32Field) field).getValue();
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

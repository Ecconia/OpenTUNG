package de.ecconia.java.opentung.tungboard.tungobjects;

import de.ecconia.java.opentung.tungboard.netremoting.elements.Array;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Class;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Field;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Object;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.ClassField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.Int32Field;

import java.util.ArrayList;
import java.util.List;

public class TungBoard extends TungObject
{
	public static final String NAME = "SavedObjects.SavedCircuitBoard";
	
	private int x;
	private int z;
	private TungColor color;
	private TungAngles angle;
	private TungPosition position;
	
	private final List<TungObject> children = new ArrayList<>();
	
	public TungBoard(Class clazz)
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
			else if("color".equals(name))
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
			else if("Children".equals(name))
			{
				if(field instanceof ClassField)
				{
					//Drop class check, get value
					Object value = ((ClassField) field).getValue();
					if(value instanceof Array)
					{
						//Skip type check
						for(Field entry : ((Array) value).getEntries())
						{
							TungObject component = convertComponent(entry);
							if(component != null)
							{
								children.add(component);
							}
						}
					}
					else
					{
						throw new RuntimeException("Expected Array as value, but got " + field.getClass().getSimpleName());
					}
				}
				else
				{
					throw new RuntimeException("Expected ClassField as field, but got " + field.getClass().getSimpleName());
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
	
	public TungAngles getAngle()
	{
		return angle;
	}
	
	public TungPosition getPosition()
	{
		return position;
	}
	
	public TungColor getColor()
	{
		return color;
	}
	
	public List<TungObject> getChildren()
	{
		return children;
	}
}

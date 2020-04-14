package de.ecconia.java.opentung.tungboard.tungobjects.common;

import de.ecconia.java.opentung.tungboard.netremoting.elements.Array;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Field;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Object;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.ClassField;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungObject;

import java.util.ArrayList;
import java.util.List;

public class TungChildable extends TungObject
{
	private final List<TungObject> children = new ArrayList<>();
	
	@Override
	protected boolean checkField(Field field)
	{
		if(super.checkField(field))
		{
			return true;
		}
		
		String name = field.getName();
		if("Children".equals(name))
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
			
			return true;
		}
		
		return false;
	}
	
	public List<TungObject> getChildren()
	{
		return children;
	}
}

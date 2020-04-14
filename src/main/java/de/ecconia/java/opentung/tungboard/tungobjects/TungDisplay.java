package de.ecconia.java.opentung.tungboard.tungobjects;

import de.ecconia.java.opentung.tungboard.netremoting.elements.Class;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Field;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.ClassField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.Int32Field;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungColorEnum;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungObject;

public class TungDisplay extends TungObject
{
	private TungColorEnum color;
	
	public TungDisplay(Class clazz)
	{
		for(Field field : clazz.getFields())
		{
			if(checkField(field))
			{
				continue;
			}
			
			String name = field.getName();
			if("Color".equals(name))
			{
				ClassField cField = (ClassField) field;
				Class valueClass = (Class) cField.getValue();
				
				Int32Field val = (Int32Field) valueClass.getFields()[0]; //Risk error...
				color = TungColorEnum.lookup(val.getValue());
			}
		}
	}
	
	public TungColorEnum getColor()
	{
		return color;
	}
}

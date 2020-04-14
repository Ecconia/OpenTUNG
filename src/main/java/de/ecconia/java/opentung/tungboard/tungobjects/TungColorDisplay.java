package de.ecconia.java.opentung.tungboard.tungobjects;

import de.ecconia.java.opentung.tungboard.netremoting.elements.Class;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Field;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungObject;

public class TungColorDisplay extends TungObject
{
	public TungColorDisplay(Class clazz)
	{
		for(Field field : clazz.getFields())
		{
			checkField(field);
		}
	}
}

package de.ecconia.java.opentung.tungboard.tungobjects;

import de.ecconia.java.opentung.tungboard.netremoting.elements.Class;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Field;
import de.ecconia.java.opentung.tungboard.tungobjects.common.TungChildable;

public class TungMount extends TungChildable
{
	public TungMount(Class clazz)
	{
		for(Field field : clazz.getFields())
		{
			checkField(field);
		}
	}
}

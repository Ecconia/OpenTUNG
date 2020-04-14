package de.ecconia.java.opentung.tungboard.tungobjects;

import de.ecconia.java.opentung.tungboard.netremoting.elements.Class;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Field;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungObject;

public class TungPanelColorDisplay extends TungObject
{
	public TungPanelColorDisplay(Class clazz)
	{
		for(Field field : clazz.getFields())
		{
			checkField(field);
		}
	}
}

package de.ecconia.java.opentung.tungboard.tungobjects;

import de.ecconia.java.opentung.tungboard.netremoting.elements.NRClass;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRField;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungObject;

public class TungButton extends TungObject
{
	public TungButton(NRClass clazz)
	{
		for(NRField field : clazz.getFields())
		{
			checkField(field);
		}
	}
}

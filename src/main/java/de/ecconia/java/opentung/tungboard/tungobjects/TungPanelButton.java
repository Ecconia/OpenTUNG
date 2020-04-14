package de.ecconia.java.opentung.tungboard.tungobjects;

import de.ecconia.java.opentung.tungboard.netremoting.elements.NRClass;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRField;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungObject;

public class TungPanelButton extends TungObject
{
	public TungPanelButton(NRClass clazz)
	{
		for(NRField field : clazz.getFields())
		{
			checkField(field);
		}
	}
}

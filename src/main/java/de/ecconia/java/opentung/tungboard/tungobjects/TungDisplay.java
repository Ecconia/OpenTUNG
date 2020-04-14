package de.ecconia.java.opentung.tungboard.tungobjects;

import de.ecconia.java.opentung.tungboard.netremoting.elements.NRClass;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.NRClassField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.NRInt32Field;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungColorEnum;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungObject;

public class TungDisplay extends TungObject
{
	private TungColorEnum color;
	
	public TungDisplay(NRClass clazz)
	{
		for(NRField field : clazz.getFields())
		{
			if(checkField(field))
			{
				continue;
			}
			
			String name = field.getName();
			if("Color".equals(name))
			{
				NRClassField cField = (NRClassField) field;
				NRClass valueClass = (NRClass) cField.getValue();
				
				NRInt32Field val = (NRInt32Field) valueClass.getFields()[0]; //Risk error...
				color = TungColorEnum.lookup(val.getValue());
			}
		}
	}
	
	public TungColorEnum getColor()
	{
		return color;
	}
}

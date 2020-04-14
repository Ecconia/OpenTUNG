package de.ecconia.java.opentung.tungboard.tungobjects;

import de.ecconia.java.opentung.tungboard.netremoting.elements.NRClass;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.NRFloatField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.NRStringField;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungObject;

public class TungPanelLabel extends TungObject
{
	private float fontSize;
	private String text;
	
	public TungPanelLabel(NRClass clazz)
	{
		for(NRField field : clazz.getFields())
		{
			if(checkField(field))
			{
				continue;
			}
			
			String name = field.getName();
			if("FontSize".equals(name))
			{
				if(field instanceof NRFloatField)
				{
					fontSize = ((NRFloatField) field).getValue();
				}
				else
				{
					throw new RuntimeException("Expected FloatField as inner value, but got " + field.getClass().getSimpleName());
				}
			}
			else if("text".equals(name))
			{
				if(field instanceof NRStringField)
				{
					text = ((NRStringField) field).getValue();
				}
				else
				{
					throw new RuntimeException("Expected BooleanField as inner value, but got " + field.getClass().getSimpleName());
				}
			}
		}
	}
	
	public float getFontSize()
	{
		return fontSize;
	}
	
	public String getText()
	{
		return text;
	}
}

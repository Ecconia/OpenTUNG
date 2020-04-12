package de.ecconia.java.opentung.tungboard.tungobjects;

import de.ecconia.java.opentung.tungboard.netremoting.elements.Class;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Field;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.FloatField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.StringField;

public class TungPanelLabel extends TungObject
{
	private TungAngles angle;
	private TungPosition position;
	
	private float fontSize;
	private String text;
	
	public TungPanelLabel(Class clazz)
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
			else if("FontSize".equals(name))
			{
				if(field instanceof FloatField)
				{
					fontSize = ((FloatField) field).getValue();
				}
				else
				{
					throw new RuntimeException("Expected FloatField as inner value, but got " + field.getClass().getSimpleName());
				}
			}
			else if("text".equals(name))
			{
				if(field instanceof StringField)
				{
					text = ((StringField) field).getValue();
				}
				else
				{
					throw new RuntimeException("Expected BooleanField as inner value, but got " + field.getClass().getSimpleName());
				}
			}
		}
	}
	
	public TungAngles getAngle()
	{
		return angle;
	}
	
	public TungPosition getPosition()
	{
		return position;
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

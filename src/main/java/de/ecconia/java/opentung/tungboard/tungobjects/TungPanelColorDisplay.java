package de.ecconia.java.opentung.tungboard.tungobjects;

import de.ecconia.java.opentung.tungboard.netremoting.elements.Class;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Field;

public class TungPanelColorDisplay extends TungObject
{
	private TungAngles angle;
	private TungPosition position;
	
	public TungPanelColorDisplay(Class clazz)
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
}

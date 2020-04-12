package de.ecconia.java.opentung.tungboard.tungobjects;

import de.ecconia.java.opentung.tungboard.netremoting.elements.Class;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Field;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.ClassField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.Int32Field;

public class TungPanelDisplay extends TungObject
{
	private TungAngles angle;
	private TungPosition position;
	
	private TungColorEnum color;
	
	public TungPanelDisplay(Class clazz)
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
			else if("Color".equals(name))
			{
				ClassField cField = (ClassField) field;
				Class valueClass = (Class) cField.getValue();
				
				Int32Field val = (Int32Field) valueClass.getFields()[0]; //Risk error...
				color = TungColorEnum.lookup(val.getValue());
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

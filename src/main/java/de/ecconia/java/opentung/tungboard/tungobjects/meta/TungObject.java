package de.ecconia.java.opentung.tungboard.tungobjects.meta;

import de.ecconia.java.opentung.tungboard.netremoting.elements.Class;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Field;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.ClassField;
import de.ecconia.java.opentung.tungboard.tungobjects.TungBlotter;
import de.ecconia.java.opentung.tungboard.tungobjects.TungBoard;
import de.ecconia.java.opentung.tungboard.tungobjects.TungButton;
import de.ecconia.java.opentung.tungboard.tungobjects.TungColorDisplay;
import de.ecconia.java.opentung.tungboard.tungobjects.TungDelayer;
import de.ecconia.java.opentung.tungboard.tungobjects.TungDisplay;
import de.ecconia.java.opentung.tungboard.tungobjects.TungInverter;
import de.ecconia.java.opentung.tungboard.tungobjects.TungLabel;
import de.ecconia.java.opentung.tungboard.tungobjects.TungMount;
import de.ecconia.java.opentung.tungboard.tungobjects.TungNoisemaker;
import de.ecconia.java.opentung.tungboard.tungobjects.TungPanelButton;
import de.ecconia.java.opentung.tungboard.tungobjects.TungPanelColorDisplay;
import de.ecconia.java.opentung.tungboard.tungobjects.TungPanelDisplay;
import de.ecconia.java.opentung.tungboard.tungobjects.TungPanelLabel;
import de.ecconia.java.opentung.tungboard.tungobjects.TungPanelSwitch;
import de.ecconia.java.opentung.tungboard.tungobjects.TungPeg;
import de.ecconia.java.opentung.tungboard.tungobjects.TungSnappingPeg;
import de.ecconia.java.opentung.tungboard.tungobjects.TungSwitch;
import de.ecconia.java.opentung.tungboard.tungobjects.TungThroughBlotter;
import de.ecconia.java.opentung.tungboard.tungobjects.TungThroughPeg;
import de.ecconia.java.opentung.tungboard.tungobjects.TungWire;
import de.ecconia.java.opentung.tungboard.tungobjects.common.Angles;
import de.ecconia.java.opentung.tungboard.tungobjects.common.Position;

public class TungObject implements Angles, Position
{
	private TungAngles angles;
	private TungPosition position;
	
	protected boolean checkField(Field field)
	{
		String name = field.getName();
		if("LocalEulerAngles".equals(name))
		{
			angles = new TungAngles(field);
			return true;
		}
		else if("LocalPosition".equals(name))
		{
			position = new TungPosition(field);
			return true;
		}
		return false;
	}
	
	@Override
	public TungAngles getAngles()
	{
		return angles;
	}
	
	@Override
	public TungPosition getPosition()
	{
		return position;
	}
	
	protected TungObject convertComponent(Field field)
	{
		//Assume its a class...
		Class childClass = (Class) ((ClassField) field).getValue();
		String className = childClass.getName();
		if("SavedObjects.SavedCircuitBoard".equals(className))
		{
			return new TungBoard(childClass);
		}
		else if("SavedObjects.SavedBlotter".equals(className))
		{
			return new TungBlotter(childClass);
		}
		else if("SavedObjects.SavedButton".equals(className))
		{
			return new TungButton(childClass);
		}
		else if("SavedObjects.SavedColorDisplay".equals(className))
		{
			return new TungColorDisplay(childClass);
		}
//		else if("SavedObjects.SavedCustomObject".equals(className))
//		{
//		}
		else if("SavedObjects.SavedDelayer".equals(className))
		{
			return new TungDelayer(childClass);
		}
		else if("SavedObjects.SavedDisplay".equals(className))
		{
			return new TungDisplay(childClass);
		}
		else if("SavedObjects.SavedInverter".equals(className))
		{
			return new TungInverter(childClass);
		}
		else if("SavedObjects.SavedLabel".equals(className))
		{
			return new TungLabel(childClass);
		}
		else if("SavedObjects.SavedMount".equals(className))
		{
			return new TungMount(childClass);
		}
		else if("SavedObjects.SavedNoisemaker".equals(className))
		{
			return new TungNoisemaker(childClass);
		}
		else if("SavedObjects.SavedPanelButton".equals(className))
		{
			return new TungPanelButton(childClass);
		}
		else if("SavedObjects.SavedPanelColorDisplay".equals(className))
		{
			return new TungPanelColorDisplay(childClass);
		}
		else if("SavedObjects.SavedPanelDisplay".equals(className))
		{
			return new TungPanelDisplay(childClass);
		}
		else if("SavedObjects.SavedPanelLabel".equals(className))
		{
			return new TungPanelLabel(childClass);
		}
		else if("SavedObjects.SavedPanelSwitch".equals(className))
		{
			return new TungPanelSwitch(childClass);
		}
		else if("SavedObjects.SavedPeg".equals(className))
		{
			return new TungPeg(childClass);
		}
		else if("SavedObjects.SavedSnappingPeg".equals(className))
		{
			return new TungSnappingPeg(childClass);
		}
		else if("SavedObjects.SavedSwitch".equals(className))
		{
			return new TungSwitch(childClass);
		}
		else if("SavedObjects.SavedThroughBlotter".equals(className))
		{
			return new TungThroughBlotter(childClass);
		}
		else if("SavedObjects.SavedThroughPeg".equals(className))
		{
			return new TungThroughPeg(childClass);
		}
		else if("SavedObjects.SavedWire".equals(className))
		{
			return new TungWire(childClass);
		}
		
		System.out.println("Unknown component found: " + className);
		return null;
	}
}

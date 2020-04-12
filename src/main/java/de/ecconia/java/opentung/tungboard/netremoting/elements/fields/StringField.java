package de.ecconia.java.opentung.tungboard.netremoting.elements.fields;

import de.ecconia.java.opentung.tungboard.netremoting.ParseBundle;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Field;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Text;

public class StringField extends Field
{
	private Text text;
	
	@Override
	public Field copy()
	{
		Field field = new StringField();
		field.setName(getName());
		return field;
	}
	
	@Override
	public void parseContent(ParseBundle b)
	{
		text = new Text(b, true);
		
//		System.out.println("Read string: " + text);
	}
}

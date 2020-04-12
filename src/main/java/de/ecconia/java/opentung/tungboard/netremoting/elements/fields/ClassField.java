package de.ecconia.java.opentung.tungboard.netremoting.elements.fields;

import de.ecconia.java.opentung.tungboard.netremoting.ParseBundle;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Field;
import de.ecconia.java.opentung.tungboard.netremoting.elements.FieldVarReference;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Library;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Object;
import de.ecconia.java.opentung.tungboard.netremoting.elements.ProperDefinedClass;
import de.ecconia.java.opentung.tungboard.netremoting.elements.ReferencedClass;

public class ClassField extends Field
{
	private final String className;
	private final Library library;
	
	private Object value;
	
	public ClassField(String className, Library library)
	{
		this.className = className;
		this.library = library;
	}
	
	@Override
	public Field copy()
	{
		Field field = new ClassField(className, library);
		field.setName(getName());
		return field;
	}
	
	@Override
	public void parseContent(ParseBundle b)
	{
		int recordTag = b.uByte();
		if(recordTag == 5)
		{
			value = new ProperDefinedClass(b);
		}
		else if(recordTag == 1)
		{
			value = new ReferencedClass(b);
		}
		else if(recordTag == 9)
		{
			value = new FieldVarReference(b, (Object object) -> {
				this.value = object;
			});
		}
		else if(recordTag == 10)
		{
			value = null;
		}
		else
		{
			throw new RuntimeException("Unknown object record ID: " + recordTag);
		}
	}
}

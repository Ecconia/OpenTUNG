package de.ecconia.java.opentung.tungboard.netremoting.elements.fields;

import de.ecconia.java.opentung.tungboard.netremoting.NRParseBundle;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRFieldVarReference;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRLibrary;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRObject;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRProperDefinedClass;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRReferencedClass;

public class NRClassField extends NRField
{
	private final String className;
	private final NRLibrary library;
	
	private NRObject value;
	
	public NRClassField(String className, NRLibrary library)
	{
		this.className = className;
		this.library = library;
	}
	
	@Override
	public NRField copy()
	{
		NRField field = new NRClassField(className, library);
		field.setName(getName());
		return field;
	}
	
	@Override
	public void parseContent(NRParseBundle b)
	{
		int recordTag = b.uByte();
		if(recordTag == 5)
		{
			value = new NRProperDefinedClass(b);
		}
		else if(recordTag == 1)
		{
			value = new NRReferencedClass(b);
		}
		else if(recordTag == 9)
		{
			value = new NRFieldVarReference(b, (NRObject object) -> {
				this.value = object;
			});
		}
		else if(recordTag == 10)
		{
			value = null;
//			System.out.println("Read Class: NULL");
		}
		else
		{
			throw new RuntimeException("Unknown object record ID: " + recordTag);
		}
	}
	
	public NRObject getValue()
	{
		return value;
	}
}

package de.ecconia.java.opentung.tungboard.netremoting.elements;

import de.ecconia.java.opentung.tungboard.netremoting.NRParseBundle;

public class NRReferencedClass extends NRClass
{
	public NRReferencedClass(NRParseBundle b)
	{
		b.readAndStoreID(this);
		
		int templateID = b.sInt();
		NRClass clazz = b.getClass(templateID);
		this.name = clazz.getName();
		this.library = clazz.getLibrary();
		this.fields = clazz.getFieldCopy();
		
//		System.out.println("ClassRef(" + templateID + "): ID: " + id + " Name: " + name + " Lib: " + library.getId() + " Fields... " + fields.length + "x");
		
		for(NRField field : fields)
		{
			field.parseContent(b);
		}
	}
}

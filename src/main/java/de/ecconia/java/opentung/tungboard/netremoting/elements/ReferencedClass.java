package de.ecconia.java.opentung.tungboard.netremoting.elements;

import de.ecconia.java.opentung.tungboard.netremoting.ParseBundle;

public class ReferencedClass extends Class
{
	public ReferencedClass(ParseBundle b)
	{
		b.readAndStoreID(this);
		
		int templateID = b.sInt();
		Class clazz = b.getClass(templateID);
		this.name = clazz.getName();
		this.library = clazz.getLibrary();
		this.fields = clazz.getFieldCopy();
		
//		System.out.println("Class: ID: " + id + " Name: " + name + " Lib: " + library.getId() + " Fields... " + fields.length + "x");
		
		for(Field field : fields)
		{
			field.parseContent(b);
		}
	}
}

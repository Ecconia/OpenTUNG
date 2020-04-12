package de.ecconia.java.opentung.tungboard.netremoting.elements;

import de.ecconia.java.opentung.tungboard.netremoting.ParseBundle;
import de.ecconia.java.opentung.tungboard.netremoting.aresolveable.ResolveField;

public class ProperDefinedClass extends Class
{
	public ProperDefinedClass(ParseBundle b)
	{
		b.readAndStoreID(this);
		
		this.name = b.string();
		this.fields = ResolveField.parseFileds(b);
		this.library = b.readLibraryAndResolve();
		
//		System.out.println("Class: ID: " + id + " Name: " + name + " Lib: " + library.getId() + " Fields... " + fields.length + "x");
		
		for(Field field : fields)
		{
			field.parseContent(b);
		}
	}
}

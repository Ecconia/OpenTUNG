package de.ecconia.java.opentung.tungboard.netremoting;

import de.ecconia.java.opentung.tungboard.netremoting.elements.Class;
import de.ecconia.java.opentung.tungboard.netremoting.elements.FieldVarReference;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Library;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Object;

import java.util.ArrayList;
import java.util.List;

public class ParseBundle
{
	public final ByteReader reader;
	public final ParsedFile file;
	
	public ParseBundle(ByteReader reader, ParsedFile pf)
	{
		this.reader = reader;
		this.file = pf;
	}
	
	public void readAndStoreID(Object object)
	{
		object.setId(reader.readIntLE());
		file.registerObject(object);
	}
	
	public int uByte()
	{
		return reader.readUnsignedByte();
	}
	
	public int sInt()
	{
		return reader.readIntLE();
	}
	
	public String string()
	{
		return reader.readBytePrefixedString();
	}
	
	public Library readLibraryAndResolve()
	{
		int id = reader.readIntLE();
		return file.getLibrary(id);
	}
	
	//Reference stuff:
	private final List<FieldVarReference> references = new ArrayList<>();
	
	public void addResolver(FieldVarReference reference)
	{
		references.add(reference);
	}
	
	public void resolve()
	{
		for(FieldVarReference ref : references)
		{
			ref.resolve(file);
		}
	}
	
	public Class getClass(int templateID)
	{
		return file.getClazz(templateID);
	}
}

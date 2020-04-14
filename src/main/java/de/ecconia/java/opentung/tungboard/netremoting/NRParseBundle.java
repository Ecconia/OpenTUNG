package de.ecconia.java.opentung.tungboard.netremoting;

import de.ecconia.java.opentung.tungboard.netremoting.elements.NRClass;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRFieldVarReference;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRLibrary;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRObject;

import java.util.ArrayList;
import java.util.List;

public class NRParseBundle
{
	public final ByteReader reader;
	public final NRFile file;
	
	public NRParseBundle(ByteReader reader, NRFile pf)
	{
		this.reader = reader;
		this.file = pf;
	}
	
	public void readAndStoreID(NRObject object)
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
	
	public NRLibrary readLibraryAndResolve()
	{
		int id = reader.readIntLE();
		return file.getLibrary(id);
	}
	
	//Reference stuff:
	private final List<NRFieldVarReference> references = new ArrayList<>();
	
	public void addResolver(NRFieldVarReference reference)
	{
		references.add(reference);
	}
	
	public void resolve()
	{
		for(NRFieldVarReference ref : references)
		{
			ref.resolve(file);
		}
	}
	
	public NRClass getClass(int templateID)
	{
		return file.getClazz(templateID);
	}
}

package de.ecconia.java.opentung.tungboard.netremoting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.ecconia.java.opentung.tungboard.netremoting.elements.NRClass;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRHeader;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRLibrary;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRObject;

public class NRFile
{
	private NRHeader header;
	private final Map<Integer, NRLibrary> libraries = new HashMap<>();
	private final Map<Integer, NRObject> objects = new HashMap<>();
	private final List<NRObject> rootObjects = new ArrayList<>();
	
	public void setHeader(NRHeader header)
	{
		if(this.header != null)
		{
			throw new RuntimeException("File has more than one Header");
		}
		this.header = header;
	}
	
	public NRHeader getHeader()
	{
		return header;
	}
	
	public void registerLibrary(NRLibrary library)
	{
		if(libraries.put(library.getId(), library) != null)
		{
			throw new RuntimeException("File did declare two libraries with the same ID: " + library.getId());
		}
	}
	
	public NRLibrary getLibrary(int id)
	{
		NRLibrary lib = libraries.get(id);
		if(lib == null)
		{
			throw new RuntimeException("Library with ID " + id + " does not exist.");
		}
		return lib;
	}
	
	public void registerObject(NRObject object)
	{
		if(objects.put(object.getId(), object) != null)
		{
			throw new RuntimeException("File did declare two objects with the same ID: " + object.getId());
		}
	}
	
	public NRClass getClazz(int objectID)
	{
		NRObject object = objects.get(objectID);
		if(object == null)
		{
			throw new RuntimeException("Class with ID " + objectID + " was referenced, but doesn't exist.");
		}
		else if(!(object instanceof NRClass))
		{
			throw new RuntimeException("ObjectThing with ID " + objectID + " was referenced as class, but is " + object.getClass().getSimpleName() + ".");
		}
		else
		{
			return (NRClass) object;
		}
	}
	
	public void addRoot(NRObject thing)
	{
		rootObjects.add(thing);
	}
	
	public List<NRObject> getRootElements()
	{
		return rootObjects;
	}
	
	public NRObject getObject(int refId)
	{
		NRObject obj = objects.get(refId);
		if(obj == null)
		{
			throw new RuntimeException("Requested Object with ID " + refId + " does not exist.");
		}
		return obj;
	}
}

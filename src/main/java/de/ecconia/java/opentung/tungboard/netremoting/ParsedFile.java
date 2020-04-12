package de.ecconia.java.opentung.tungboard.netremoting;

import de.ecconia.java.opentung.tungboard.netremoting.elements.Class;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Library;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Object;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Header;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParsedFile
{
	private Header header;
	private final Map<Integer, Library> libraries = new HashMap<>();
	private final Map<Integer, Object> objects = new HashMap<>();
	private final List<Object> rootObjects = new ArrayList<>();
	
	public void setHeader(Header header)
	{
		if(this.header != null)
		{
			throw new RuntimeException("File has more than one Header");
		}
		this.header = header;
	}
	
	public Header getHeader()
	{
		return header;
	}
	
	public void registerLibrary(Library library)
	{
		if(libraries.put(library.getId(), library) != null)
		{
			throw new RuntimeException("File did declare two libraries with the same ID: " + library.getId());
		}
	}
	
	public Library getLibrary(int id)
	{
		Library lib = libraries.get(id);
		if(lib == null)
		{
			throw new RuntimeException("Library with ID " + id + " does not exist.");
		}
		return lib;
	}
	
	public void registerObject(Object object)
	{
		if(objects.put(object.getId(), object) != null)
		{
			throw new RuntimeException("File did declare two objects with the same ID: " + object.getId());
		}
	}
	
	public Class getClazz(int objectID)
	{
		Object object = objects.get(objectID);
		if(object == null)
		{
			throw new RuntimeException("Class with ID " + objectID + " was referenced, but doesn't exist.");
		}
		else if(!(object instanceof Class))
		{
			throw new RuntimeException("ObjectThing with ID " + objectID + " was referenced as class, but is " + object.getClass().getSimpleName() + ".");
		}
		else
		{
			return (Class) object;
		}
	}
	
	public void addRoot(Object thing)
	{
		rootObjects.add(thing);
	}
	
	public List<Object> getRootElements()
	{
		return rootObjects;
	}
	
	public Object getObject(int refId)
	{
		Object obj = objects.get(refId);
		if(obj == null)
		{
			throw new RuntimeException("Requested Object with ID " + refId + " does not exist.");
		}
		return obj;
	}
}

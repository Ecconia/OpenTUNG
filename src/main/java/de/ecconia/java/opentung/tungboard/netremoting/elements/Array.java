package de.ecconia.java.opentung.tungboard.netremoting.elements;

import de.ecconia.java.opentung.tungboard.netremoting.ParseBundle;
import de.ecconia.java.opentung.tungboard.netremoting.FieldResolver;

public class Array extends Object
{
	private final Field[] entries;
	private final Field proto;
	
	public Array(ParseBundle b)
	{
		b.readAndStoreID(this);
		
		int arrayType = b.uByte();
		if(arrayType != 0) //A single-dimensional Array.
		{
			throw new RuntimeException("ArrayRecord type is not 0, cannot be handled yet. Its: " + arrayType);
		}
		
		int rank = b.sInt();
		if(rank != 1)
		{
			throw new RuntimeException("Non-one-dimensional arrays cannot be handled yet.");
		}
		
		int length = b.sInt();
		//Skip LowerBounds, cause type 0.
		
		proto = FieldResolver.parseSimpleField(b);
		
//		System.out.println("Array: ID: " + id + " type: " + arrayType + " rank: " + rank + " length: " + length + " etype: " + proto.getClass().getSimpleName());
		
		entries = new Field[length];
		for(int i = 0; i < length; i++)
		{
			entries[i] = proto.copy();
			entries[i].parseContent(b);
		}
	}
	
	public Field getProto()
	{
		return proto;
	}
	
	public Field[] getEntries()
	{
		return entries;
	}
}

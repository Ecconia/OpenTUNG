package de.ecconia.java.opentung.tungboard.netremoting.elements;

import de.ecconia.java.opentung.tungboard.netremoting.NRFieldResolver;
import de.ecconia.java.opentung.tungboard.netremoting.NRParseBundle;

public class NRArrayPrimitive extends NRObject
{
	private final NRField[] field;
	
	public NRArrayPrimitive(NRParseBundle b)
	{
		b.readAndStoreID(this);
		
		int amount = b.sInt();
		field = new NRField[amount];
		
		for(int i = 0; i < amount; i++)
		{
			field[i] = NRFieldResolver.parsePrimitiveEntry(b);
			field[i].parseContent(b);
		}
	}
}

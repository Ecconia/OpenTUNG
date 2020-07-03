package de.ecconia.java.opentung.tungboard.netremoting.elements;

import de.ecconia.java.opentung.tungboard.netremoting.NRFieldResolver;
import de.ecconia.java.opentung.tungboard.netremoting.NRParseBundle;

public class NRArrayObject extends NRObject
{
	private final NRField[] entries;
	
	public NRArrayObject(NRParseBundle b)
	{
		b.readAndStoreID(this);
		
		int amount = b.sInt();
		entries = new NRField[amount];
		
		for(int i = 0; i < amount; i++)
		{
			entries[i] = NRFieldResolver.parseSimpleField(b);
			entries[i].parseContent(b);
		}
	}
}

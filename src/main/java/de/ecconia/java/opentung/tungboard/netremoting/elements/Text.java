package de.ecconia.java.opentung.tungboard.netremoting.elements;

import de.ecconia.java.opentung.tungboard.netremoting.ParseBundle;

public class Text extends Object
{
	private String text;
	
	//Unused (Might happen when string templating):
	public Text(ParseBundle b)
	{
		this(b, false);
	}
	
	public Text(ParseBundle b, boolean checkTag)
	{
		if(checkTag)
		{
			int tag = b.uByte();
			if(tag != 6)
			{
				throw new RuntimeException("Wrong Text record ID: " + tag);
			}
		}
		
		b.readAndStoreID(this);
		
		text = b.string();
		
//		System.out.println("String: ID: " + id + " Text: \"" + text + "\"");
	}
	
	public String getContent()
	{
		return text;
	}
}

package de.ecconia.java.opentung.tungboard.netremoting;

import de.ecconia.java.opentung.tungboard.netremoting.elements.Array;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Header;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Library;
import de.ecconia.java.opentung.tungboard.netremoting.elements.ProperDefinedClass;
import de.ecconia.java.opentung.tungboard.netremoting.elements.ReferencedClass;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class NRParser
{
	public static ParsedFile parse(File file)
	{
		try
		{
			byte[] data = Files.readAllBytes(file.toPath());
			ByteReader reader = new ByteReader(data);
			return parse(reader);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
		return null;
	}
	
	private static ParsedFile parse(ByteReader reader)
	{
		ParsedFile pf = new ParsedFile();
		ParseBundle b = new ParseBundle(reader, pf);
		
		loop:
		while(true)
		{
			int tag = reader.readUnsignedByte();
			switch(tag)
			{
				case 0:
				{
					pf.setHeader(new Header(b));
					break;
				}
				case 1:
				{
					pf.addRoot(new ReferencedClass(b));
					break;
				}
				case 5:
				{
					pf.addRoot(new ProperDefinedClass(b));
					break;
				}
				case 7:
				{
					pf.addRoot(new Array(b));
					break;
				}
				case 11:
					break loop;
				case 12:
				{
					pf.registerLibrary(new Library(b));
					break;
				}
				default:
				{
					throw new RuntimeException("Read unknown root Record: " + tag);
				}
			}
		}
		
		if(reader.hasMore())
		{
			throw new RuntimeException("Reader had " + reader.getRemaining() + " bytes to read.");
		}
		
		b.resolve();
		
		return pf;
	}
}

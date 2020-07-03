package de.ecconia.java.opentung.tungboard.netremoting;

import de.ecconia.java.opentung.tungboard.netremoting.elements.NRArrayPrimitive;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRArray;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRHeader;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRLibrary;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRArrayObject;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRProperDefinedClass;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRReferencedClass;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class NRParser
{
	public static NRFile parse(File file)
	{
		try
		{
			byte[] data = Files.readAllBytes(file.toPath());
			ByteReader reader = new ByteReader(data);
			return parse(reader);
		}
		catch(IOException e)
		{
			throw new RuntimeException("Could not read/load tungboard file.", e);
		}
	}
	
	private static NRFile parse(ByteReader reader)
	{
		NRFile pf = new NRFile();
		NRParseBundle b = new NRParseBundle(reader, pf);
		
		loop:
		while(true)
		{
			int tag = reader.readUnsignedByte();
//			System.out.print("\n-> ");
			switch(tag)
			{
				case 0:
				{
					pf.setHeader(new NRHeader(b));
					break;
				}
				case 1:
				{
					pf.addRoot(new NRReferencedClass(b));
					break;
				}
				case 5:
				{
					pf.addRoot(new NRProperDefinedClass(b));
					break;
				}
				case 7:
				{
					pf.addRoot(new NRArray(b));
					break;
				}
				case 11:
//					System.out.println("End.");
					break loop;
				case 12:
				{
					pf.registerLibrary(new NRLibrary(b));
					break;
				}
				case 15:
				{
					pf.addRoot(new NRArrayPrimitive(b));
					break;
				}
				case 16:
				{
					pf.addRoot(new NRArrayObject(b));
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

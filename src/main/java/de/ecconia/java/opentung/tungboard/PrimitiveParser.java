package de.ecconia.java.opentung.tungboard;

import de.ecconia.java.opentung.tungboard.netremoting.NRParser;
import de.ecconia.java.opentung.tungboard.netremoting.ParsedFile;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Class;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Object;
import de.ecconia.java.opentung.tungboard.tungobjects.TungBoard;

import java.io.File;

public class PrimitiveParser
{
	public static void main(String[] args)
	{
		new PrimitiveParser();
	}
	
	public PrimitiveParser()
	{
		ParsedFile pf = NRParser.parse(new File("boards/ASDF.tungboard"));
//		ParsedFile pf = NRParser.parse(new File("boards/16Bit-Paralell-CLA-ALU.tungboard"));
		
		Object object = pf.getRootElements().get(0);
		Class firstClass;
		if(object instanceof Class)
		{
			firstClass = (Class) object;
		}
		else
		{
			throw new RuntimeException("Unknown first object: " + object.getClass().getSimpleName());
		}
		
		if(TungBoard.NAME.equals(firstClass.getName()))
		{
			TungBoard board = new TungBoard(firstClass);
			
			new Exporter(new File("boards/output.tungboard"), board);
		}
		else
		{
			throw new RuntimeException("First Class has wrong type: " + firstClass.getName());
		}
	}
}

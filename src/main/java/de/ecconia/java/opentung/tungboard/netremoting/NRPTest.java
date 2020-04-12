package de.ecconia.java.opentung.tungboard.netremoting;

import java.io.File;

public class NRPTest
{
	public static void main(String[] args)
	{
		ParsedFile pf = NRParser.parse(new File("16Bit-Paralell-CLA-ALU.tungboard"));
		
//		System.out.println("Root element:");
//		for(ObjectThing obj : pf.getRootElements())
//		{
//			obj.debugPrint("");
//		}
	}
}

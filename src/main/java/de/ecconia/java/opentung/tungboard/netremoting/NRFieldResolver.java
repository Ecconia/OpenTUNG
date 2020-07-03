package de.ecconia.java.opentung.tungboard.netremoting;

import de.ecconia.java.opentung.tungboard.netremoting.elements.NRField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRLibrary;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.NRBooleanField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.NRClassField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.NRFloatField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.NRInt32Field;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.NRObjectReferenceField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.NRStringField;

public class NRFieldResolver
{
	public static NRField[] parseFields(NRParseBundle b)
	{
		int amount = b.sInt();
		String[] names = new String[amount];
		for(int i = 0; i < amount; i++)
		{
			names[i] = b.string();
		}
		
		int[] roughTypes = new int[amount];
		for(int i = 0; i < amount; i++)
		{
			roughTypes[i] = b.uByte();
		}
		
		NRField[] fields = new NRField[amount];
		for(int i = 0; i < amount; i++)
		{
			int roughType = roughTypes[i];
			if(roughType == 0)
			{
				fields[i] = parsePrimitiveEntry(b);
			}
			else if(roughType == 1)
			{
				//StringType:
				fields[i] = new NRStringField();
			}
			else if(roughType == 4)
			{
				//ClassType:
				String className = b.string();
				NRLibrary library = b.readLibraryAndResolve();
				fields[i] = new NRClassField(className, library);
			}
			else if(roughType == 5)
			{
				fields[i] = new NRObjectReferenceField(true);
			}
			else
			{
				throw new RuntimeException("Found unknown field type: " + roughType);
			}
			
			fields[i].setName(names[i]);
		}
		
		return fields;
	}
	
	public static NRField parseSimpleField(NRParseBundle b)
	{
		int roughType = b.uByte();
		if(roughType == 0)
		{
			return parsePrimitiveEntry(b);
		}
		else if(roughType == 1)
		{
			//StringType:
			return new NRStringField();
		}
		else if(roughType == 4)
		{
			//ClassType:
			String className = b.string();
			NRLibrary library = b.readLibraryAndResolve();
			return new NRClassField(className, library);
		}
		else if(roughType == 6)
		{
			//TBI: Only occurrence so far as object-array entry.
			return new NRStringField(false);
		}
		else if(roughType == 9)
		{
			//TBI: Only occurrence so far as object-array entry.
			return new NRObjectReferenceField(false);
		}
		else
		{
			throw new RuntimeException("Found unknown field type: " + roughType);
		}
	}
	
	public static NRField parsePrimitiveEntry(NRParseBundle b)
	{
		//PrimitiveType:
		int fineType = b.uByte();
		if(fineType == 1)
		{
			//Boolean:
			return new NRBooleanField();
		}
		else if(fineType == 8)
		{
			//Int32:
			return new NRInt32Field();
		}
		else if(fineType == 11)
		{
			//Single/Float
			return new NRFloatField();
		}
		else
		{
			throw new RuntimeException("Unknown primitive sub type ID: " + fineType);
		}
	}
}

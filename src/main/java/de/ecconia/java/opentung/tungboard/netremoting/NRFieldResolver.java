package de.ecconia.java.opentung.tungboard.netremoting;

import de.ecconia.java.opentung.tungboard.netremoting.elements.NRField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRLibrary;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.NRBooleanField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.NRClassField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.NRFloatField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.NRInt32Field;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.NRStringField;

public class NRFieldResolver
{
	public static NRField[] parseFileds(NRParseBundle b)
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
				//PrimitiveType:
				int fineType = b.uByte();
				if(fineType == 1)
				{
					//Boolean:
					fields[i] = new NRBooleanField();
				}
				else if(fineType == 8)
				{
					//Int32:
					fields[i] = new NRInt32Field();
				}
				else if(fineType == 11)
				{
					//Single/Float
					fields[i] = new NRFloatField();
				}
				else
				{
					throw new RuntimeException("Unknown primitive sub type ID: " + fineType);
				}
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
		else
		{
			throw new RuntimeException("Found unknown field type: " + roughType);
		}
	}
}

package de.ecconia.java.opentung.tungboard.netremoting.aresolveable;

import de.ecconia.java.opentung.tungboard.netremoting.ParseBundle;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Field;
import de.ecconia.java.opentung.tungboard.netremoting.elements.Library;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.BooleanField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.ClassField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.FloatField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.Int32Field;
import de.ecconia.java.opentung.tungboard.netremoting.elements.fields.StringField;

public class ResolveField
{
	public static Field[] parseFileds(ParseBundle b)
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
		
		Field[] fields = new Field[amount];
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
					fields[i] = new BooleanField();
				}
				else if(fineType == 8)
				{
					//Int32:
					fields[i] = new Int32Field();
				}
				else if(fineType == 11)
				{
					//Single/Float
					fields[i] = new FloatField();
				}
				else
				{
					throw new RuntimeException("Unknown primitive sub type ID: " + fineType);
				}
			}
			else if(roughType == 1)
			{
				//StringType:
				fields[i] = new StringField();
			}
			else if(roughType == 4)
			{
				//ClassType:
				String className = b.string();
				Library library = b.readLibraryAndResolve();
				fields[i] = new ClassField(className, library);
			}
			else
			{
				throw new RuntimeException("Found unknown field type: " + roughType);
			}
			
			fields[i].setName(names[i]);
		}
		
		return fields;
	}
	
	public static Field parseSimpleField(ParseBundle b)
	{
		int roughType = b.uByte();
		if(roughType == 0)
		{
			//PrimitiveType:
			int fineType = b.uByte();
			if(fineType == 1)
			{
				//Boolean:
				return new BooleanField();
			}
			else if(fineType == 8)
			{
				//Int32:
				return new Int32Field();
			}
			else if(fineType == 11)
			{
				//Single/Float
				return new FloatField();
			}
			else
			{
				throw new RuntimeException("Unknown primitive sub type ID: " + fineType);
			}
		}
		else if(roughType == 1)
		{
			//StringType:
			return new StringField();
		}
		else if(roughType == 4)
		{
			//ClassType:
			String className = b.string();
			Library library = b.readLibraryAndResolve();
			return new ClassField(className, library);
		}
		else
		{
			throw new RuntimeException("Found unknown field type: " + roughType);
		}
	}
}

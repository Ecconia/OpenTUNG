package de.ecconia.java.opentung.util.io;

public class ByteReader
{
	private final byte[] data;
	private int pointer;
	
	public ByteReader(byte[] data)
	{
		this.data = data;
	}
	
	//Big Endian
	public String readCharsBE(int amount)
	{
		char[] chars = new char[amount];
		
		for(int i = 0; i < amount; i++)
		{
			chars[i] = (char) data[pointer++];
		}
		
		return new String(chars);
	}
	
	public String readCharsLE(int amount)
	{
		char[] chars = new char[amount];
		
		for(int i = amount - 1; i >= 0; i--)
		{
			chars[i] = (char) data[pointer++];
		}
		
		return new String(chars);
	}
	
	//Little Endian
	public int readIntLE()
	{
		return readUnsignedByte() | readUnsignedByte() << 8 | readUnsignedByte() << 16 | readUnsignedByte() << 24;
	}
	
	public int readIntBE()
	{
		return readUnsignedByte() << 24 | readUnsignedByte() << 16 | readUnsignedByte() << 8 | readUnsignedByte();
	}
	
	public int readShortBE()
	{
		return readUnsignedByte() << 8 | readUnsignedByte();
	}
	
	public int readShortLE()
	{
		return readUnsignedByte() | readUnsignedByte() << 8;
	}
	
	public int readUnsignedByte()
	{
		return ((int) data[pointer++]) & 255;
	}
	
	public int readIntVariable()
	{
		int tmp = 0;
		int val = readUnsignedByte();
		int amountRead = 1;
		
		while(val >= 128)
		{
			tmp = tmp << 7;
			tmp |= (val & 127);
			val = readUnsignedByte();
			amountRead++;
		}
		
		//0x FF FF FF FF
		//0b 0000000 0|000000 00|00000 000|0000 0000
		
		if(amountRead > 5)
		{
			throw new RuntimeException("Read too much for variable int: " + amountRead);
		}
		
		return (tmp << 7) | val;
	}
	
	public void skip(int size)
	{
		pointer += size;
	}
	
	public byte[] readBytes(int size)
	{
		byte[] bytes = new byte[size];
		System.arraycopy(data, pointer, bytes, 0, size);
		pointer += size;
		return bytes;
	}
	
	public boolean hasMore()
	{
		return pointer < data.length;
	}
	
	public int getRemaining()
	{
		return data.length - pointer;
	}
	
	public int getPointer(int offset)
	{
		return pointer - offset;
	}
	
	public String readPrepaddedString(boolean termination)
	{
		int length = readUnsignedByte();
		String str = new String(readBytes(length));
		if(termination)
		{
			int term = readUnsignedByte();
			if(term != 0)
			{
				throw new RuntimeException("Non 0 termination: " + term);
			}
		}
		return str;
	}
	
	private int variableInteger()
	{
		int shiftAmount = 0;
		int value = 0;
		int validation = 0;
		
		int read;
		do
		{
			read = readUnsignedByte();
			value |= ((read & 0b01111111) << shiftAmount);
			shiftAmount += 7;
			if(++validation == 5)
			{
				if(read >> 3 != 0)
				{
					throw new RuntimeException("Read fifth byte of a variable integer, but the upper 5 bits had not been zero: " + Integer.toBinaryString(read));
				}
			}
		}
		while((read & 0x80) != 0);
		
		return value;
	}
	
	public String readBytePrefixedString()
	{
		int byteAmount = variableInteger();
		return new String(readBytes(byteAmount));
	}
	
	public float readFloatLE()
	{
		int value = readIntLE();
		return Float.intBitsToFloat(value);
	}
}

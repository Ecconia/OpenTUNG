package de.ecconia.java.opentung.util.io;

import java.nio.charset.StandardCharsets;

import de.ecconia.java.opentung.savefile.CompactText;

public class ByteReader
{
	private final byte[] data;
	private int pointer;
	
	public ByteReader(byte[] data)
	{
		this.data = data;
	}
	
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
	
	public int readVariableInt()
	{
		int tmp = 0;
		int val = readUnsignedByte();
		int amountRead = 1;
		int shifts = 0;
		
		while(val >= 128)
		{
			tmp |= ((val & 0x7F) << shifts);
			shifts += 7;
			val = readUnsignedByte();
			amountRead++;
		}
		
		if(amountRead > 5)
		{
			throw new RuntimeException("Read too much for variable int: " + amountRead);
		}
		
		return tmp | (val << shifts);
	}
	
	public static void main(String... arguments)
	{
	
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
	
	public String readLengthPrefixedString()
	{
		int byteAmount = variableInteger();
		return new String(readBytes(byteAmount));
	}
	
	public String readIntLEPrefixedString()
	{
		int byteAmount = readIntLE();
		return new String(readBytes(byteAmount), StandardCharsets.UTF_8);
	}
	
	public float readFloatLE()
	{
		int value = readIntLE();
		return Float.intBitsToFloat(value);
	}
	
	public float readFloatBE()
	{
		int value = readIntBE();
		return Float.intBitsToFloat(value);
	}
	
	public String readCompactString()
	{
		int byteAmount = readVariableInt();
		return CompactText.decode(readBytes(byteAmount));
	}
	
	public boolean readBoolean()
	{
		return readUnsignedByte() != 0;
	}
	
	public double readDouble()
	{
		return Double.longBitsToDouble(readLongLE());
	}
	
	public long readLongBE()
	{
		return (long) readUnsignedByte() << 56
				| (long) readUnsignedByte() << 48
				| (long) readUnsignedByte() << 40
				| (long) readUnsignedByte() << 32
				| (long) readUnsignedByte() << 24
				| (long) readUnsignedByte() << 16
				| (long) readUnsignedByte() << 8
				| (long) readUnsignedByte();
	}
	
	public long readLongLE()
	{
		return (long) readUnsignedByte()
				| (long) readUnsignedByte() << 8
				| (long) readUnsignedByte() << 16
				| (long) readUnsignedByte() << 24
				| (long) readUnsignedByte() << 32
				| (long) readUnsignedByte() << 40
				| (long) readUnsignedByte() << 48
				| (long) readUnsignedByte() << 56;
	}
}

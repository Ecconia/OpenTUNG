package de.ecconia.java.opentung.util.io;

import de.ecconia.java.opentung.savefile.CompactText;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class ByteWriter
{
	private final OutputStream fos;
	
	public ByteWriter(Path file)
	{
		try
		{
			fos = new FileOutputStream(file.toFile(), false);
		}
		catch(FileNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public ByteWriter(OutputStream stream)
	{
		fos = stream;
	}
	
	public void writeVariableInt(int value)
	{
		while(true)
		{
			int export = value & 0x7F;
			value >>>= 7;
			if(value != 0)
			{
				writeByte(export | 0x80);
			}
			else
			{
				writeByte(export);
				break;
			}
		}
	}
	
	public void writeBoolean(boolean value)
	{
		if(value)
		{
			writeByte(1);
		}
		else
		{
			writeByte(0);
		}
	}
	
	public void writeCompactString(String text)
	{
		byte[] bytes = CompactText.encode(text);
		writeVariableInt(bytes.length);
		writeBytes(bytes);
	}
	
	public void writeString(String value)
	{
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		writeVariableInt(bytes.length);
		writeBytes(bytes);
	}
	
	public void writeBytes(byte[] bytes)
	{
		try
		{
			fos.write(bytes);
		}
		catch(IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private void writeLong(long value)
	{
		writeByte((int) (value & 0xFF));
		value >>>= 8;
		writeByte((int) (value & 0xFF));
		value >>>= 8;
		writeByte((int) (value & 0xFF));
		value >>>= 8;
		writeByte((int) (value & 0xFF));
		value >>>= 8;
		writeByte((int) (value & 0xFF));
		value >>>= 8;
		writeByte((int) (value & 0xFF));
		value >>>= 8;
		writeByte((int) (value & 0xFF));
		value >>>= 8;
		writeByte((int) (value & 0xFF));
	}
	
	public void writeInt(int value)
	{
		writeByte(value & 0xFF);
		value >>>= 8;
		writeByte(value & 0xFF);
		value >>>= 8;
		writeByte(value & 0xFF);
		value >>>= 8;
		writeByte(value & 0xFF);
	}
	
	public void writeByte(int value)
	{
		try
		{
			fos.write(value);
		}
		catch(IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public void writeFloat(float value)
	{
		writeInt(Float.floatToRawIntBits(value));
	}
	
	public void writeDouble(double value)
	{
		writeLong(Double.doubleToRawLongBits(value));
	}
	
	public void close()
	{
		try
		{
			fos.flush();
			fos.close();
		}
		catch(IOException e)
		{
			throw new RuntimeException(e);
		}
	}
}

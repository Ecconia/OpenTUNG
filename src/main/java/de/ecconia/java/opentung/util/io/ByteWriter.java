package de.ecconia.java.opentung.util.io;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import de.ecconia.java.opentung.savefile.CompactText;

public class ByteWriter
{
	private final OutputStream fos;
	
	private final byte[] buffer = new byte[1024];
	private int bufferIndex = 0;
	
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
	
	public void writeIntLEPrefixedString(String value)
	{
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		writeInt(bytes.length);
		writeBytes(bytes);
	}
	
	public void writeBytes(byte[] bytes)
	{
		if(bytes.length > buffer.length) //Array does not fit into buffer.
		{
			try
			{
				fos.write(buffer, 0, bufferIndex); //Flush the temp buffer first, regardless current size.
				fos.write(bytes, 0, bytes.length);
			}
			catch(IOException e)
			{
				throw new RuntimeException(e);
			}
			bufferIndex = 0;
		}
		else
		{
			int endIndex = bufferIndex + bytes.length; //Where the new index would be.
			if(endIndex >= buffer.length) //New index does not fit into buffer.
			{
				try
				{
					fos.write(buffer, 0, bufferIndex); //Flush buffer
					System.arraycopy(bytes, 0, buffer, 0, bytes.length); //And overwrite buffer with the array.
					bufferIndex = bytes.length;
				}
				catch(IOException e)
				{
					throw new RuntimeException(e);
				}
			}
			else
			{
				System.arraycopy(bytes, 0, buffer, bufferIndex, bytes.length);
				bufferIndex = endIndex; //Reuse addition result from earlier.
			}
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
	
	public void writeShort(int value)
	{
		writeByte(value & 0xFF);
		value >>>= 8;
		writeByte(value & 0xFF);
	}
	
	public void writeByte(int value)
	{
		if(bufferIndex >= buffer.length) //Should never ever be above 1024.
		{
			try
			{
				fos.write(buffer, 0, buffer.length);
			}
			catch(IOException e)
			{
				throw new RuntimeException(e);
			}
			bufferIndex = 0;
		}
		buffer[bufferIndex++] = (byte) value;
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
			if(bufferIndex != 0)
			{
				fos.write(buffer, 0, bufferIndex);
			}
			fos.flush();
			fos.close();
		}
		catch(IOException e)
		{
			throw new RuntimeException(e);
		}
	}
}

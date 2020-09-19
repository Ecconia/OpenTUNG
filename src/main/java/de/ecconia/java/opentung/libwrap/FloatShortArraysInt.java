package de.ecconia.java.opentung.libwrap;

public class FloatShortArraysInt
{
	private final float[] floats;
	private final short[] shorts;
	private final int integer;
	
	public FloatShortArraysInt(float[] floats, short[] shorts, int integer)
	{
		this.floats = floats;
		this.shorts = shorts;
		this.integer = integer;
	}
	
	public float[] getFloats()
	{
		return floats;
	}
	
	public short[] getShorts()
	{
		return shorts;
	}
	
	public int getInteger()
	{
		return integer;
	}
}

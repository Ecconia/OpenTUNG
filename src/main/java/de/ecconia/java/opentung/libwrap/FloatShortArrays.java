package de.ecconia.java.opentung.libwrap;

public class FloatShortArrays
{
	private final float[] floats;
	private final short[] shorts;
	
	public FloatShortArrays(float[] floats, short[] shorts)
	{
		this.floats = floats;
		this.shorts = shorts;
	}
	
	public float[] getFloats()
	{
		return floats;
	}
	
	public short[] getShorts()
	{
		return shorts;
	}
}

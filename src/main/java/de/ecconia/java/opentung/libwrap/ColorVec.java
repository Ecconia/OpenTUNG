package de.ecconia.java.opentung.libwrap;

public class ColorVec
{
	public static final ColorVec boardColor = new ColorVec(195f / 255f, 195f / 255f, 195f / 255f);
	
	private final float r;
	private final float g;
	private final float b;
	
	public ColorVec(float r, float g, float b)
	{
		this.r = r;
		this.g = g;
		this.b = b;
	}
	
	public float getR()
	{
		return r;
	}
	
	public float getG()
	{
		return g;
	}
	
	public float getB()
	{
		return b;
	}
}

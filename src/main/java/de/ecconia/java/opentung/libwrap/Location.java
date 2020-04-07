package de.ecconia.java.opentung.libwrap;

public class Location
{
	private float x, y, z;
	private float rotation;
	private float neck;
	
	public Location(float x, float y, float z, float rotation, float neck)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.rotation = rotation;
		this.neck = neck;
	}
	
	public float getNeck()
	{
		return neck;
	}
	
	public float getRotation()
	{
		return rotation;
	}
	
	public float getX()
	{
		return x;
	}
	
	public float getY()
	{
		return y;
	}
	
	public float getZ()
	{
		return z;
	}
	
	public void print()
	{
		System.out.println("Pos{ X: " + x + " Y: " + y + " Z: " + z + " R: " + rotation + " N: " + neck);
	}
}

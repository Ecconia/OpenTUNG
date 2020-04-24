package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.math.Vector3;

public class CompBoard extends CompGeneric
{
	private Vector3 color = new Vector3(195f / 255f, 195f / 255f, 195f / 255f);
	private int x, z;
	
	public CompBoard(int x, int z)
	{
		this.x = x;
		this.z = z;
	}
	
	public void setColor(Vector3 color)
	{
		this.color = color;
	}
	
	public Vector3 getColor()
	{
		return color;
	}
	
	public int getX()
	{
		return x;
	}
	
	public int getZ()
	{
		return z;
	}
}

package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.math.Quaternion;
import de.ecconia.java.opentung.math.Vector3;

public class Board
{
	private Vector3 color = new Vector3(195f / 255f, 195f / 255f, 195f / 255f);
	private Quaternion rotation;
	private Vector3 position;
	private int x, z;
	
	public Board(int x, int z)
	{
		this.x = x;
		this.z = z;
	}
	
	public void setColor(Vector3 color)
	{
		this.color = color;
	}
	
	public void setPosition(Vector3 position)
	{
		this.position = position;
	}
	
	public void setRotation(Quaternion rotation)
	{
		this.rotation = rotation;
	}
	
	public Vector3 getPosition()
	{
		return position;
	}
	
	public Vector3 getColor()
	{
		return color;
	}
	
	public Quaternion getRotation()
	{
		return rotation;
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

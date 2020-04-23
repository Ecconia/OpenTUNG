package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.math.Quaternion;
import de.ecconia.java.opentung.math.Vector3;

public class GenericComponent
{
	private Quaternion rotation;
	private Vector3 position;
	
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
	
	public Quaternion getRotation()
	{
		return rotation;
	}
}

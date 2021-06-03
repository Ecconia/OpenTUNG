package de.ecconia.java.opentung.core.data;

import de.ecconia.java.opentung.components.meta.Part;
import de.ecconia.java.opentung.util.math.Quaternion;
import de.ecconia.java.opentung.util.math.Vector3;

public class Hitpoint
{
	private final Part hitPart;
	private final double distance;
	
	public Hitpoint(Part hitPart, double distance)
	{
		this.hitPart = hitPart;
		this.distance = distance;
	}
	
	public Hitpoint()
	{
		this.hitPart = null;
		this.distance = Double.MAX_VALUE;
	}
	
	public Part getHitPart()
	{
		return hitPart;
	}
	
	public double getDistance()
	{
		return distance;
	}
	
	public boolean isEmpty()
	{
		return hitPart == null;
	}
	
	public boolean isBoard()
	{
		return false;
	}
	
	public boolean canBePlacedOn()
	{
		return false;
	}
	
	//Wire drawing data: TBI: Can these values be stored somewhere else?
	
	private Vector3 wireCenterPosition;
	private Quaternion wireAlignment;
	private double wireDistance;
	
	public void setWireData(Quaternion wireAlignment, Vector3 wireCenterPosition, double wireDistance)
	{
		this.wireAlignment = wireAlignment;
		this.wireCenterPosition = wireCenterPosition;
		this.wireDistance = wireDistance;
	}
	
	public double getWireDistance()
	{
		return wireDistance;
	}
	
	public Quaternion getWireAlignment()
	{
		return wireAlignment;
	}
	
	public Vector3 getWireCenterPosition()
	{
		return wireCenterPosition;
	}
}

package de.ecconia.java.opentung.core.data;

import de.ecconia.java.opentung.components.meta.Part;

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
}

package de.ecconia.java.opentung.raycast;

import de.ecconia.java.opentung.components.meta.Part;

public class RayCastResult
{
	private final double distance;
	private final Part match;
	
	public RayCastResult(double distance, Part match)
	{
		this.distance = distance;
		this.match = match;
	}
	
	public double getDistance()
	{
		return distance;
	}
	
	public Part getMatch()
	{
		return match;
	}
}

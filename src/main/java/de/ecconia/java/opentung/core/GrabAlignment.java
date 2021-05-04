package de.ecconia.java.opentung.core;

import de.ecconia.java.opentung.util.math.Quaternion;

public class GrabAlignment
{
	private final Quaternion absolute;
	private final Quaternion relative;
	
	public GrabAlignment(Quaternion absolute, Quaternion relative)
	{
		this.absolute = absolute;
		this.relative = relative;
	}
	
	public Quaternion getAbsolute()
	{
		return absolute;
	}
	
	public Quaternion getRelative()
	{
		return relative;
	}
}

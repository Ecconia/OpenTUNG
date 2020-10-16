package de.ecconia.java.opentung.util.math;

import de.ecconia.java.opentung.util.Ansi;

public class Vector2
{
	private final double x;
	private final double y;
	
	public Vector2(double x, double y)
	{
		this.x = x;
		this.y = y;
	}
	
	public double getX()
	{
		return x;
	}
	
	public double getY()
	{
		return y;
	}
	
	@Override
	public String toString()
	{
		return "[X: " + Ansi.yellow + fix(x) + Ansi.r + " Y: " + Ansi.yellow + fix(y) + Ansi.r + "]";
	}
	
	private String fix(double value)
	{
		double a = Math.abs(value);
		if(a == 0.0)
		{
			return " 0.0";
		}
		if(a < 0.00000000000001D)
		{
			return "~0.?";
		}
		else
		{
			String s = String.valueOf(value);
			if(s.lastIndexOf('E') == -1)
			{
				if(s.length() > 8)
				{
					return s.substring(0, 8);
				}
				else
				{
					return s;
				}
			}
			else
			{
				return s;
			}
		}
	}
}

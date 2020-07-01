package de.ecconia.java.opentung.components.fragments;

import de.ecconia.java.opentung.math.Vector3;

public class Color
{
	public static final Color material = new Color(255, 255, 255);
	//These two are redundant in the conductor-shader:
	public static final Color circuitON = new Color(255, 0, 0);
	public static final Color circuitOFF = new Color(0, 0, 0);
	
	public static final Color boardDefault = new Color(195, 195, 195);
	public static final Color interactable = new Color(109, 53, 6);
	public static final Color noisemakerOFF = new Color(56, 22, 120);
	public static final Color noisemakerON = new Color(168, 127, 223);
	public static final Color snappingPeg = new Color(0, 150, 141);
	
	public static final Color displayOff = new Color(32, 32, 32);
	public static final Color displayYellow = new Color(255, 227, 2);
	public static final Color displayBlue = new Color(0, 50, 200);
	public static final Color displayGreen = new Color(20, 150, 0);
	public static final Color displayRed = new Color(186, 0, 0);
	public static final Color displayOrange = new Color(225, 95, 0);
	public static final Color displayPurple = new Color(142, 18, 255);
	public static final Color displayWhite = new Color(200, 200, 210);
	
	private final int r, g, b;
	
	public Color(int r, int g, int b)
	{
		this.r = r;
		this.b = b;
		this.g = g;
	}
	
	public static Color byColorDisplayIndex(int index)
	{
		switch(index)
		{
			case 0:
				return displayOff;
			case 1:
				return displayYellow;
			case 2:
				return displayBlue;
			case 3:
				return displayGreen;
			case 4:
				return displayRed;
			case 5:
				return displayOrange;
			case 6:
				return displayPurple;
			case 7:
				return displayWhite;
			default:
				throw new RuntimeException("Attempted to resolve ColorDisplay index: " + index);
		}
	}
	
	public static Color fromComponent(float r, float g, float b)
	{
		return new Color(
				(int) (r * 255f),
				(int) (g * 255f),
				(int) (b * 255f)
		);
	}
	
	public int getR()
	{
		return r;
	}
	
	public int getG()
	{
		return g;
	}
	
	public int getB()
	{
		return b;
	}
	
	//Export as 0...1 values.
	public Vector3 asVector()
	{
		return new Vector3(r / 255.0, g / 255.0, b / 255.0);
	}
}

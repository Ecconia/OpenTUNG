package de.ecconia.java.opentung.components.fragments;

import de.ecconia.java.opentung.math.Vector3;

public class Color
{
	public static final Color material = new Color(1, 1, 1);
	public static final Color circuitON = new Color(1, 0, 0);
	public static final Color circuitOFF = new Color(0, 0, 0);
	
	public static final Color interactable = Color.rgb(109, 53, 6);
	public static final Color noisemakerOFF = Color.rgb(109, 53, 6);
	public static final Color noisemakerON = Color.rgb(168, 127, 223);
	public static final Color snappingPeg = Color.rgb(0, 150, 141);
	
	public static final Color displayOff = Color.rgb(32, 32, 32);
	public static final Color displayRed = Color.rgb(186, 0, 0);
	public static final Color displayGreen = Color.rgb(20, 150, 0);
	public static final Color displayBlue = Color.rgb(0, 50, 200);
	public static final Color displayYellow = Color.rgb(255, 227, 2);
	public static final Color displayOrange = Color.rgb(225, 95, 0);
	public static final Color displayPurple = Color.rgb(142, 18, 255);
	public static final Color displayWhite = Color.rgb(200, 200, 210);
	
	private final double r, g, b;
	
	public Color(double r, double g, double b)
	{
		this.r = r;
		this.b = b;
		this.g = g;
	}
	
	public double getR()
	{
		return r;
	}
	
	public double getG()
	{
		return g;
	}
	
	public double getB()
	{
		return b;
	}
	
	public static Color rgb(int r, int g, int b)
	{
		return new Color(
				(double) r / 255D,
				(double) g / 255D,
				(double) b / 255D
		);
	}
	
	//TODO: Fix code containing this method. (Deprecated)
	public Vector3 asVector()
	{
		return new Vector3(r, g, b);
	}
}

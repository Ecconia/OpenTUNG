package de.ecconia.java.opentung.components.fragments;

import de.ecconia.java.opentung.util.math.Vector3;

public enum Direction
{
	/**
	 * Right
	 */
	XPos(new Vector3(1, 0, 0)),
	/**
	 * Left
	 */
	XNeg(new Vector3(-1, 0, 0)),
	/**
	 * Up
	 */
	YPos(new Vector3(0, 1, 0)),
	/**
	 * Down
	 */
	YNeg(new Vector3(0, -1, 0)),
	/**
	 * Forward
	 */
	ZPos(new Vector3(0, 0, 1)),
	/**
	 * Backwards
	 */
	ZNeg(new Vector3(0, 0, -1));
	
	private final Vector3 direction;
	
	Direction(Vector3 direction)
	{
		this.direction = direction;
	}
	
	public Vector3 asVector()
	{
		return direction;
	}
}

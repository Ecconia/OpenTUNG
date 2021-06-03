package de.ecconia.java.opentung.core.data;

import de.ecconia.java.opentung.components.meta.Part;
import de.ecconia.java.opentung.util.math.Vector3;

public class HitpointBoard extends HitpointContainer
{
	private final Vector3 localNormal;
	private final Vector3 collisionPointBoardSpace;
	
	public HitpointBoard(Part hitPart, double distance, Vector3 localNormal, Vector3 collisionPointBoardSpace)
	{
		super(hitPart, distance);
		this.localNormal = localNormal;
		this.collisionPointBoardSpace = collisionPointBoardSpace;
	}
	
	@Override
	public boolean isBoard()
	{
		return true;
	}
	
	public Vector3 getLocalNormal()
	{
		return localNormal;
	}
	
	public Vector3 getCollisionPointBoardSpace()
	{
		return collisionPointBoardSpace;
	}
}

package de.ecconia.java.opentung.core.data;

import de.ecconia.java.opentung.components.meta.Part;
import de.ecconia.java.opentung.util.math.Quaternion;
import de.ecconia.java.opentung.util.math.Vector3;

public class HitpointContainer extends Hitpoint
{
	private Vector3 position;
	private Vector3 normal;
	private Quaternion alignment;
	
	public HitpointContainer(Part hitPart)
	{
		super(hitPart);
	}
	
	public void setPosition(Vector3 position)
	{
		this.position = position;
	}
	
	public void setNormal(Vector3 normal)
	{
		this.normal = normal;
	}
	
	public Vector3 getPosition()
	{
		return position;
	}
	
	public Vector3 getNormal()
	{
		return normal;
	}
	
	@Override
	public boolean canBePlacedOn()
	{
		return true;
	}
	
	public void setAlignment(Quaternion alignment)
	{
		this.alignment = alignment;
	}
	
	public Quaternion getAlignment()
	{
		return alignment;
	}
	
	//Board placement data:
	private Vector3 boardCenterPosition;
	private int boardX, boardZ;
	
	public void setBoardData(Vector3 boardCenterPosition, int boardX, int boardZ)
	{
		this.boardCenterPosition = boardCenterPosition;
		this.boardX = boardX;
		this.boardZ = boardZ;
	}
	
	public Vector3 getBoardCenterPosition()
	{
		return boardCenterPosition;
	}
	
	public int getBoardX()
	{
		return boardX;
	}
	
	public int getBoardZ()
	{
		return boardZ;
	}
}

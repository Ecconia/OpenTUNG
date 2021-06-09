package de.ecconia.java.opentung.core.data;

import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.util.math.Vector3;

public class ResizeData
{
	private final CompBoard board;
	
	private Vector3 position;
	private int x, z;
	
	private Double pointX, pointZ;
	private boolean isMouseDown;
	
	public ResizeData(CompBoard board)
	{
		this.board = board;
		this.position = board.getPosition();
		this.x = board.getX();
		this.z = board.getZ();
	}
	
	public CompBoard getBoard()
	{
		return board;
	}
	
	public Vector3 getPosition()
	{
		return position;
	}
	
	public int getBoardX()
	{
		return x;
	}
	
	public int getBoardZ()
	{
		return z;
	}
	
	public boolean hasPoints()
	{
		return pointX != null;
	}
	
	public void setPoints(Double pointX, Double pointZ)
	{
		this.pointX = pointX;
		this.pointZ = pointZ;
	}
	
	public double getPointX()
	{
		return pointX;
	}
	
	public double getPointZ()
	{
		return pointZ;
	}
	
	public void setMouseDown(boolean mouseDown)
	{
		isMouseDown = mouseDown;
	}
	
	public boolean isMouseDown()
	{
		return isMouseDown;
	}
	
	private boolean isAxisX;
	
	public void setAxisX(boolean xMatch)
	{
		this.isAxisX = xMatch;
	}
	
	public boolean isAxisX()
	{
		return isAxisX;
	}
	
	private boolean isNegative;
	
	public void setNegative(boolean isNegative)
	{
		this.isNegative = isNegative;
	}
	
	public boolean isNegative()
	{
		return isNegative;
	}
	
	public void adjustSize(int squareChange) //If possible...
	{
		if(squareChange > 100)
		{
			squareChange = 100;
		}
		else if(squareChange < -100)
		{
			squareChange = -100;
		}
		//Ignore this for now.
		if(isAxisX)
		{
			//Expand/Shrink board:
			x += squareChange;
			//If the board smaller than the current minimum (1):
			if(x < 1)
			{
				//Pretend that the change amount was smaller:
				squareChange += (1 - x);
				x = 1; //And just use the minimum.
			}
			double offset = (double) squareChange * (isNegative ? -0.15 : 0.15);
			pointX += offset; //Change the last known mouse cursor position.
			modPos(offset, 0); //Change the board position.
		}
		else //Axis Z
		{
			//Expand/Shrink board:
			z += squareChange;
			//If the board smaller than the current minimum (1):
			if(z < 1)
			{
				//Pretend that the change amount was smaller:
				squareChange += (1 - z);
				z = 1; //And just use the minimum.
			}
			double offset = (double) squareChange * (isNegative ? -0.15 : 0.15);
			pointZ += offset; //Change the last known mouse cursor position.
			modPos(0, offset); //Change the board position.
		}
	}
	
	private void modPos(double x, double z)
	{
		position = board.getRotation().multiply(position);
		position = position.add(x, 0, z);
		position = board.getRotation().inverse().multiply(position);
	}
}

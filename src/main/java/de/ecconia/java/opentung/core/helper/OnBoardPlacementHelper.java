package de.ecconia.java.opentung.core.helper;

import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.components.meta.PlacementSettingBoardSide;
import de.ecconia.java.opentung.components.meta.PlacementSettingBoardSquare;
import de.ecconia.java.opentung.util.math.MathHelper;
import de.ecconia.java.opentung.util.math.Quaternion;
import de.ecconia.java.opentung.util.math.Vector3;

public class OnBoardPlacementHelper
{
	private final Quaternion alignment;
	private final boolean isSide;
	private final Boolean isSideX;
	private final Boolean isSideFar;
	private final Integer xSquareOffset;
	private final Integer zSquareOffset;
	private final double xHalf;
	private final double zHalf;
	private final Double xOffset;
	private final Double zOffset;
	
	public OnBoardPlacementHelper(CompBoard board, Vector3 localNormal, Vector3 collisionPointBoardSpace)
	{
		this.alignment = board.getRotation();
		
		//Adjust placement position according to component properties:
		isSide = localNormal.getY() == 0;
		
		//Get the radius's of the board:
		xHalf = board.getX() * 0.15;
		zHalf = board.getZ() * 0.15;
		
		//Transform into positive non-centered space:
		//Move the center of the board into one corner, so that the collision point is in the positive quarter:
		double x = collisionPointBoardSpace.getX() + xHalf;
		double z = collisionPointBoardSpace.getZ() + zHalf;
		
		//Calculate the amount of squares to the collision point:
		int xSquareOffset = (int) (x / 0.3);
		int zSquareOffset = (int) (z / 0.3);
		//Calculate the distance of the collision point within a square:
		double xOffset = x - (double) xSquareOffset * 0.3D;
		double zOffset = z - (double) zSquareOffset * 0.3D;
		
		//The 0.3 division does not properly work cause of rounding errors, when the number which clearly should be 0.3 is 0.29999999.
		if(xOffset > 0.299999)
		{
			xSquareOffset++;
			xOffset = 0;
		}
		if(zOffset > 0.299999)
		{
			zSquareOffset++;
			zOffset = 0;
		}
		
		//Apply values to fields:
		this.xSquareOffset = xSquareOffset;
		this.zSquareOffset = zSquareOffset;
		this.xOffset = xOffset;
		this.zOffset = zOffset;
		
		//Calculate side booleans:
		isSideX = isSide ? localNormal.getZ() == 0 : null;
		isSideFar = isSide ? (isSideX ? localNormal.getX() : localNormal.getZ()) >= 0 : null;
	}
	
	public boolean isSide()
	{
		return isSide;
	}
	
	public Vector3 middleEither()
	{
		if(isSide)
		{
			return sideMiddle();
		}
		else
		{
			return squareMiddle();
		}
	}
	
	public Vector3 auto(ModelHolder model, boolean isControl, Quaternion alignment)
	{
		//Placing non-board component onto a board:
		if(isSide)
		{
			if(model.getPlacementSettingBoardSide() == PlacementSettingBoardSide.Middle)
			{
				return sideMiddle();
			}
			else if(model.getPlacementSettingBoardSide() == PlacementSettingBoardSide.All)
			{
				if(isControl)
				{
					return sideAll();
				}
				else
				{
					return sideMiddle();
				}
			}
			else
			{
				return null; //Do not place it.
			}
		}
		else //Is top/bottom:
		{
			//Center of square:
			if(model.getPlacementSettingBoardSquare() == PlacementSettingBoardSquare.Middle)
			{
				return squareMiddle();
			}
			else if(model.getPlacementSettingBoardSquare() == PlacementSettingBoardSquare.Cross)
			{
				if(isControl)
				{
					//Calculate the alignment of a snapping peg:
					return squareCross(alignment);
				}
				else
				{
					return squareMiddle();
				}
			}
			else //if(model.getPlacementSettingBoardSquare() == PlacementSettingBoardSquare.All)
			{
				if(isControl)
				{
					//Calculate the nearest of 9 positions in a square...
					return squareAll();
				}
				else
				{
					return squareMiddle();
				}
			}
		}
	}
	
	private Vector3 squareCross(Quaternion alignment)
	{
		final double squareThird = 0.1D;
		int xFineStep = (int) (xOffset / squareThird);
		int zFineStep = (int) (zOffset / squareThird);
		
		Quaternion fixedAlignment = this.alignment.multiply(alignment.inverse()).inverse();
		Vector3 orientation = fixedAlignment.multiply(Vector3.zn);
		
		double angle = MathHelper.angleFromVectors(orientation, Vector3.zn);
		boolean x = angle >= -0.001 && angle <= 0.001 || angle >= 179.999 && angle <= 180.001;
		angle = MathHelper.angleFromVectors(orientation, Vector3.xn);
		boolean z = angle >= -0.001 && angle <= 0.001 || angle >= 179.999 && angle <= 180.001;
		
		if(!x && !z)
		{
			//Non 90° rotation, reject.
			return null;
		}
		
		if(z)
		{
			return new Vector3(
					-xHalf + xSquareOffset * 0.3 + 0.15D,
					0,
					-zHalf + zSquareOffset * 0.3 + zFineStep * squareThird + 0.05D
			);
		}
		else
		{
			return new Vector3(
					-xHalf + xSquareOffset * 0.3 + xFineStep * squareThird + 0.05D,
					0,
					-zHalf + zSquareOffset * 0.3 + 0.15D
			);
		}
	}
	
	private Vector3 squareAll()
	{
		final double squareThird = 0.1D;
		int xFineStep = (int) (xOffset / squareThird);
		int zFineStep = (int) (zOffset / squareThird);
		return new Vector3(
				-xHalf + xSquareOffset * 0.3 + xFineStep * squareThird + 0.05D,
				0,
				-zHalf + zSquareOffset * 0.3 + zFineStep * squareThird + 0.05D
		);
	}
	
	private Vector3 squareMiddle()
	{
		return new Vector3(xSquareOffset * 0.3 + 0.15 - xHalf, 0, zSquareOffset * 0.3 + 0.15 - zHalf);
	}
	
	private Vector3 sideMiddle()
	{
		double xExtra = xSquareOffset * 0.3;
		double zExtra = zSquareOffset * 0.3;
		double belowSurface = isSideFar ? -0.075 : +0.075;
		if(isSideX)
		{
			return new Vector3(-xHalf + xExtra + belowSurface, 0, -zHalf + zExtra + 0.15);
		}
		else
		{
			return new Vector3(-xHalf + xExtra + 0.15, 0, -zHalf + zExtra + belowSurface);
		}
	}
	
	private Vector3 sideAll()
	{
		double xExtra = xSquareOffset * 0.3;
		double zExtra = zSquareOffset * 0.3;
		double belowSurface = isSideFar ? -0.075 : +0.075;
		if(isSideX)
		{
			int steps = (int) (zOffset / 0.1);
			return new Vector3(-xHalf + xExtra + belowSurface, 0, -zHalf + zExtra + (steps * 0.1) + 0.05);
		}
		else
		{
			int steps = (int) (xOffset / 0.1);
			return new Vector3(-xHalf + xExtra + (steps * 0.1) + 0.05, 0, -zHalf + zExtra + belowSurface);
		}
	}
}

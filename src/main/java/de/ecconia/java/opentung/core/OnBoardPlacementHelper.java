package de.ecconia.java.opentung.core;

import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.components.meta.PlacementSettingBoardSide;
import de.ecconia.java.opentung.components.meta.PlacementSettingBoardSquare;
import de.ecconia.java.opentung.util.math.Vector3;

public class OnBoardPlacementHelper
{
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
		//Adjust placement position according to component properties:
		isSide = localNormal.getY() == 0;
		
		//Get the radius's of the board:
		xHalf = board.getX() * 0.15;
		zHalf = board.getZ() * 0.15;
		
		//Transform into positive non-centered space:
		//Move the center of the board into one corner, so that the collision point is in the positive quarter:
		double x = collisionPointBoardSpace.getX() + xHalf;
		double z = collisionPointBoardSpace.getZ() + zHalf;

//		if(isSide)
//		{
//			if(localNormal.getX() == 0) // Z side
//			{
//				//Get the amount of squares until you get to the collision square:
//				zSquareOffset = null;
//				xSquareOffset = (int) (x / 0.3);
//				//Calculate the center pos:
//				xOffset = xSquareOffset * 0.3 + 0.15;
//				zOffset = localNormal.getZ() < 0 ? -0.075 : 0.075;
//			}
//			else // X side
//			{
//				//Get the amount of squares until you get to the collision square:
//				xSquareOffset = null;
//				zSquareOffset = (int) (z / 0.3);
//				//Calculate the center pos:
//				xOffset = localNormal.getX() < 0 ? -0.075 : 0.075;
//				zOffset = zSquareOffset * 0.3 + 0.15;
//			}
//		}
//		else //Square side - bottom/top
//		{
		//Get the amount of squares until you get to the collision square:
		xSquareOffset = (int) (x / 0.3);
		zSquareOffset = (int) (z / 0.3);
		//Calculate the position within a square:
		xOffset = x - (double) xSquareOffset * 0.3D;
		zOffset = z - (double) zSquareOffset * 0.3D;
		
		isSideX = isSide ? localNormal.getZ() == 0 : null;
		isSideFar = isSide ? (isSideX ? localNormal.getX() : localNormal.getZ()) >= 0 : null;
//		}
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
	
	public Vector3 auto(ModelHolder model, boolean isControl)
	{
		//Placing non-board component onto a board:
		if(isSide)
		{
			if(model.getPlacementSettingBoardSide() == PlacementSettingBoardSide.Middle)
			{
			
			}
			else if(model.getPlacementSettingBoardSide() == PlacementSettingBoardSide.All)
			{
			
			}
			else
			{
			
			}
			//TODO: Change to correct:
			return squareMiddle(); //Totally wrong here, lol.
		}
		else //Is top/bottom:
		{
			//Center of square:
			if(model.getPlacementSettingBoardSquare() == PlacementSettingBoardSquare.Middle)
			{
				return squareMiddle();
			}
			else if(model.getPlacementSettingBoardSquare() == PlacementSettingBoardSquare.AlsoBorders)
			{
				return squareMiddleAlsoBorders();
			}
			else if(model.getPlacementSettingBoardSquare() == PlacementSettingBoardSquare.Cross)
			{
				if(isControl)
				{
					//Calculate the alignment of a snapping peg:
					return squareCross();
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
	
	private Vector3 squareCross()
	{
		throw new RuntimeException("Not implemented yet.");
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
	
	private Vector3 squareMiddleAlsoBorders()
	{
		final double squareQuad = 0.075;
		int xFineStep = (int) (xOffset / squareQuad);
		int zFineStep = (int) (zOffset / squareQuad);
		if(xFineStep == 1)
		{
			//Move up in the middle
			xFineStep = 2;
		}
		else if(xFineStep == 3)
		{
			//Move up onto the border
			xFineStep = 4;
		}
		if(zFineStep == 1)
		{
			//Move up in the middle
			zFineStep = 2;
		}
		else if(zFineStep == 3)
		{
			//Move up onto the border
			zFineStep = 4;
		}
		if((xFineStep == 0 || xFineStep == 4) && (zFineStep == 0 || zFineStep == 4))
		{
			Vector3 position = new Vector3(xOffset, 0, zOffset);
			//We are in a corner of the 4² grid: Check which border is closest:
			Vector3 px = new Vector3(0.3, 0, 0.15);
			double minDistance = px.subtract(position).lengthSqared();
			Vector3 ret = px;
			Vector3 nx = new Vector3(0, 0, 0.15);
			double dist = nx.subtract(position).lengthSqared();
			if(dist < minDistance)
			{
				minDistance = dist;
				ret = nx;
			}
			Vector3 pz = new Vector3(0.15, 0, 0.3);
			dist = pz.subtract(position).lengthSqared();
			if(dist < minDistance)
			{
				minDistance = dist;
				ret = pz;
			}
			Vector3 nz = new Vector3(0.15, 0, 0);
			dist = nz.subtract(position).lengthSqared();
			if(dist < minDistance)
			{
				ret = nz;
			}
			return ret.add(new Vector3(-xHalf + xSquareOffset * 0.3, 0, -zHalf + zSquareOffset * 0.3));
		}
		return new Vector3(
				-xHalf + xSquareOffset * 0.3 + xFineStep * squareQuad,
				0,
				-zHalf + zSquareOffset * 0.3 + zFineStep * squareQuad
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
		
		if(isSideX)
		{
			if(isSideFar)
			{
				System.out.println("X+");
				return new Vector3(-xHalf + xExtra, 0, -zHalf + zExtra + 0.15);
			}
			else
			{
				System.out.println("X-");
				return new Vector3(-xHalf + xExtra, 0, -zHalf + zExtra + 0.15);
			}
			
//			double flip = isSideFar ? 1 : -1;
//			return new Vector3(xSquareOffset * 0.3 - xHalf + flip * 0.15, 0, zSquareOffset * 0.3 - zHalf + 0.15);
		}
		else
		{
//			double flip = isSideFar ? 1 : -1;
//			double x = xSquareOffset * 0.3 - xHalf;
//			double z = zSquareOffset * 0.3 - zHalf;
//			System.out.println(xSquareOffset + " | " + zSquareOffset);
//			return new Vector3(x, 0, z + flip * 0.075);
			
			if(isSideFar)
			{
				System.out.println("Z+");
				return new Vector3(-xHalf + xExtra + 0.15, 0, -zHalf + zExtra);
			}
			else
			{
				System.out.println("Z-");
				return new Vector3(-xHalf + xExtra + 0.15, 0, -zHalf + zExtra);
			}
//			return new Vector3(xSquareOffset * 0.3 - xHalf + 0.15, 0, zSquareOffset * 0.3 - zHalf + 0.075);
		}
	}
}

//	public void calculatePlacementPosition()
//	{
//		//If looking at a board
//		Part part = currentlySelected;
//
//		if(part instanceof CompMount)
//		{
//			CompMount parent = (CompMount) part;
//			//Is placement of the current component on a Mount allowed?
//			if(isGrabbing())
//			{
//
//			}
//			else //Normal placement:
//			{
//				PlaceableInfo placeable = sharedData.getCurrentPlaceable();
//				if(placeable != null)
//				{
//					ModelHolder model = placeable.getModel();
//					if(placeable == CompBoard.info)
//					{
//						double extraY = 0.15;
//						if(!placeableBoardIsLaying)
//						{
//							extraY += 0.075;
//						}
//						Vector3 placementNormal = parent.getRotation().inverse().multiply(Vector3.yp).normalize();
//						Vector3 placementPosition = parent.getPosition().add(placementNormal.multiply(CompMount.MOUNT_HEIGHT).addY(extraY));
//						placementData = new PlacementData(placementPosition, placementNormal, parent, Vector3.yp);
//						return;
//					}
//					else if(model.canBePlacedOnMounts())
//					{
//						Vector3 placementNormal = parent.getRotation().inverse().multiply(Vector3.yp).normalize();
//						Vector3 placementPosition = parent.getPosition().add(placementNormal.multiply(CompMount.MOUNT_HEIGHT));
//						placementData = new PlacementData(placementPosition, placementNormal, parent, Vector3.yp);
//						return;
//					}
//				}
//			}
//		}
//		else if(part instanceof CompBoard) //If parent container is a board
//		{
//			CompBoard board = (CompBoard) part;
//			//Calculate the collision point and face vector in board space:
//			CPURaycast.CollisionResult result = CPURaycast.collisionPoint(board, camera);
//			Vector3 localNormal = result.getLocalNormal();
//			Vector3 collisionPointBoardSpace = result.getCollisionPointBoardSpace();
//
//			PlacementHelper placer = new PlacementHelper(board, localNormal, collisionPointBoardSpace);
//
//			if(isGrabbing())
//			{
//
//			}
//			else //Normal placement:
//			{
//				PlaceableInfo placeable = sharedData.getCurrentPlaceable();
//				if(placeable != null)
//				{
//					ModelHolder model = placeable.getModel();
//					if(placeable == CompBoard.info) //If placing a board
//					{
////						collisionPointBoardSpace = placer.middleEither();
//						//Boards have their center within, thus the offset needs to be adjusted:
//						collisionPointBoardSpace = collisionPointBoardSpace.add(localNormal.multiply(placeableBoardIsLaying ? 0.15 : (0.15 + 0.075)));
//					}
//					else //if placing any new component
//					{
//						//TODO: Move placement abort to right click, and replace ALT with CONTROL here.
//						collisionPointBoardSpace = placer.auto(model, inputHandler.getController3D().isAlt());
//					}
//				}
//			}
//
//			//Convert values back to global space and apply:
//			Vector3 placementPosition = board.getRotation().inverse().multiply(collisionPointBoardSpace).add(board.getPosition());
//			Vector3 placementNormal = board.getRotation().inverse().multiply(localNormal).normalize(); //Safety normalization.
//			CompBoard placementBoard = board;
//
//			placementData = new PlacementData(placementPosition, placementNormal, placementBoard, localNormal);
//			return;
//		}
//
//		//Fallback case:
//		placementData = null; //Only place on boards.
//	}

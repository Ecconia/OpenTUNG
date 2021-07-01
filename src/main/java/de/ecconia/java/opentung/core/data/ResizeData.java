package de.ecconia.java.opentung.core.data;

import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.CompMount;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.core.helper.BoardRelationHelper;
import de.ecconia.java.opentung.util.math.Vector2;
import de.ecconia.java.opentung.util.math.Vector3;

public class ResizeData
{
	private final CompBoard board;
	
	private Vector3 position;
	private int x, z;
	
	private Double pointX, pointZ;
	private boolean isMouseDown;
	
	private boolean allowPX = true;
	private boolean allowNX = true;
	private boolean allowPZ = true;
	private boolean allowNZ = true;
	
	//For debugging mode, the MAX_VALUE have to be replaced with something like 1000 else the thing dies the clipping space death.
	public double px = -Double.MAX_VALUE;
	public double nx = Double.MAX_VALUE;
	public double pz = -Double.MAX_VALUE;
	public double nz = Double.MAX_VALUE;
	
	public ResizeData(CompBoard board)
	{
		this.board = board;
		this.position = board.getPositionGlobal();
		this.x = board.getX();
		this.z = board.getZ();
		
		//Since we gonna add a small padding later on, lets subtract it first, so that Double does not get back to us. NaN or whatever might happen.
		nz -= 0.002;
		pz += 0.002;
		nx -= 0.002;
		px += 0.002;
		
		if(board.getParent() != null)
		{
			CompContainer parent = (CompContainer) board.getParent();
			Vector3 vec;
			if(parent instanceof CompBoard)
			{
				BoardRelationHelper rel = new BoardRelationHelper(board, (CompBoard) parent); //WARNING: Treats parent as child! Cause we only care for the relation.
				vec = rel.getAttachmentNormal();
				if(!rel.isComplexRelated())
				{
					//Values are fine, lets calculate bounds:
					expandBounds(rel);
				}
			}
			else
			{
				vec = parent.getAlignmentGlobal().inverse().multiply(Vector3.yp);
				//Calculate placement position, as if mount is standing on board:
				Vector3 globalPosition = parent.getPositionGlobal().add(vec.multiply(CompMount.MOUNT_HEIGHT + 0.15));
				Vector3 positionBoardSpace = board.getAlignmentGlobal().multiply(globalPosition.subtract(position));
				expandBounds(positionBoardSpace);
				//Finalization:
				vec = vec.multiply(-1.0); //Invert, cause the vector is from the parents view.
			}
			if(vec != null) //Might be missing, if the boards are complex related (as in relation is broken already)
			{
				vec = board.getAlignmentGlobal().multiply(vec);
				removeSideIfMatch(vec);
			}
		}
		for(Component child : board.getChildren())
		{
			Vector3 vec;
			if(child instanceof CompBoard)
			{
				BoardRelationHelper rel = new BoardRelationHelper(board, (CompBoard) child); //Using the parents vector, so no inverting.
				vec = rel.getAttachmentNormal();
				if(!rel.isComplexRelated())
				{
					//Values are fine, lets calculate bounds:
					expandBounds(rel);
				}
			}
			else
			{
				vec = child.getAlignmentGlobal().inverse().multiply(Vector3.yp);
				//Calculate minimum:
				Vector3 positionBoardSpace = board.getAlignmentGlobal().multiply(child.getPositionGlobal().subtract(position));
				expandBounds(positionBoardSpace);
			}
			if(vec != null) //Might be missing, if the boards are complex related (as in relation is broken already)
			{
				vec = board.getAlignmentGlobal().multiply(vec);
				removeSideIfMatch(vec);
			}
		}
		
		//It is important to increase the bounding rect a bit, because Mounts may stand a bit outside of it. (They are within, but not on the correct square).
		nz += 0.001;
		pz -= 0.001;
		nx += 0.001;
		px -= 0.001;
	}
	
	private void expandBounds(BoardRelationHelper rel)
	{
		if(rel.isAttachedAtSide())
		{
			if(rel.isSideX()) //Side normal is point in X axis.
			{
				double min = rel.getMinimumPoint().getY();
				double max = rel.getMaximumPoint().getY();
				
				if(min > pz)
				{
					pz = min;
				}
				if(max < nz)
				{
					nz = max;
				}
			}
			else // SideZ
			{
				double min = rel.getMinimumPoint().getX();
				double max = rel.getMaximumPoint().getX();
				
				if(min > px)
				{
					px = min;
				}
				if(max < nx)
				{
					nx = max;
				}
			}
		}
		else
		{
			Vector2 min = rel.getMinimumPoint();
			Vector2 max = rel.getMaximumPoint();
			
			if(min.getY() > pz)
			{
				pz = min.getY();
			}
			if(max.getY() < nz)
			{
				nz = max.getY();
			}
			if(min.getX() > px)
			{
				px = min.getX();
			}
			if(max.getX() < nx)
			{
				nx = max.getX();
			}
		}
	}
	
	private void expandBounds(Vector3 positionBoardSpace)
	{
		double xx = positionBoardSpace.getX();
		double zz = positionBoardSpace.getZ();
		if(xx < nx)
		{
			nx = xx;
		}
		if(xx > px)
		{
			px = xx;
		}
		if(zz < nz)
		{
			nz = zz;
		}
		if(zz > pz)
		{
			pz = zz;
		}
	}
	
	private void removeSideIfMatch(Vector3 vec)
	{
		if(vec.getY() < 0.1 && vec.getY() > -0.1)
		{
			if(vec.getX() < -0.9)
			{
				allowNX = false;
			}
			else if(vec.getX() > 0.9)
			{
				allowPX = false;
			}
			else if(vec.getZ() < -0.9)
			{
				allowNZ = false;
			}
			else if(vec.getZ() > 0.9)
			{
				allowPZ = false;
			}
		}
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
		if(isAxisX)
		{
			//Calculate how big the board would be with this change:
			int wouldBeX = x + squareChange;
			//If the board smaller than the minimum size:
			if(wouldBeX < 1)
			{
				//Pretend that the change amount was smaller:
				squareChange += (1 - wouldBeX);
				wouldBeX = 1; //And just use the minimum.
			}
			//Grab the actual minimum:
			double posMin = isNegative ? -nx : px;
			//Calculate where the board would end with new size: (We only change one side, without changing position, thus scale different for squareChange)
			double newPos = x * 0.15 + squareChange * 0.3;
			if(newPos < posMin) //Check if we would go below the minimum.
			{
				double error = posMin - newPos; //Calculate by how much we are below.
				int failAmount = (int) Math.ceil(error / 0.3 - 0.0000001); //Calculate how many squares that is, round the value up. The 0.000001 is to remove rounding errors which would cause a number to get rounded up too much.
				squareChange += failAmount; //Expand the negative squareAmount number.
				wouldBeX += failAmount; //Correct the new size.
			}
			x = wouldBeX; //Apply.
			
			double offset = (double) squareChange * (isNegative ? -0.15 : 0.15);
			pointX += offset; //Change the last known mouse cursor position.
			//Update the two borders, cause they are relative to the position, which just changed.
			px -= offset;
			nx -= offset;
			modPos(offset, 0);
		}
		else //Axis Z
		{
			//Calculate how big the board would be with this change:
			int wouldBeZ = z + squareChange;
			//If the board smaller than the minimum size:
			if(wouldBeZ < 1)
			{
				//Pretend that the change amount was smaller:
				squareChange += (1 - wouldBeZ);
				wouldBeZ = 1; //And just use the minimum.
			}
			//Grab the actual minimum:
			double posMin = isNegative ? -nz : pz;
			//Calculate where the board would end with new size: (We only change one side, without changing position, thus scale different for squareChange)
			double newPos = z * 0.15 + squareChange * 0.3;
			if(newPos < posMin) //Check if we would go below the minimum.
			{
				double error = posMin - newPos; //Calculate by how much we are below.
				int failAmount = (int) Math.ceil(error / 0.3 - 0.0000001); //Calculate how many squares that is, round the value up. The 0.000001 is to remove rounding errors which would cause a number to get rounded up too much.
				squareChange += failAmount; //Expand the negative squareAmount number.
				wouldBeZ += failAmount; //Correct the new size.
			}
			z = wouldBeZ; //Apply.
			
			double offset = (double) squareChange * (isNegative ? -0.15 : 0.15);
			pointZ += offset; //Change the last known mouse cursor position.
			//Update the two borders, cause they are relative to the position, which just changed.
			pz -= offset;
			nz -= offset;
			modPos(0, offset); //Change the board position.
		}
	}
	
	private void modPos(double x, double z)
	{
		position = board.getAlignmentGlobal().multiply(position);
		position = position.add(x, 0, z);
		position = board.getAlignmentGlobal().inverse().multiply(position);
	}
	
	public boolean allowsNX()
	{
		return allowNX;
	}
	
	public boolean allowsNZ()
	{
		return allowNZ;
	}
	
	public boolean allowsPX()
	{
		return allowPX;
	}
	
	public boolean allowsPZ()
	{
		return allowPZ;
	}
	
	public boolean isAllowed()
	{
		return isAxisX ?
				(isNegative ? allowNX : allowPX) :
				(isNegative ? allowNZ : allowPZ);
	}
	
	public boolean isResizeAllowed()
	{
		return allowNX || allowPX || allowNZ || allowPZ;
	}
}

package de.ecconia.java.opentung.core.helper;

import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.util.math.Quaternion;
import de.ecconia.java.opentung.util.math.Vector2;
import de.ecconia.java.opentung.util.math.Vector3;

public class BoardRelationHelper
{
	private final CompBoard parent;
	private final ThreeValues childBoundsInParentSpace = new ThreeValues();
	
	private final Vector3 childCenter;
	
	private boolean complexRelated;
	private Vector3 localParentFaceAxis;
	
	public BoardRelationHelper(CompBoard parent, CompBoard child)
	{
		this.parent = parent;
		
//		System.out.println("---Attachment---");
//		System.out.println("Parent:");
//		System.out.println(" X: " + (parent.getRotation().inverse().multiply(Vector3.xp)));
//		System.out.println(" Y: " + (parent.getRotation().inverse().multiply(Vector3.yp)));
//		System.out.println(" Z: " + (parent.getRotation().inverse().multiply(Vector3.zp)));
//		System.out.println(" Pos: " + parent.getPosition());
//		System.out.println("Child:");
//		System.out.println(" X: " + (child.getRotation().inverse().multiply(Vector3.xp)));
//		System.out.println(" Y: " + (child.getRotation().inverse().multiply(Vector3.yp)));
//		System.out.println(" Z: " + (child.getRotation().inverse().multiply(Vector3.zp)));
//		System.out.println(" Pos: " + child.getPosition());
//		System.out.println(" Pos Rel: " + child.getPosition().subtract(parent.getPosition()));
		
		//Calculate child position and alignment in parent board space:
		childCenter = parent.getRotation().inverse().multiply(child.getPosition().subtract(parent.getPosition()));
		Quaternion childAlignment = child.getRotation().multiply(parent.getRotation());
		Quaternion childAlignmentInverse = childAlignment.inverse();
		
//		System.out.println();
//		System.out.println("Child in parent space:");
//		System.out.println(" X: " + (childAlignment.inverse().multiply(Vector3.xp)));
//		System.out.println(" Y: " + (childAlignment.inverse().multiply(Vector3.yp)));
//		System.out.println(" Z: " + (childAlignment.inverse().multiply(Vector3.zp)));
//		System.out.println(" Pos: " + childCenter);
		
		//Calculate the children bounds:
		{
			//Use the alignment in parent space, to check which
			if(assignChildBounds((double) child.getX() * 0.15D, childAlignmentInverse.multiply(Vector3.xp)))
			{
				complexRelated = true;
			}
			if(assignChildBounds(0.5D * 0.15D, childAlignmentInverse.multiply(Vector3.yp)))
			{
				complexRelated = true;
			}
			if(assignChildBounds((double) child.getZ() * 0.15D, childAlignmentInverse.multiply(Vector3.zp)))
			{
				complexRelated = true;
			}
		}
		
//		if(!complexRelated)
//		{
//			System.out.println();
//			System.out.println("Child size X: " + childBoundsInParentSpace.x);
//			System.out.println("Child size Y: " + childBoundsInParentSpace.y);
//			System.out.println("Child size Z: " + childBoundsInParentSpace.z);
//		}
//		System.out.println();
		
		//Store the axes the board is located in, null if none and else the right direction:
		double parentX = (double) parent.getX() * 0.15D;
		double parentY = 0.075D;
		double parentZ = (double) parent.getZ() * 0.15D;
		Vector3 vecX = childCenter.getX() >= parentX ? Vector3.xp : (childCenter.getX() <= -parentX ? Vector3.xn : null);
		Vector3 vecY = childCenter.getY() >= parentY ? Vector3.yp : (childCenter.getY() <= -parentY ? Vector3.yn : null);
		Vector3 vecZ = childCenter.getZ() >= parentZ ? Vector3.zp : (childCenter.getZ() <= -parentZ ? Vector3.zn : null);
		int count = count(vecX, vecY, vecZ);
		if(count == 1)
		{
			localParentFaceAxis = vecX != null ? vecX : (vecY != null ? vecY : vecZ);
		}
		else if(!complexRelated)
		{
			if(vecX != null)
			{
				double parentPos = parentX;
				double childPos = childCenter.getX() * vecX.getX() - childBoundsInParentSpace.x;
//			    System.out.println("Comparing X: " + parentPos + " | " + childPos);
				if(almostSame(parentPos, childPos))
				{
//			    	System.out.println("Same X");
					localParentFaceAxis = vecX;
				}
			}
			if(vecY != null)
			{
				double parentPos = parentY;
				double childPos = childCenter.getY() * vecY.getY() - childBoundsInParentSpace.y;
//			    System.out.println("Comparing Y: " + parentPos + " | " + childPos);
				if(almostSame(parentPos, childPos))
				{
//			    	System.out.println("Same Y");
					if(localParentFaceAxis != null)
					{
						error("Error: Board is intersecting with parent board.");
					}
					else
					{
						localParentFaceAxis = vecY;
					}
				}
			}
			if(vecZ != null)
			{
				double parentPos = parentZ;
				double childPos = childCenter.getZ() * vecZ.getZ() - childBoundsInParentSpace.z;
//			    System.out.println("Comparing Z: " + parentPos + " | " + childPos);
				if(almostSame(parentPos, childPos))
				{
//				    System.out.println("Same Z");
					if(localParentFaceAxis != null)
					{
						error("Error: Board is intersecting with parent board.");
					}
					else
					{
						localParentFaceAxis = vecZ;
					}
				}
			}
		}
		
		if(localParentFaceAxis == null)
		{
			complexRelated = true; //No, do not use this board in good code.
			error("Error: Child board must be floating. Thus no relation.");
		}
	}
	
	public boolean isComplexRelated()
	{
		return complexRelated;
	}
	
	public Vector2 getMinimumPoint()
	{
		return new Vector2(
				childCenter.getX() - childBoundsInParentSpace.x + 0.075,
				childCenter.getZ() - childBoundsInParentSpace.z + 0.075
		);
	}
	
	public Vector2 getMaximumPoint()
	{
		return new Vector2(
				childCenter.getX() + childBoundsInParentSpace.x - 0.075,
				childCenter.getZ() + childBoundsInParentSpace.z - 0.075
		);
	}
	
	public boolean isAttachedAtSide()
	{
		return localParentFaceAxis.getY() == 0;
	}
	
	public boolean isSideX()
	{
		return localParentFaceAxis.getX() != 0;
	}
	
	public Vector3 getAttachmentNormal()
	{
		if(complexRelated)
		{
			return null;
		}
		return parent.getRotation().multiply(localParentFaceAxis); //Convert back to global space.
	}
	
	private static boolean almostSame(double a, double b)
	{
		double diff = a - b;
		//TBI: Epsilon should actually be 0.0001, but TUNG generates awesome values, so lets use something less precise.
		return diff <= 0.002 && diff >= -0.002;
	}
	
	private boolean assignChildBounds(double axisLength, Vector3 probeVector)
	{
		if(isAxis(probeVector.getX()))
		{
			if(childBoundsInParentSpace.x != null)
			{
				error("Error: X axis was already assigned: " + probeVector);
				return true;
			}
			childBoundsInParentSpace.x = axisLength;
		}
		else if(isAxis(probeVector.getY()))
		{
			if(childBoundsInParentSpace.y != null)
			{
				error("Error: Y axis was already assigned: " + probeVector);
				return true;
			}
			childBoundsInParentSpace.y = axisLength;
		}
		else if(isAxis(probeVector.getZ()))
		{
			if(childBoundsInParentSpace.z != null)
			{
				error("Error: Z axis was already assigned: " + probeVector);
				return true;
			}
			childBoundsInParentSpace.z = axisLength;
		}
		else
		{
			error("Error: Not able to find axis for: " + probeVector);
			return true;
		}
		return false;
	}
	
	private void error(String message)
	{
		new RuntimeException(message).printStackTrace(System.out);
	}
	
	private static class ThreeValues
	{
		public Double x;
		public Double y;
		public Double z;
	}
	
	private static boolean isAxis(double probeValue)
	{
		return probeValue >= 0.98 || probeValue <= -0.98;
	}
	
	private static int count(Object a, Object b, Object c)
	{
		int counter = 0;
		if(a != null)
		{
			counter++;
		}
		if(b != null)
		{
			counter++;
		}
		if(c != null)
		{
			counter++;
		}
		return counter;
	}
}

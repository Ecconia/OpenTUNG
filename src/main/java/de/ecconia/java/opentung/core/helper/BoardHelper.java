package de.ecconia.java.opentung.core.helper;

import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.util.math.Quaternion;
import de.ecconia.java.opentung.util.math.Vector3;

public class BoardHelper
{
	//This function cannot and will never be compatible with boards which are inside of each other. Aka it may fail until collision has been added.
	public static Vector3 getAttachmentNormalLocal(CompBoard parent, CompBoard child)
	{
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
		
		//Convert child board into a normalized parent space:
//		System.out.println(parent.getPosition());
//		System.out.println(child.getPosition());
//		System.out.println(child.getPosition().subtract(parent.getPosition()));
		Vector3 childCenter = parent.getRotation().inverse().multiply(child.getPosition().subtract(parent.getPosition()));
		Quaternion childAlignment = child.getRotation().multiply(parent.getRotation());
		
//		System.out.println("Child in parent space:");
//		System.out.println(" X: " + (childAlignment.inverse().multiply(Vector3.xp)));
//		System.out.println(" Y: " + (childAlignment.inverse().multiply(Vector3.yp)));
//		System.out.println(" Z: " + (childAlignment.inverse().multiply(Vector3.zp)));
//		System.out.println(" Pos: " + childCenter);
		
		double parentX = (double) parent.getX() * 0.15D;
		double parentY = 0.075D;
		double parentZ = (double) parent.getZ() * 0.15D;
		Vector3 vecX = childCenter.getX() >= parentX ? Vector3.xp : (childCenter.getX() <= -parentX ? Vector3.xn : null);
		Vector3 vecY = childCenter.getY() >= parentY ? Vector3.yp : (childCenter.getY() <= -parentY ? Vector3.yn : null);
		Vector3 vecZ = childCenter.getZ() >= parentZ ? Vector3.zp : (childCenter.getZ() <= -parentZ ? Vector3.zn : null);
		
		int count = count(vecX, vecY, vecZ);
//		System.out.println("Count: " + count);
		if(count == 1)
		{
			return vecX != null ? vecX : (vecY != null ? vecY : vecZ);
		}
		
		ThreeValues childBoundsParentSpace = new ThreeValues();
		{
			if(!assignChildBounds(childBoundsParentSpace, (double) child.getX() * 0.15D, childAlignment.inverse().multiply(Vector3.xp)))
			{
				return new Vector3(0.1, 0.1, 0.1);
			}
			if(!assignChildBounds(childBoundsParentSpace, 0.5D * 0.15D, childAlignment.inverse().multiply(Vector3.yp)))
			{
				return new Vector3(0.1, 0.1, 0.1);
			}
			if(!assignChildBounds(childBoundsParentSpace, (double) child.getZ() * 0.15D, childAlignment.inverse().multiply(Vector3.zp)))
			{
				return new Vector3(0.1, 0.1, 0.1);
			}
		}
		
//		System.out.println("Child size X: " + childBoundsParentSpace.x);
//		System.out.println("Child size Y: " + childBoundsParentSpace.y);
//		System.out.println("Child size Z: " + childBoundsParentSpace.z);
		
		Vector3 resultVector = null;
		if(vecX != null)
		{
			double parentPos = parentX;
//			System.out.println(childCenter.getX() + " " + vecX.getX() + " (" + (childCenter.getX() * vecX.getX()) + ") " + childBoundsParentSpace.x);
			double childPos = childCenter.getX() * vecX.getX() - childBoundsParentSpace.x;
//			System.out.println("Comparing X: " + parentPos + " | " + childPos);
			if(almostSame(parentPos, childPos))
			{
//				System.out.println("Same X");
				resultVector = vecX;
			}
		}
		if(vecY != null)
		{
			double parentPos = parentY;
			double childPos = childCenter.getY() * vecY.getY() - childBoundsParentSpace.y;
//			System.out.println("Comparing Y: " + parentPos + " | " + childPos);
			if(almostSame(parentPos, childPos))
			{
//				System.out.println("Same Y");
				if(resultVector != null)
				{
					System.out.println("Error: Board is intersecting with parent board.");
				}
				else
				{
					resultVector = vecY;
				}
			}
		}
		if(vecZ != null)
		{
			double parentPos = parentZ;
			double childPos = childCenter.getZ() * vecZ.getZ() - childBoundsParentSpace.z;
//			System.out.println("Comparing Z: " + parentPos + " | " + childPos);
			if(almostSame(parentPos, childPos))
			{
//				System.out.println("Same Z");
				if(resultVector != null)
				{
					System.out.println("Error: Board is intersecting with parent board.");
				}
				else
				{
					resultVector = vecZ;
				}
			}
		}
		
		if(resultVector == null)
		{
			System.out.println("Error: Child board must be floating, return nonsense.");
			resultVector = new Vector3(0.5, 0.5, 0.5);
		}
		
//		System.out.println(resultVector);
		return resultVector; //Return random garbage for now.
	}
	
	private static boolean almostSame(double a, double b)
	{
		double diff = a - b;
		//TBI: Epsilon should actually be 0.0001, but TUNG generates awesome values, so lets use something less precise.
		return diff <= 0.002 && diff >= -0.002;
	}
	
	private static boolean assignChildBounds(ThreeValues childBoundsParentSpace, double a, Vector3 probeVector)
	{
		if(isAxis(probeVector.getX()))
		{
			if(childBoundsParentSpace.x != null)
			{
				System.out.println("Error: X axis was already assigned: " + probeVector);
				return false;
			}
			childBoundsParentSpace.x = a;
		}
		else if(isAxis(probeVector.getY()))
		{
			if(childBoundsParentSpace.y != null)
			{
				System.out.println("Error: Y axis was already assigned: " + probeVector);
				return false;
			}
			childBoundsParentSpace.y = a;
		}
		else if(isAxis(probeVector.getZ()))
		{
			if(childBoundsParentSpace.z != null)
			{
				System.out.println("Error: Z axis was already assigned: " + probeVector);
				return false;
			}
			childBoundsParentSpace.z = a;
		}
		else
		{
			System.out.println("Error: Not able to find axis for: " + probeVector);
			return false;
		}
		return true;
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
	
	public static Vector3 getAttachmentNormal(CompBoard parent, CompBoard child)
	{
		Vector3 result = getAttachmentNormalLocal(parent, child);
		return parent.getRotation().multiply(result); //Convert back to global space.
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

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
//
//		System.out.println("Parent:");
//		printAlignedQuaternion(" ", parent.getRotation());
//		System.out.println(" Pos: " + parent.getPosition());
//
//		System.out.println("Child:");
//		printAlignedQuaternion(" ", child.getRotation());
//		System.out.println(" Pos: " + child.getPosition());
//		System.out.println(" Pos Rel: " + child.getPosition().subtract(parent.getPosition()));
		
		//Calculate child position and alignment in parent board space:
		childCenter = parent.getAlignmentGlobal().multiply(child.getPositionGlobal().subtract(parent.getPositionGlobal()));
		Quaternion childAlignment = child.getAlignmentGlobal().multiply(parent.getAlignmentGlobal().inverse());
		Quaternion childAlignmentInverse = childAlignment.inverse();
		
//		System.out.println();
//		System.out.println("Child in parent space:");
		//Wrong:
//		printAlignedQuaternion(" 00: ", child.getRotation().multiply(parent.getRotation()));
//		printAlignedQuaternion(" 01: ", child.getRotation().inverse().multiply(parent.getRotation()));
//		printAlignedQuaternion(" 04: ", child.getRotation().multiply(parent.getRotation()).inverse());
//		printAlignedQuaternion(" 05: ", child.getRotation().inverse().multiply(parent.getRotation()).inverse());
//		printAlignedQuaternion(" 11: ", parent.getRotation().inverse().multiply(child.getRotation()));
//		printAlignedQuaternion(" 13: ", parent.getRotation().inverse().multiply(child.getRotation().inverse()));
//		printAlignedQuaternion(" 15: ", parent.getRotation().inverse().multiply(child.getRotation()).inverse());
//		printAlignedQuaternion(" 17: ", parent.getRotation().inverse().multiply(child.getRotation().inverse()).inverse());
		//Less wrong:
//		printAlignedQuaternion(" 06: ", child.getRotation().multiply(parent.getRotation().inverse()).inverse());
//		printAlignedQuaternion(" 07: ", child.getRotation().inverse().multiply(parent.getRotation().inverse()).inverse());
//		printAlignedQuaternion(" 10: ", parent.getRotation().multiply(child.getRotation()));
//		printAlignedQuaternion(" 12: ", parent.getRotation().multiply(child.getRotation().inverse()));
		//Even less wrong:
//		printAlignedQuaternion(" 03: ", child.getRotation().inverse().multiply(parent.getRotation().inverse()));
//		printAlignedQuaternion(" 14: ", parent.getRotation().multiply(child.getRotation()).inverse());
		//Probably not wrong:
//		printAlignedQuaternion(" 02: ", child.getRotation().multiply(parent.getRotation().inverse()));
//		printAlignedQuaternion(" 16: ", parent.getRotation().multiply(child.getRotation().inverse()).inverse());
//		System.out.println(" Pos: " + (parent.getRotation().multiply(child.getPosition().subtract(parent.getPosition()))));
		//Wrong:
//		System.out.println(" Pos: " + (parent.getRotation().inverse().multiply(child.getPosition().subtract(parent.getPosition()))));
		
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
//			System.out.println("Count 1");
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
						complexRelated = true;
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
						complexRelated = true;
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
	
//	private void printAlignedQuaternion(String prefix, Quaternion q)
//	{
//		q = q.inverse();
//		Vector3 x = q.multiply(Vector3.xp);
//		Vector3 y = q.multiply(Vector3.yp);
//		Vector3 z = q.multiply(Vector3.zp);
//		String xs = resolveAxis(x);
//		String ys = resolveAxis(y);
//		String zs = resolveAxis(z);
//		if(xs == null || ys == null || zs == null)
//		{
//			System.out.println(prefix + "X: " + x);
//			System.out.println(prefix + "Y: " + y);
//			System.out.println(prefix + "Z: " + z);
//		}
//		else
//		{
//			System.out.println(prefix + "X: " + Ansi.yellow + xs + Ansi.r + " Y: " + Ansi.yellow + ys + Ansi.r + " Z: " + Ansi.yellow + zs + Ansi.r);
//		}
//	}
//
//	private String resolveAxis(Vector3 v)
//	{
//		if(v.getX() < -0.99)
//		{
//			return "-X";
//		}
//		if(v.getY() < -0.99)
//		{
//			return "-Y";
//		}
//		if(v.getZ() < -0.99)
//		{
//			return "-Z";
//		}
//		if(v.getX() > 0.99)
//		{
//			return "+X";
//		}
//		if(v.getY() > 0.99)
//		{
//			return "+Y";
//		}
//		if(v.getZ() > 0.99)
//		{
//			return "+Z";
//		}
//		return null;
//	}
	
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
		return parent.getAlignmentGlobal().inverse().multiply(localParentFaceAxis); //Convert back to global space.
	}
	
	private static boolean almostSame(double a, double b)
	{
		double diff = a - b;
		//TBI: Epsilon should actually be 0.0001, but TUNG generates awesome values, so lets use something less precise.
		// Might not always be TUNGs fault. Got difference of 0.011 for matching edge :(
		return diff <= 0.011 && diff >= -0.011;
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

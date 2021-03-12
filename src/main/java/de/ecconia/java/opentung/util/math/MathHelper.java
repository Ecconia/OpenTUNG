package de.ecconia.java.opentung.util.math;

import de.ecconia.java.opentung.util.Ansi;

public class MathHelper
{
	public static Quaternion rotationFromVectors(Vector3 origin, Vector3 target)
	{
		Vector3 crossProduct = origin.cross(target);
		if(crossProduct.getX() == 0 && crossProduct.getY() == 0 && crossProduct.getZ() == 0)
		{
			if(target.equals(origin))
			{
				//Rare case 1 of "can happen". Target==Origin, no change.
				return Quaternion.angleAxis(0, Vector3.yp);
			}
			else
			{
				//Rare case 2 of "can happen". Target is exactly opposite of Origin.
				//Quaternion needs to rotate by 180 degrees. But exactly not by either of the two vectors.
				Vector3 otherVector = origin.equals(Vector3.yp) ? Vector3.xp : Vector3.yp;
				return Quaternion.angleAxis(180, origin.cross(otherVector).normalize());
			}
		}
		
		double angle = angleFromVectors(origin, target);
		return Quaternion.angleAxis(-angle, crossProduct.normalize());
	}
	
	//Basically a copy of above, but just returning the angles. Used for correcting rotations of components.
	public static double angleFromVectors(Vector3 origin, Vector3 target)
	{
		double dot = origin.dot(target);
		//Handle floating-point errors which happen every now and then:
		if(dot > 1)
		{
			if(dot > 1.000000000000001)
			{
				error(dot, origin, target);
			}
			dot = 1;
		}
		else if(dot < -1)
		{
			if(dot < -1.000000000000001)
			{
				error(dot, origin, target);
			}
			dot = -1;
		}
		
		double value = Math.toDegrees(Math.acos(dot));
		//In case an error happened anyway...
		if(Double.isNaN(value))
		{
			error(value, origin, target);
		}
		return value;
	}
	
	private static void error(double value, Vector3 val1, Vector3 val2)
	{
		System.out.println(Ansi.red + "[ERROR] Value outside of accepted bounds or NaN while calculating angle: " + val1.dot(val2) + " " + (value) + Ansi.r);
		System.out.println(" Vertices: [" + val1.getX() + ", " + val1.getY() + ", " + val1.getZ() + "] [" + val2.getX() + ", " + val2.getY() + ", " + val2.getZ() + "]");
		System.out.println(" Stacktrace:");
		new RuntimeException().printStackTrace(System.out);
	}
}

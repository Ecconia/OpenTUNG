package de.ecconia.java.opentung.math;

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
				return Quaternion.angleAxis(180, origin.cross(otherVector));
			}
		}
		
		double angle = Math.toDegrees(Math.acos(origin.dot(target)));
		return Quaternion.angleAxis(-angle, crossProduct.normalize());
	}
}

package de.ecconia.java.opentung.math;

public class Quaternion
{
	public static final Quaternion xp90 = Quaternion.angleAxis(90, Vector3.xp);
	public static final Quaternion xp180 = Quaternion.angleAxis(180, Vector3.xp);
	public static final Quaternion xp270 = Quaternion.angleAxis(270, Vector3.xp);
	public static final Quaternion yp90 = Quaternion.angleAxis(90, Vector3.yp);
	public static final Quaternion yp180 = Quaternion.angleAxis(180, Vector3.yp);
	public static final Quaternion yp270 = Quaternion.angleAxis(270, Vector3.yp);
	public static final Quaternion zp90 = Quaternion.angleAxis(90, Vector3.zp);
	public static final Quaternion zp180 = Quaternion.angleAxis(180, Vector3.zp);
	public static final Quaternion zp270 = Quaternion.angleAxis(270, Vector3.zp);
	public static final Quaternion xn90 = Quaternion.angleAxis(90, Vector3.xn);
	public static final Quaternion xn180 = Quaternion.angleAxis(180, Vector3.xn);
	public static final Quaternion xn270 = Quaternion.angleAxis(270, Vector3.xn);
	public static final Quaternion yn90 = Quaternion.angleAxis(90, Vector3.yn);
	public static final Quaternion yn180 = Quaternion.angleAxis(180, Vector3.yn);
	public static final Quaternion yn270 = Quaternion.angleAxis(270, Vector3.yn);
	public static final Quaternion zn90 = Quaternion.angleAxis(90, Vector3.zn);
	public static final Quaternion zn180 = Quaternion.angleAxis(180, Vector3.zn);
	public static final Quaternion zn270 = Quaternion.angleAxis(270, Vector3.zn);
	
	private final Vector3 v;
	private final float a;
	
	public static Quaternion angleAxis(float angle, Vector3 axis)
	{
		angle = angle / 360f * (float) Math.PI;
		return new Quaternion(
				(float) Math.cos(angle),
				axis.multiply((float) Math.sin(angle))
		);
	}
	
	public Quaternion(float a, Vector3 n)
	{
		this.a = a;
		this.v = n;
	}
	
	public Quaternion inverse()
	{
		return new Quaternion(a, v.invert());
	}
	
	public Quaternion multiply(Quaternion that)
	{
		return new Quaternion(
				this.a * that.a
						+ this.v.dot(that.v),
				this.v.multiply(that.a)
						.add(that.v.multiply(this.a))
						.add(this.v.cross(that.v))
		);
	}
	
	//For vector rotation:
	public Vector3 multiply(Vector3 that)
	{
		//return (this * new Quaternion(0, that) * this.invert()).v;
		//Optimized:
		Vector3 cross = this.v.cross(that);
		return that
				.add(cross.multiply(2f * this.a))
				.add(this.v.cross(cross).multiply(2f));
	}
	
	public float[] createMatrix()
	{
		float[] m = new float[16];

		float x = v.getX();
		float y = v.getY();
		float z = v.getZ();
		
		float xx = x * x;
		float xy = x * y;
		float xz = x * z;
		float xw = x * a;
		float yy = y * y;
		float yz = y * z;
		float yw = y * a;
		float zz = z * z;
		float zw = z * a;

		m[0] = 1 - 2 * (yy + zz);
		m[1] = 2 * (xy - zw);
		m[2] = 2 * (xz + yw);

		m[4] = 2 * (xy + zw);
		m[5] = 1 - 2 * (xx + zz);
		m[6] = 2 * (yz - xw);

		m[8] = 2 * (xz - yw);
		m[9] = 2 * (yz + xw);
		m[10] = 1 - 2 * (xx + yy);

		//Outer rect:
		m[3] = m[7] = m[11] = m[12] = m[13] = m[14] = 0;
		m[15] = 1;

		return m;
	}
}

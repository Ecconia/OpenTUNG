package de.ecconia.java.opentung.util.math;

import de.ecconia.java.opentung.util.Ansi;

public class Quaternion
{
	public static final Quaternion zero = Quaternion.angleAxis(0, Vector3.yp);
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
	private final double a;
	
	public static Quaternion angleAxis(double angle, Vector3 axis)
	{
		angle = angle * Math.PI / 360D;
		return new Quaternion(
				Math.cos(angle),
				axis.multiply(Math.sin(angle))
		);
	}
	
	public Quaternion(double a, Vector3 n)
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
						- this.v.dot(that.v),
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
		
		float x = (float) v.getX();
		float y = (float) v.getY();
		float z = (float) v.getZ();
		
		float xx = x * x;
		float xy = x * y;
		float xz = x * z;
		float xw = x * (float) a;
		float yy = y * y;
		float yz = y * z;
		float yw = y * (float) a;
		float zz = z * z;
		float zw = z * (float) a;
		
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
	
	public double getLength()
	{
		return Math.sqrt(a * a + v.lengthSquared());
	}
	
	@Override
	public String toString()
	{
		return "Q[X: " + fix(v.getX()) + " Y: " + fix(v.getY()) + " Z: " + fix(v.getZ()) + " W: " + fix(a) + " | X: " + multiply(Vector3.xp) + " Y: " + multiply(Vector3.yp) + " Z: " + multiply(Vector3.zp) + "]";
	}
	
	public void debug()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Quaternion:\n");
		//General:
		double length = getLength();
		sb.append(" Length: ").append(length == 1.0 ? Ansi.green : Ansi.red).append(length).append(Ansi.r).append(" (==1?)\n");
		//Angle:
		sb.append(" Angle:\n");
		sb.append("  A: ").append(a).append('\n');
		double angleHalf = Math.acos(a);
		double angle = angleHalf * 2.0;
		sb.append("  Rad: ").append(angle).append(" Deg: ").append(Math.toDegrees(angle)).append('\n');
		
		sb.append(" Vector:\n");
		sb.append("  X: ").append(v.getX()).append('\n');
		sb.append("  Y: ").append(v.getY()).append('\n');
		sb.append("  Z: ").append(v.getZ()).append('\n');
		sb.append("  L: ").append(v.length()).append('\n');
		
		double divisor = Math.sin(angleHalf);
		if(divisor != 0) //Can be 0, if angle is 0° or 180°.
		{
			sb.append(" Rotation axis:\n");
			Vector3 vector = v.divide(divisor);
			sb.append("  X: ").append(vector.getX()).append('\n');
			sb.append("  Y: ").append(vector.getY()).append('\n');
			sb.append("  Z: ").append(vector.getZ()).append('\n');
			sb.append("  L: ").append(vector.length()).append('\n');
		}
		
		sb.setLength(sb.length() - 1); //Remove last '\n'
		System.out.println(sb);
	}
	
	private String fix(double value)
	{
		double a = Math.abs(value);
		if(a == 0.0)
		{
			return " 0.0";
		}
		if(a < 0.00000000000001D)
		{
			return "~0.?";
		}
		else
		{
			String s = String.valueOf(value);
			if(s.lastIndexOf('E') == -1)
			{
				if(s.length() > 8)
				{
					return s.substring(0, 8);
				}
				else
				{
					return s;
				}
			}
			else
			{
				return s;
			}
		}
	}
	
	public Quaternion normalize()
	{
		double lengthSquared = a * a + v.lengthSquared();
		if(lengthSquared == 1.0)
		{
			return this;
		}
		double length = Math.sqrt(lengthSquared);
		double inv = 1.0 / length;
		return new Quaternion(
				Vector3.round(a * inv),
				new Vector3(
						Vector3.round(v.getX() * inv),
						Vector3.round(v.getY() * inv),
						Vector3.round(v.getZ() * inv)
				)
		);
	}
	
	public Vector3 getV()
	{
		return v;
	}
	
	public double getA()
	{
		return a;
	}
}

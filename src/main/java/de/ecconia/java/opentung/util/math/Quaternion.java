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
	
	public Quaternion(Vector3 n, double a)
	{
		this.a = a;
		this.v = n;
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
	
	public String griddyDebug()
	{
		Vector3 toX = multiply(Vector3.xp);
		Vector3 toY = multiply(Vector3.yp);
		Vector3 toZ = multiply(Vector3.zp);
		
		StringBuilder sb = new StringBuilder();
		
		griddyDebugHelper(sb, toX, 1);
		sb.append(" | ");
		griddyDebugHelper(sb, toY, 2);
		sb.append(" | ");
		griddyDebugHelper(sb, toZ, 3);
		
		return sb.toString();
	}
	
	private void griddyDebugHelper(StringBuilder sb, Vector3 vec, int axis)
	{
		double x = vec.getX();
		double y = vec.getY();
		double z = vec.getZ();
		
		if(x < 0.00001 && x > -0.00001)
		{
			x = 0;
		}
		else if(x > 0.9999)
		{
			x = 1;
		}
		else if(x < -0.9999)
		{
			x = -1;
		}
		
		if(y < 0.00001 && y > -0.00001)
		{
			y = 0;
		}
		else if(y > 0.9999)
		{
			y = 1;
		}
		else if(y < -0.9999)
		{
			y = -1;
		}
		
		if(z < 0.00001 && z > -0.00001)
		{
			z = 0;
		}
		else if(z > 0.9999)
		{
			z = 1;
		}
		else if(z < -0.9999)
		{
			z = -1;
		}
		
		boolean found = false;
//		if(x == 1 || x == -1)
//		{
//			if(y == 0 && z == 0)
//			{
//				found = true;
//				if(axis == 1 && x > 0) //Matching
//				{
//					sb.append("\033[38;2;0;255;0mX\033[m");
//				}
//				else //Converted.
//				{
//					sb.append("\033[38;2;255;200;0mX -> ");
//					if(x < 0)
//					{
//						sb.append('-');
//					}
//					sb.append(numberToAxis(axis)).append("\033[m");
//				}
//			}
//		}
//		else if(y == 1 || y == -1)
//		{
//			if(x == 0 && z == 0)
//			{
//				found = true;
//				if(axis == 2 && y > 0) //Matching
//				{
//					sb.append("\033[38;2;0;255;0mY\033[m");
//				}
//				else //Converted.
//				{
//					sb.append("\033[38;2;255;200;0mY -> ");
//					if(y < 0)
//					{
//						sb.append('-');
//					}
//					sb.append(numberToAxis(axis)).append("\033[m");
//				}
//			}
//		}
//		else if(z == 1 || z == -1)
//		{
//			if(x == 0 && y == 0)
//			{
//				found = true;
//				if(axis == 3 && z > 0) //Matching
//				{
//					sb.append("\033[38;2;0;255;0mZ\033[m");
//				}
//				else //Converted.
//				{
//					sb.append("\033[38;2;255;200;0mZ -> ");
//					if(z < 0)
//					{
//						sb.append('-');
//					}
//					sb.append(numberToAxis(axis)).append("\033[m");
//				}
//			}
//		}
//
//		sb.append(' ');
		
		found = false;
		if(x == 1 || x == -1)
		{
			if(y == 0 && z == 0)
			{
				found = true;
				if(axis == 1 && x > 0) //Matching
				{
					sb.append("\033[38;2;0;255;0mX\033[m");
				}
				else //Converted.
				{
					sb.append("\033[38;2;255;200;0m");
					sb.append(numberToAxis(axis));
					sb.append(" -> ");
					if(x < 0)
					{
						sb.append('-');
					}
					sb.append('X');
					sb.append("\033[m");
				}
			}
		}
		else if(y == 1 || y == -1)
		{
			if(x == 0 && z == 0)
			{
				found = true;
				if(axis == 2 && y > 0) //Matching
				{
					sb.append("\033[38;2;0;255;0mY\033[m");
				}
				else //Converted.
				{
					sb.append("\033[38;2;255;200;0m");
					sb.append(numberToAxis(axis));
					sb.append(" -> ");
					if(y < 0)
					{
						sb.append('-');
					}
					sb.append('Y');
					sb.append("\033[m");
				}
			}
		}
		else if(z == 1 || z == -1)
		{
			if(x == 0 && y == 0)
			{
				found = true;
				if(axis == 3 && z > 0) //Matching
				{
					sb.append("\033[38;2;0;255;0mZ\033[m");
				}
				else //Converted.
				{
					sb.append("\033[38;2;255;200;0m");
					sb.append(numberToAxis(axis));
					sb.append(" -> ");
					if(z < 0)
					{
						sb.append('-');
					}
					sb.append('Z');
					sb.append("\033[m");
				}
			}
		}

		if(!found)
		{
			sb.append('[')
					.append(x).append(',').append(' ')
					.append(y).append(',').append(' ')
					.append(z).append("] -> ").append(numberToAxis(axis));
		}
	}
	
	private String numberToAxis(int number)
	{
		return number == 1 ? "X" : number == 2 ? "Y" : "Z";
	}
}

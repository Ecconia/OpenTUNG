package de.ecconia.java.opentung.math;

public class Vector3
{
	public static final Vector3 xp = new Vector3(1, 0, 0);
	public static final Vector3 xn = new Vector3(-1, 0, 0);
	public static final Vector3 yp = new Vector3(0, 1, 0);
	public static final Vector3 yn = new Vector3(0, -1, 0);
	public static final Vector3 zp = new Vector3(0, 0, 1);
	public static final Vector3 zn = new Vector3(0, 0, -1);
	public static final Vector3 zero = new Vector3(0, 0, 0);
	
	private final double x;
	private final double y;
	private final double z;
	
	public Vector3(double x, double y, double z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Vector3 multiply(double factor)
	{
		return new Vector3(x * factor, y * factor, z * factor);
	}
	
	public Vector3 divide(double divisor)
	{
		return new Vector3(x / divisor, y / divisor, z / divisor);
	}
	
	public Vector3 cross(Vector3 that)
	{
		return new Vector3(
				this.y * that.z - this.z * that.y,
				this.z * that.x - this.x * that.z,
				this.x * that.y - this.y * that.x
		);
	}
	
	public Vector3 subtract(Vector3 that)
	{
		return new Vector3(
				this.x - that.x,
				this.y - that.y,
				this.z - that.z
		);
	}
	
	public Vector3 add(Vector3 that)
	{
		return new Vector3(
				this.x + that.x,
				this.y + that.y,
				this.z + that.z
		);
	}
	
	public Vector3 add(double x, double y, double z)
	{
		return new Vector3(
				this.x + x,
				this.y + y,
				this.z + z
		);
	}
	
	public Vector3 normalize()
	{
		return divide(length());
	}
	
	public Vector3 invert()
	{
		return new Vector3(-x, -y, -z);
	}
	
	public double dot(Vector3 that)
	{
		return this.x * that.x + this.y * that.y + this.z * that.z;
	}
	
	public double length()
	{
		return Math.sqrt(x * x + y * y + z * z);
	}
	
	public double lengthSqared()
	{
		return x * x + y * y + z * z;
	}
	
	public double getX()
	{
		return x;
	}
	
	public double getY()
	{
		return y;
	}
	
	public double getZ()
	{
		return z;
	}
	
	@Override
	public String toString()
	{
		return "[X: " + fix(x) + " Y: " + fix(y) + " Z: " + fix(z) + ']';
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
}

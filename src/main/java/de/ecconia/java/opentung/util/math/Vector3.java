package de.ecconia.java.opentung.util.math;

import java.util.Objects;

import de.ecconia.java.opentung.util.Ansi;

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
		double length = length();
		if(length == 1.0)
		{
			return this;
		}
		return new Vector3(
				round(x / length),
				round(y / length),
				round(z / length)
		);
	}
	
	public static double round(double in)
	{
		//TBI: This is quite ugly, but the only thing which currently helps (quick and dirty workaround).
		//Actually needed, cause something divided by something else cannot be zero, but something very small only.
		double delta = 0.000000000000001D;
		if(in < delta && in > -delta)
		{
			return 0;
		}
		else
		{
			return in;
		}
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
	
	public double lengthSquared()
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
	
	public Vector3 addX(double x)
	{
		return new Vector3(this.x + x, this.y, this.z);
	}
	
	public Vector3 addY(double y)
	{
		return new Vector3(this.x, this.y + y, this.z);
	}
	
	public Vector3 addZ(double z)
	{
		return new Vector3(this.x, this.y, this.z + z);
	}
	
	@Override
	public String toString()
	{
		return "[X: " + Ansi.yellow + fix(x) + Ansi.r + " Y: " + Ansi.yellow + fix(y) + Ansi.r + " Z: " + Ansi.yellow + fix(z) + Ansi.r + "]";
	}
	
	public void debug()
	{
		System.out.println("X: " + x + "\n" +
				"Y: " + y + "\n" +
				"Z: " + z + "\n" +
				"Length: " + length());
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
	
	@Override
	public boolean equals(Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(o == null || getClass() != o.getClass())
		{
			return false;
		}
		Vector3 vector3 = (Vector3) o;
		return Double.compare(vector3.x, x) == 0 &&
				Double.compare(vector3.y, y) == 0 &&
				Double.compare(vector3.z, z) == 0;
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(x, y, z);
	}
	
	public boolean oneNegative()
	{
		return x < 0 || y < 0 || z < 0;
	}
	
	public Vector3 multiply(Vector3 that)
	{
		return new Vector3(
				this.x * that.x,
				this.y * that.y,
				this.z * that.z
		);
	}
	
	public static float[] toFloatArray(Vector3... vertices)
	{
		float[] array = new float[vertices.length * 3];
		int index = 0;
		for(Vector3 v : vertices)
		{
			array[index++] = (float) v.getX();
			array[index++] = (float) v.getY();
			array[index++] = (float) v.getZ();
		}
		return array;
	}
	
	public String griddyDebug()
	{
		double x = Math.round(this.x * 1000D) / 1000D;
		double y = Math.round(this.y * 1000D) / 1000D;
		double z = Math.round(this.z * 1000D) / 1000D;
		double ux = x / 0.075D;
		double uy = y / 0.075D;
		double uz = z / 0.075D;
		ux = Math.round(ux * 1000D) / 1000D;
		uy = Math.round(uy * 1000D) / 1000D;
		uz = Math.round(uz * 1000D) / 1000D;
		double uux = ux / 4D;
		double uuy = uy / 4D;
		double uuz = uz / 4D;
		uux = Math.round(uux * 1000D) / 1000D;
		uuy = Math.round(uuy * 1000D) / 1000D;
		uuz = Math.round(uuz * 1000D) / 1000D;
		return "[" + Ansi.yellow + x + Ansi.r + ", " + Ansi.yellow + y + Ansi.r + ", " + Ansi.yellow + z + Ansi.r + "] " +
				"| [" + Ansi.blue + ux + Ansi.r + ", " + Ansi.blue + uy + Ansi.r + ", " + Ansi.blue + uz + Ansi.r + "] " +
				"| [" + Ansi.orange + uux + Ansi.r + ", " + Ansi.orange + uuy + Ansi.r + ", " + Ansi.orange + uuz + Ansi.r + "]";
	}
}

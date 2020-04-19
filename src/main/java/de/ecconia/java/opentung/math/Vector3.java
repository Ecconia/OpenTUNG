package de.ecconia.java.opentung.math;

public class Vector3
{
	public static final Vector3 xp = new Vector3(1, 0, 0);
	public static final Vector3 xn = new Vector3(-1, 0, 0);
	public static final Vector3 yp = new Vector3(0, 1, 0);
	public static final Vector3 yn = new Vector3(0, -1, 0);
	public static final Vector3 zp = new Vector3(0, 0, 1);
	public static final Vector3 zn = new Vector3(0, 0, -1);
	
	private final float x;
	private final float y;
	private final float z;
	
	public Vector3(float x, float y, float z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Vector3 multiply(float factor)
	{
		return new Vector3(x * factor, y * factor, z * factor);
	}
	
	public Vector3 divide(float divisor)
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
	
	public Vector3 add(float x, float y, float z)
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
	
	public float dot(Vector3 that)
	{
		return this.x * that.x + this.y * that.y + this.z * that.z;
	}
	
	public float length()
	{
		return (float) Math.sqrt(x * x + y * y + z * z);
	}
	
	public float lengthSqared()
	{
		return x * x + y * y + z * z;
	}
	
	public float getX()
	{
		return x;
	}
	
	public float getY()
	{
		return y;
	}
	
	public float getZ()
	{
		return z;
	}
}

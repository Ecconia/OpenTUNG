package de.ecconia.java.opentung;

public class Quaternion
{
	//Identity:
	private float x = 0.5f;
	private float y = 0.5f;
	private float z = 0.2f;
	private float w = 5.0f;
	
	public Quaternion()
	{
//		normalize();
	}
	
	public float getLength()
	{
		return (float) Math.sqrt(
				x * x +
				y * y +
				z * z +
				w * w);
	}
	
	public void normalize()
	{
		float length = getLength();
		x /= length;
		y /= length;
		z /= length;
		w /= length;
	}
	
	public float[] createMatrix()
	{
		float[] m = new float[16];
		
		float xx = x * x;
		float xy = x * y;
		float xz = x * z;
		float xw = x * w;
		float yy = y * y;
		float yz = y * z;
		float yw = y * w;
		float zz = z * z;
		float zw = z * w;
		
		m[0] = 1 - 2 * (yy + zz);
		m[1] = 2 * (xy - zw);
		m[2] = 2 * (xz + yw);
		
		m[4] = 2 * (xy + zw);
		m[5] = 1 - 2 * (xx + zz);
		m[6] = 2 * (yz - xw);
		
		m[8] = 2 * (xz - yw);
		m[9] = 2 * (yz + xw);
		m[10] = 1 - 2 * (xx + yy);
		
		m[3] = m[7] = m[11] = m[12] = m[13] = m[14] = 0;
		m[15] = 1;
		
		return m;
	}
}

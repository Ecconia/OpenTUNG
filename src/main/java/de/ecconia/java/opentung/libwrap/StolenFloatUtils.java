package de.ecconia.java.opentung.libwrap;

public class StolenFloatUtils
{
	public static final float PI = 3.14159265358979323846f;
	public static final float EPSILON = 1.1920929E-7f;
	
	public static float sqrt(final float a)
	{
		return (float) java.lang.Math.sqrt(a);
	}
	
	public static boolean isZero(final float a, final float epsilon)
	{
		return Math.abs(a) < epsilon;
	}
	
	public static float[] makePerspective(final float[] m, final int m_off, final boolean initM,
	                                      final float fovy_rad, final float aspect, final float zNear, final float zFar)
	{
		final float top = tan(fovy_rad / 2f) * zNear; // use tangent of half-fov !
		final float bottom = -1.0f * top;
		final float left = aspect * bottom;
		final float right = aspect * top;
		return makeFrustum(m, m_off, initM, left, right, bottom, top, zNear, zFar);
	}
	
	public static float normSquareVec3(final float[] vec)
	{
		return vec[0] * vec[0] + vec[1] * vec[1] + vec[2] * vec[2];
	}
	
	public static float[] normalizeVec3(final float[] vector)
	{
		final float lengthSq = normSquareVec3(vector);
		if(isZero(lengthSq, EPSILON))
		{
			vector[0] = 0f;
			vector[1] = 0f;
			vector[2] = 0f;
		}
		else
		{
			final float invSqr = 1f / sqrt(lengthSq);
			vector[0] *= invSqr;
			vector[1] *= invSqr;
			vector[2] *= invSqr;
		}
		return vector;
	}
	
	public static float[] makeFrustum(final float[] m, final int m_offset, final boolean initM,
	                                  final float left, final float right,
	                                  final float bottom, final float top,
	                                  final float zNear, final float zFar)
	{
		if(zNear <= 0.0f || zFar < 0.0f)
		{
			throw new RuntimeException("GL_INVALID_VALUE: zNear and zFar must be positive, and zNear>0");
		}
		if(left == right || top == bottom || zNear == zFar)
		{
			throw new RuntimeException("GL_INVALID_VALUE: top,bottom and left,right and zNear,zFar must not be equal");
		}
		if(initM)
		{
			// m[m_offset+0+4*0] = 1f;
			m[m_offset + 1 + 4 * 0] = 0f;
			m[m_offset + 2 + 4 * 0] = 0f;
			m[m_offset + 3 + 4 * 0] = 0f;
			
			m[m_offset + 0 + 4 * 1] = 0f;
			// m[m_offset+1+4*1] = 1f;
			m[m_offset + 2 + 4 * 1] = 0f;
			m[m_offset + 3 + 4 * 1] = 0f;
			
			// m[m_offset+0+4*2] = 0f;
			// m[m_offset+1+4*2] = 0f;
			// m[m_offset+2+4*2] = 1f;
			// m[m_offset+3+4*2] = 0f;
			
			m[m_offset + 0 + 4 * 3] = 0f;
			m[m_offset + 1 + 4 * 3] = 0f;
			// m[m_offset+2+4*3] = 0f;
			// m[m_offset+3+4*3] = 1f;
		}
		final float zNear2 = 2.0f * zNear;
		final float dx = right - left;
		final float dy = top - bottom;
		final float dz = zFar - zNear;
		final float A = (right + left) / dx;
		final float B = (top + bottom) / dy;
		final float C = -1.0f * (zFar + zNear) / dz;
		final float D = -2.0f * (zFar * zNear) / dz;
		
		m[m_offset + 0 + 4 * 0] = zNear2 / dx;
		
		m[m_offset + 1 + 4 * 1] = zNear2 / dy;
		
		m[m_offset + 0 + 4 * 2] = A;
		m[m_offset + 1 + 4 * 2] = B;
		m[m_offset + 2 + 4 * 2] = C;
		m[m_offset + 3 + 4 * 2] = -1.0f;
		
		m[m_offset + 2 + 4 * 3] = D;
		m[m_offset + 3 + 4 * 3] = 0f;
		
		return m;
	}
	
	public static float[] makeIdentity(final float[] m)
	{
		m[0 + 4 * 0] = 1f;
		m[1 + 4 * 0] = 0f;
		m[2 + 4 * 0] = 0f;
		m[3 + 4 * 0] = 0f;
		
		m[0 + 4 * 1] = 0f;
		m[1 + 4 * 1] = 1f;
		m[2 + 4 * 1] = 0f;
		m[3 + 4 * 1] = 0f;
		
		m[0 + 4 * 2] = 0f;
		m[1 + 4 * 2] = 0f;
		m[2 + 4 * 2] = 1f;
		m[3 + 4 * 2] = 0f;
		
		m[0 + 4 * 3] = 0f;
		m[1 + 4 * 3] = 0f;
		m[2 + 4 * 3] = 0f;
		m[3 + 4 * 3] = 1f;
		return m;
	}
	
	public static float[] multMatrix(final float[] a, final float[] b)
	{
		final float b00 = b[0 + 0 * 4];
		final float b10 = b[1 + 0 * 4];
		final float b20 = b[2 + 0 * 4];
		final float b30 = b[3 + 0 * 4];
		final float b01 = b[0 + 1 * 4];
		final float b11 = b[1 + 1 * 4];
		final float b21 = b[2 + 1 * 4];
		final float b31 = b[3 + 1 * 4];
		final float b02 = b[0 + 2 * 4];
		final float b12 = b[1 + 2 * 4];
		final float b22 = b[2 + 2 * 4];
		final float b32 = b[3 + 2 * 4];
		final float b03 = b[0 + 3 * 4];
		final float b13 = b[1 + 3 * 4];
		final float b23 = b[2 + 3 * 4];
		final float b33 = b[3 + 3 * 4];
		
		float ai0 = a[0 * 4]; // row-0 of a
		float ai1 = a[1 * 4];
		float ai2 = a[2 * 4];
		float ai3 = a[3 * 4];
		a[0 * 4] = ai0 * b00 + ai1 * b10 + ai2 * b20 + ai3 * b30;
		a[1 * 4] = ai0 * b01 + ai1 * b11 + ai2 * b21 + ai3 * b31;
		a[2 * 4] = ai0 * b02 + ai1 * b12 + ai2 * b22 + ai3 * b32;
		a[3 * 4] = ai0 * b03 + ai1 * b13 + ai2 * b23 + ai3 * b33;
		
		ai0 = a[1 + 0 * 4]; // row-1 of a
		ai1 = a[1 + 1 * 4];
		ai2 = a[1 + 2 * 4];
		ai3 = a[1 + 3 * 4];
		a[1 + 0 * 4] = ai0 * b00 + ai1 * b10 + ai2 * b20 + ai3 * b30;
		a[1 + 1 * 4] = ai0 * b01 + ai1 * b11 + ai2 * b21 + ai3 * b31;
		a[1 + 2 * 4] = ai0 * b02 + ai1 * b12 + ai2 * b22 + ai3 * b32;
		a[1 + 3 * 4] = ai0 * b03 + ai1 * b13 + ai2 * b23 + ai3 * b33;
		
		ai0 = a[2 + 0 * 4]; // row-2 of a
		ai1 = a[2 + 1 * 4];
		ai2 = a[2 + 2 * 4];
		ai3 = a[2 + 3 * 4];
		a[2 + 0 * 4] = ai0 * b00 + ai1 * b10 + ai2 * b20 + ai3 * b30;
		a[2 + 1 * 4] = ai0 * b01 + ai1 * b11 + ai2 * b21 + ai3 * b31;
		a[2 + 2 * 4] = ai0 * b02 + ai1 * b12 + ai2 * b22 + ai3 * b32;
		a[2 + 3 * 4] = ai0 * b03 + ai1 * b13 + ai2 * b23 + ai3 * b33;
		
		ai0 = a[3 + 0 * 4]; // row-3 of a
		ai1 = a[3 + 1 * 4];
		ai2 = a[3 + 2 * 4];
		ai3 = a[3 + 3 * 4];
		a[3 + 0 * 4] = ai0 * b00 + ai1 * b10 + ai2 * b20 + ai3 * b30;
		a[3 + 1 * 4] = ai0 * b01 + ai1 * b11 + ai2 * b21 + ai3 * b31;
		a[3 + 2 * 4] = ai0 * b02 + ai1 * b12 + ai2 * b22 + ai3 * b32;
		a[3 + 3 * 4] = ai0 * b03 + ai1 * b13 + ai2 * b23 + ai3 * b33;
		
		return a;
	}
	
	public static float tan(final float a)
	{
		return (float) java.lang.Math.tan(a);
	}
}

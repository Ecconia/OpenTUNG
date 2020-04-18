package de.ecconia.java.opentung.libwrap;

public class Matrix
{
	private float[] mat = new float[16];
	private float[] tmpMat = new float[16];
	private float[] tmpVec = new float[3];
	
	public Matrix()
	{
		identity();
	}
	
	public float[] getMat()
	{
		return mat;
	}
	
	public void identity()
	{
		StolenFloatUtils.makeIdentity(mat);
	}
	
	public void translate(float x, float y, float z)
	{
		StolenFloatUtils.makeIdentity(tmpMat);
		tmpMat[12] = x;
		tmpMat[13] = y;
		tmpMat[14] = z;
		StolenFloatUtils.multMatrix(mat, tmpMat);
	}
	
	public void scale(float x, float y, float z)
	{
		StolenFloatUtils.makeIdentity(tmpMat);
		tmpMat[0] = x;
		tmpMat[5] = y;
		tmpMat[10] = z;
		StolenFloatUtils.multMatrix(mat, tmpMat);
	}
	
	public void rotate(final float angle, float x, float y, float z)
	{
		StolenFloatUtils.multMatrix(mat, makeRotationAxis(tmpMat, angle * StolenFloatUtils.PI / 180.0f, x, y, z, tmpVec));
	}
	
	public void perspective(float angle, float aspect, float zNear, float zFar)
	{
		identity();
		StolenFloatUtils.makePerspective(mat, 0, true, angle * StolenFloatUtils.PI / 180.0f, aspect, zNear, zFar);
		
		//TODO: Improve this:
		float[] fix = new float[]{
				1, 0, 0, 0,
				0, 1, 0, 0,
				0, 0, -1, 0,
				0, 0, 0, 1,
		};
		StolenFloatUtils.multMatrix(mat, fix);
	}
	
	public void print()
	{
		System.out.println(f(mat[0]) + f(mat[1]) + f(mat[2]) + f(mat[3]));
		System.out.println(f(mat[4]) + f(mat[5]) + f(mat[6]) + f(mat[7]));
		System.out.println(f(mat[8]) + f(mat[9]) + f(mat[10]) + f(mat[11]));
		System.out.println(f(mat[12]) + f(mat[13]) + f(mat[14]) + f(mat[15]));
	}
	
	private static String f(float f)
	{
		String v = String.valueOf(f);
		
		if(v.length() > 3)
		{
			v = v.substring(0, 3);
		}
		
		if(f >= 0)
		{
			v = ' ' + v;
		}
		
		return v;
	}
	
	//Copied from FloatUtil, slightly changed:
	
	//Removed offset.
	private static float[] makeRotationAxis(final float[] m, final float angrad, float x, float y, float z, final float[] tmpVec3f)
	{
		final float c = (float) Math.cos(angrad);
		final float ic = 1.0f - c;
		final float s = (float) Math.sin(angrad);
		
		tmpVec3f[0] = x;
		tmpVec3f[1] = y;
		tmpVec3f[2] = z;
		StolenFloatUtils.normalizeVec3(tmpVec3f);
		x = tmpVec3f[0];
		y = tmpVec3f[1];
		z = tmpVec3f[2];
		
		final float xy = x * y;
		final float xz = x * z;
		final float xs = x * s;
		final float ys = y * s;
		final float yz = y * z;
		final float zs = z * s;
		m[0 + 0 * 4] = x * x * ic + c;
		m[1 + 0 * 4] = xy * ic + zs;
		m[2 + 0 * 4] = xz * ic - ys;
		m[3 + 0 * 4] = 0;
		
		m[0 + 1 * 4] = xy * ic - zs;
		m[1 + 1 * 4] = y * y * ic + c;
		m[2 + 1 * 4] = yz * ic + xs;
		m[3 + 1 * 4] = 0;
		
		m[0 + 2 * 4] = xz * ic + ys;
		m[1 + 2 * 4] = yz * ic - xs;
		m[2 + 2 * 4] = z * z * ic + c;
		m[3 + 2 * 4] = 0;
		
		m[0 + 3 * 4] = 0f;
		m[1 + 3 * 4] = 0f;
		m[2 + 3 * 4] = 0f;
		m[3 + 3 * 4] = 1f;
		
		return m;
	}
	
	public void interfaceMatrix(int width, int height)
	{
		mat[0 + 0 * 4] = 2.0f / width;
		mat[1 + 0 * 4] = 0.0f;
		mat[2 + 0 * 4] = 0.0f;
		mat[3 + 0 * 4] = 0.0f;
		
		mat[0 + 1 * 4] = 0.0f;
		mat[1 + 1 * 4] = -2.0f / height;
		mat[2 + 1 * 4] = 0.0f;
		mat[3 + 1 * 4] = 0.0f;
		
		mat[0 + 2 * 4] = 0.0f;
		mat[1 + 2 * 4] = 0.0f;
		mat[2 + 2 * 4] = 1.0f;
		mat[3 + 2 * 4] = 0.0f;
		
		mat[0 + 3 * 4] = -1.0f;
		mat[1 + 3 * 4] = 1.0f;
		mat[2 + 3 * 4] = 0.0f;
		mat[3 + 3 * 4] = 1.0f;
	}

//	public void multiplyLeft(Matrix matrix)
//	{
//		mat = StolenFloatUtils.multMatrix(matrix.getMat(), mat);
//	}
//
//	public void multiplyRight(Matrix matrix)
//	{
//		mat = StolenFloatUtils.multMatrix(mat, matrix.getMat());
//	}
}

package de.ecconia.java.opentung.libwrap;

import de.ecconia.java.opentung.math.Vector2;
import de.ecconia.java.opentung.math.Vector3;

public class Matrix
{
	private float[] mat = new float[16];
	private float[] tmpMat = new float[16];
	private float[] tmpVec = new float[3];
	
	public Matrix()
	{
		identity();
	}
	
	public Matrix(float[] mat)
	{
		this.mat = mat;
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
	
	public void orthoMatrix(int width, int height)
	{
		float aspect = (float) height / (float) width;
		mat[0 + 0 * 4] = aspect;
		mat[1 + 0 * 4] = 0.0f;
		mat[2 + 0 * 4] = 0.0f;
		mat[3 + 0 * 4] = 0.0f;
		
		mat[0 + 1 * 4] = 0.0f;
		mat[1 + 1 * 4] = 1.0f;
		mat[2 + 1 * 4] = 0.0f;
		mat[3 + 1 * 4] = 0.0f;
		
		mat[0 + 2 * 4] = 0.0f;
		mat[1 + 2 * 4] = 0.0f;
		mat[2 + 2 * 4] = 1.0f;
		mat[3 + 2 * 4] = 0.0f;
		
		mat[0 + 3 * 4] = 0.0f;
		mat[1 + 3 * 4] = 0.0f;
		mat[2 + 3 * 4] = 0.0f;
		mat[3 + 3 * 4] = 1.0f;
	}
	
	public void interfaceMatrix(int width, int height)
	{
		//Maps 0 to pixels to -1 to 1 for width and height. 0 is at top left.
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
	
	public void multiply(Matrix matrix)
	{
		StolenFloatUtils.multMatrix(mat, matrix.getMat());
	}
	
	public Vector2 getMapped(Vector3 modelPos)
	{
		//Performs multiplication with vector, but only considers the X result.
		
		/*
			Multiplication: x: 0
							y: 1
							z: 2
					   vT   w: 3
			x: 0  4  8 12
			y: 1  5  9 13
			z: 2  6 10 14
			w: 3  7 11 15
		 */
		
		//TBI: Handle 0? Or confident...
		double w = mat[3] * modelPos.getX() + mat[7] * modelPos.getY() + mat[11] * modelPos.getZ() + mat[15];
		return new Vector2(
				(mat[0] * modelPos.getX() + mat[4] * modelPos.getY() + mat[8] * modelPos.getZ() + mat[12]) / w,
				(mat[1] * modelPos.getX() + mat[5] * modelPos.getY() + mat[9] * modelPos.getZ() + mat[13]) / w);
	}
}

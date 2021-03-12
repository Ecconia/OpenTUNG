package de.ecconia.java.opentung.util;

import de.ecconia.java.opentung.util.math.MathHelper;
import de.ecconia.java.opentung.util.math.Quaternion;
import de.ecconia.java.opentung.util.math.Vector3;

public class FourDirections
{
	private Vector3 a;
	private Vector3 b;
	private Vector3 c;
	private Vector3 d;
	
	public FourDirections(Vector3 localNormal, Quaternion rotation)
	{
		setByNormal(localNormal);
		Quaternion iRotation = rotation.inverse();
		a = iRotation.multiply(a);
		b = iRotation.multiply(b);
		c = iRotation.multiply(c);
		d = iRotation.multiply(d);
	}
	
	private void setByNormal(Vector3 localNormal)
	{
		if(Vector3.xp.equals(localNormal) || Vector3.xn.equals(localNormal))
		{
			a = Vector3.yp;
			b = Vector3.yn;
			c = Vector3.zp;
			d = Vector3.zn;
		}
		else if(Vector3.yp.equals(localNormal) || Vector3.yn.equals(localNormal))
		{
			a = Vector3.xp;
			b = Vector3.xn;
			c = Vector3.zp;
			d = Vector3.zn;
		}
		else if(Vector3.zp.equals(localNormal) || Vector3.zn.equals(localNormal))
		{
			a = Vector3.xp;
			b = Vector3.xn;
			c = Vector3.yp;
			d = Vector3.yn;
		}
		else
		{
			System.out.println("[ERROR] Local collision normal is not one of the 6 axes. Defaulting to +X.");
			setByNormal(Vector3.xp);
		}
	}

//	public void draw(Vector3 pos, Vector3 normal)
//	{
//		ShaderProgram lineShader = shaderStorage.getLineShader();
//		lineShader.use();
//		lineShader.setUniformM4(1, view);
//		lineShader.setUniformM4(2, new Matrix().getMat());
//		GL30.glLineWidth(5f);
//		axes.draw(placementData.getPosition(), placementData.getNormal());
//		Vector3 end = pos.add(normal.multiply(0.3));
//		float[] points = Vector3.toFloatArray(
//				pos, new Vector3(1, 0, 0),
//				end.add(a.multiply(0.4)), new Vector3(1, 0, 0),
//				pos, new Vector3(0, 1, 0),
//				end.add(b.multiply(0.4)), new Vector3(0, 1, 0),
//				pos, new Vector3(0, 0, 1),
//				end.add(c.multiply(0.4)), new Vector3(0, 0, 1),
//				pos, new Vector3(1, 1, 0),
//				end.add(d.multiply(0.4)), new Vector3(1, 1, 0)
//		);
//		short[] indices = new short[]{
//				0, 1, 2, 3, 4, 5, 6, 7
//		};
//		LineVAO vao = new LineVAO(points, indices);
//		vao.use();
//		vao.draw();
//		vao.unload();
//	}
	
	public Vector3 getFitting(Vector3 fixXAxis)
	{
		double aAngle = MathHelper.angleFromVectors(a, fixXAxis);
		double bAngle = MathHelper.angleFromVectors(b, fixXAxis);
		double cAngle = MathHelper.angleFromVectors(c, fixXAxis);
		double dAngle = MathHelper.angleFromVectors(d, fixXAxis);
		
		double smallestFound = aAngle;
		boolean isAlone = true;
		Vector3 currentChoice = a;
		
		double diff = Math.abs(smallestFound - bAngle);
		if(diff < 0.00001)
		{
			isAlone = false;
		}
		else
		{
			if(bAngle < smallestFound)
			{
				smallestFound = bAngle;
				currentChoice = b;
				isAlone = true;
			}
		}
		
		diff = Math.abs(smallestFound - cAngle);
		if(diff < 0.00001)
		{
			isAlone = false;
		}
		else
		{
			if(cAngle < smallestFound)
			{
				smallestFound = cAngle;
				currentChoice = c;
				isAlone = true;
			}
		}
		
		diff = Math.abs(smallestFound - dAngle);
		if(diff < 0.00001)
		{
			isAlone = false;
		}
		else
		{
			if(dAngle < smallestFound)
			{
				currentChoice = d;
				isAlone = true;
			}
		}
		
		if(isAlone)
		{
			return currentChoice;
		}
		else
		{
			return null;
		}
	}
	
	public Vector3 getA()
	{
		return a;
	}
	
	@Override
	public String toString()
	{
		return "A: " + a + "\nB: " + b + "\nC: " + c + "\nD: " + d;
	}
}

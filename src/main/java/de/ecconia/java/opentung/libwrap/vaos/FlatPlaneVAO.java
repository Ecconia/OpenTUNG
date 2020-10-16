package de.ecconia.java.opentung.libwrap.vaos;

import org.lwjgl.opengl.GL30;

public class FlatPlaneVAO extends GenericVAO
{
	public FlatPlaneVAO(float[] vertices, short[] indices)
	{
		super(vertices, indices);
	}
	
	public static FlatPlaneVAO generateFullCanvasPlane()
	{
		return new FlatPlaneVAO(new float[]{
				-1, -1,
				+1, -1,
				+1, +1,
				-1, +1
		}, new short[]{
				0, 1, 2,
				0, 2, 3
		});
	}
	
	@Override
	protected void init()
	{
		//Position:
		GL30.glVertexAttribPointer(0, 2, GL30.GL_FLOAT, false, 2 * Float.BYTES, 0);
		GL30.glEnableVertexAttribArray(0);
	}
}

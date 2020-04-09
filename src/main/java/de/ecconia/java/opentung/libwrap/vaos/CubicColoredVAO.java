package de.ecconia.java.opentung.libwrap.vaos;

import org.lwjgl.opengl.GL30;

public class CubicColoredVAO extends GenericVAO
{
	public CubicColoredVAO(float[] vertices, short[] indices)
	{
		super(vertices, indices);
	}
	
	@Override
	protected void init()
	{
		//Position:
		GL30.glVertexAttribPointer(0, 3, GL30.GL_FLOAT, false, 9 * Float.BYTES, 0);
		GL30.glEnableVertexAttribArray(0);
		//Color:
		GL30.glVertexAttribPointer(1, 3, GL30.GL_FLOAT, false, 9 * Float.BYTES, 3 * Float.BYTES);
		GL30.glEnableVertexAttribArray(1);
		//Normal:
		GL30.glVertexAttribPointer(2, 3, GL30.GL_FLOAT, false, 9 * Float.BYTES, 6 * Float.BYTES);
		GL30.glEnableVertexAttribArray(2);
	}
}

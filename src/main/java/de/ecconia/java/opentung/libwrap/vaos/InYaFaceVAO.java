package de.ecconia.java.opentung.libwrap.vaos;

import org.lwjgl.opengl.GL30;

public class InYaFaceVAO extends GenericVAO
{
	public InYaFaceVAO(float[] vertices, short[] indices)
	{
		super(vertices, indices);
	}
	
	@Override
	protected void init()
	{
		//Position:
		GL30.glVertexAttribPointer(0, 2, GL30.GL_FLOAT, false, 2 * Float.BYTES, 0);
		GL30.glEnableVertexAttribArray(0);
	}
}

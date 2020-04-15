package de.ecconia.java.opentung.libwrap.vaos;

import org.lwjgl.opengl.GL30;

public class InterfaceVAO extends GenericVAO
{
	public InterfaceVAO(float[] vertices, short[] indices)
	{
		super(vertices, indices);
	}
	
	@Override
	protected void init()
	{
		//Position:
		GL30.glVertexAttribPointer(0, 2, GL30.GL_FLOAT, false, 5 * Float.BYTES, 0);
		GL30.glEnableVertexAttribArray(0);
		//Color:
		GL30.glVertexAttribPointer(1, 3, GL30.GL_FLOAT, false, 5 * Float.BYTES, 2 * Float.BYTES);
		GL30.glEnableVertexAttribArray(1);
	}
}

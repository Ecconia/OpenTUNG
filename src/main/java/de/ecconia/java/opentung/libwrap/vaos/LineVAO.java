package de.ecconia.java.opentung.libwrap.vaos;

import org.lwjgl.opengl.GL30;

public class LineVAO extends GenericVAO
{
	public LineVAO(float[] vertices, short[] indices)
	{
		super(vertices, indices);
	}
	
	@Override
	protected void init()
	{
		//Position:
		GL30.glVertexAttribPointer(0, 3, GL30.GL_FLOAT, false, 6 * Float.BYTES, 0);
		GL30.glEnableVertexAttribArray(0);
		//Color:
		GL30.glVertexAttribPointer(1, 3, GL30.GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES);
		GL30.glEnableVertexAttribArray(1);
	}
	
	@Override
	public void draw()
	{
		GL30.glDrawElements(GL30.GL_LINES, amount, GL30.GL_UNSIGNED_SHORT, 0);
	}
}

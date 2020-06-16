package de.ecconia.java.opentung.libwrap.vaos;

import org.lwjgl.opengl.GL30;

public abstract class LargeGenericVAO extends GenericVAO
{
	protected LargeGenericVAO(float[] vertices, int[] indices)
	{
		super(vertices, indices);
	}
	
	protected abstract void init();
	
	public void draw()
	{
		GL30.glDrawElements(GL30.GL_TRIANGLES, amount, GL30.GL_UNSIGNED_INT, 0);
	}
}

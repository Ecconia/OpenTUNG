package de.ecconia.java.opentung.libwrap.vaos;

import org.lwjgl.opengl.GL30;

public abstract class LargeGenericVAO extends GenericVAO
{
	protected LargeGenericVAO(float[] vertices, int[] indices, Object... objects)
	{
		super(vertices, indices, objects);
	}
	
	protected abstract void init();
	
	public void draw()
	{
		GL30.glDrawElements(GL30.GL_TRIANGLES, amount, GL30.GL_UNSIGNED_INT, 0);
	}
}

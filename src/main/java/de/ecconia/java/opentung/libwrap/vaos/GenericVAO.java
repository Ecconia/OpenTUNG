package de.ecconia.java.opentung.libwrap.vaos;

import org.lwjgl.opengl.GL30;

public abstract class GenericVAO
{
	private final int vaoID;
	protected final int amount;
	
	protected GenericVAO(float[] vertices, short[] indices)
	{
		vaoID = GL30.glGenVertexArrays();
		int vboID = GL30.glGenBuffers();
		int eabID = GL30.glGenBuffers();
		
		GL30.glBindVertexArray(vaoID);
		
		GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, vboID);
		GL30.glBufferData(GL30.GL_ARRAY_BUFFER, vertices, GL30.GL_STATIC_DRAW);
		
		GL30.glBindBuffer(GL30.GL_ELEMENT_ARRAY_BUFFER, eabID);
		amount = indices.length;
		GL30.glBufferData(GL30.GL_ELEMENT_ARRAY_BUFFER, indices, GL30.GL_STATIC_DRAW);
		
		init();
		
		//Cleanup:
		GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0);
	}
	
	protected abstract void init();
	
	public void use()
	{
		GL30.glBindVertexArray(vaoID);
	}
	
	public void draw()
	{
		GL30.glDrawElements(GL30.GL_TRIANGLES, amount, GL30.GL_UNSIGNED_SHORT, 0);
	}
	
	public void unload()
	{
		GL30.glDeleteVertexArrays(vaoID);
	}
}

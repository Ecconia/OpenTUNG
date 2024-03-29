package de.ecconia.java.opentung.libwrap.vaos;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL30;

public abstract class GenericVAO
{
	private final int vaoID;
	protected final List<Integer> deleteLater = new ArrayList<>();
	protected final int amount;
	
	protected GenericVAO(float[] vertices, int[] indices, Object... extra)
	{
		System.out.println("[MeshDebug] " + getClass().getSimpleName() + " V: " + vertices.length + " I: " + indices.length);
		vaoID = GL30.glGenVertexArrays();
		GL30.glBindVertexArray(vaoID);
		
		uploadMoreData(extra);
		
		int vboID = GL30.glGenBuffers();
		GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, vboID);
		GL30.glBufferData(GL30.GL_ARRAY_BUFFER, vertices, GL30.GL_STATIC_DRAW);
		
		int eabID = GL30.glGenBuffers();
		GL30.glBindBuffer(GL30.GL_ELEMENT_ARRAY_BUFFER, eabID);
		amount = indices.length;
		GL30.glBufferData(GL30.GL_ELEMENT_ARRAY_BUFFER, indices, GL30.GL_STATIC_DRAW);
		
		init();
		
		//Cleanup:
		GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0);
		
		deleteLater.add(vboID);
		deleteLater.add(eabID);
	}
	
	protected void uploadMoreData(Object... extra)
	{
	}
	
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
		
		for(Integer i : deleteLater)
		{
			GL30.glDeleteBuffers(i);
		}
	}
}

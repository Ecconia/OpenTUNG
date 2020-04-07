package de.ecconia.java.opentung.libwrap;

import org.lwjgl.opengl.GL30;

public class VAOWrapper
{
	private final int vaoID;
	private final int amount;
	
	public VAOWrapper(float[] vertices, short[] indices)
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
		
		//Define, how the values should be used in the shader:
		//Position data:
		GL30.glVertexAttribPointer(0, 3, GL30.GL_FLOAT, false, 9 * Float.BYTES, 0);
		GL30.glEnableVertexAttribArray(0);
		//Color data:
		GL30.glVertexAttribPointer(1, 3, GL30.GL_FLOAT, false, 9 * Float.BYTES, 3 * Float.BYTES);
		GL30.glEnableVertexAttribArray(1);
		//Normals vector:
		GL30.glVertexAttribPointer(2, 3, GL30.GL_FLOAT, false, 9 * Float.BYTES, 6 * Float.BYTES);
		GL30.glEnableVertexAttribArray(2);
		
		//Cleanup:
		GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0);
	}
	
	public void use()
	{
		GL30.glBindVertexArray(vaoID);
	}
	
	public void draw()
	{
		GL30.glDrawElements(GL30.GL_TRIANGLES, amount, GL30.GL_UNSIGNED_SHORT, 0);
	}
}

package de.ecconia.java.opentung.libwrap.vaos;

import org.lwjgl.opengl.GL30;

public class DynamicBoardVAO extends GenericVAO
{
	public DynamicBoardVAO(float[] vertices, short[] indices)
	{
		super(vertices, indices);
	}
	
	@Override
	protected void init()
	{
		//Position:
		GL30.glVertexAttribPointer(0, 3, GL30.GL_FLOAT, false, 9 * Float.BYTES, 0);
		GL30.glEnableVertexAttribArray(0);
		//Normal:
		GL30.glVertexAttribPointer(2, 3, GL30.GL_FLOAT, false, 9 * Float.BYTES, 3 * Float.BYTES);
		GL30.glEnableVertexAttribArray(2);
		//Texture
		GL30.glVertexAttribPointer(3, 2, GL30.GL_FLOAT, false, 9 * Float.BYTES, 6 * Float.BYTES);
		GL30.glEnableVertexAttribArray(3);
		//Flag isNotSide
		GL30.glVertexAttribPointer(4, 1, GL30.GL_FLOAT, false, 9 * Float.BYTES, 8 * Float.BYTES);
		GL30.glEnableVertexAttribArray(4);
	}
}

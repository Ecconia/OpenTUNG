package de.ecconia.java.opentung.libwrap.vaos;

import de.ecconia.java.opentung.MinMaxBox;
import org.lwjgl.opengl.GL30;

public class LineVAO extends GenericVAO
{
	public static LineVAO fromAbsoluteBoundingBox(MinMaxBox box)
	{
		float[] vertices = {
				(float) box.getMin().getX(), (float) box.getMin().getY(), (float) box.getMin().getZ(), 1, 0, 1,
				(float) box.getMax().getX(), (float) box.getMin().getY(), (float) box.getMin().getZ(), 1, 0, 1,
				(float) box.getMin().getX(), (float) box.getMax().getY(), (float) box.getMin().getZ(), 1, 0, 1,
				(float) box.getMax().getX(), (float) box.getMax().getY(), (float) box.getMin().getZ(), 1, 0, 1,
				(float) box.getMin().getX(), (float) box.getMin().getY(), (float) box.getMax().getZ(), 1, 0, 1,
				(float) box.getMax().getX(), (float) box.getMin().getY(), (float) box.getMax().getZ(), 1, 0, 1,
				(float) box.getMin().getX(), (float) box.getMax().getY(), (float) box.getMax().getZ(), 1, 0, 1,
				(float) box.getMax().getX(), (float) box.getMax().getY(), (float) box.getMax().getZ(), 1, 0, 1,
		};
		
		short[] indices = {
				0, 1,
				0, 2,
				3, 1,
				3, 2,
				
				4, 5,
				4, 6,
				7, 5,
				7, 6,
				
				0, 4,
				1, 5,
				2, 6,
				3, 7,
		};
		
		return new LineVAO(vertices, indices);
	}
	
	public LineVAO(float[] vertices, short[] indices)
	{
		super(vertices, indices);
	}
	
	public static LineVAO generateCrossyIndicator()
	{
		return new LineVAO(new float[]{
				-0.3f, +0.0f, +0.0f, 0.2f, 0.2f, 1.0f,
				+0.3f, +0.0f, +0.0f, 0.2f, 0.2f, 1.0f,
				+0.0f, -0.3f, +0.0f, 0.2f, 0.2f, 1.0f,
				+0.0f, +0.3f, +0.0f, 0.2f, 0.2f, 1.0f,
				+0.0f, +0.0f, -0.3f, 0.2f, 0.2f, 1.0f,
				+0.0f, +0.0f, +0.3f, 0.2f, 0.2f, 1.0f,
		}, new short[]{
				0, 1,
				2, 3,
				4, 5,
		});
	}
	
	public static LineVAO generateAxisIndicator()
	{
		return new LineVAO(new float[]{
				-0.0f, +0.0f, +0.0f, 1.0f, 0.0f, 0.0f,
				+1.0f, +0.0f, +0.0f, 1.0f, 0.0f, 0.0f,
				+0.0f, -0.0f, +0.0f, 0.0f, 1.0f, 0.0f,
				+0.0f, +1.0f, +0.0f, 0.0f, 1.0f, 0.0f,
				+0.0f, +0.0f, -0.0f, 0.0f, 0.0f, 1.0f,
				+0.0f, +0.0f, +1.0f, 0.0f, 0.0f, 1.0f,
		}, new short[]{
				0, 1,
				2, 3,
				4, 5,
		});
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

package de.ecconia.java.opentung.libwrap.vaos;

import org.lwjgl.opengl.GL30;

public class InvisibleCubeVAO extends GenericVAO
{
	public InvisibleCubeVAO(float[] vertices, short[] indices)
	{
		super(vertices, indices);
	}
	
	public static InvisibleCubeVAO generateInvisibleCube()
	{
		return new InvisibleCubeVAO(new float[]{
				+1, -1, -1, //0 - R B N
				-1, -1, -1, //1 - L B N
				-1, +1, -1, //2 - L T N
				+1, +1, -1, //3 - R T N
				+1, -1, +1, //4 - R B F
				-1, -1, +1, //5 - L B F
				-1, +1, +1, //6 - L T F
				+1, +1, +1, //7 - R T F
		}, new short[]{
				//Near:
				1, 0, 3,
				1, 3, 2,
				//Far:
				4, 5, 6,
				4, 6, 7,
				//Right:
				0, 4, 7,
				0, 7, 3,
				//Left:
				5, 1, 2,
				5, 2, 6,
				//Top:
				2, 3, 7,
				2, 7, 6,
				//Bottom:
				5, 4, 0,
				5, 0, 1,
		});
	}
	
	@Override
	protected void init()
	{
		//Position:
		GL30.glVertexAttribPointer(0, 3, GL30.GL_FLOAT, false, 3 * Float.BYTES, 0);
		GL30.glEnableVertexAttribArray(0);
	}
}

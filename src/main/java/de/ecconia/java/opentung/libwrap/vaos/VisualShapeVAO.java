package de.ecconia.java.opentung.libwrap.vaos;

import org.lwjgl.opengl.GL30;

public class VisualShapeVAO extends GenericVAO
{
	public VisualShapeVAO(float[] vertices, short[] indices)
	{
		super(vertices, indices);
	}
	
	public static VisualShapeVAO generateCube()
	{
		//Texture is for Boards only!!!
		float x = 0.01f;
		return new VisualShapeVAO(new float[]{
				//Near:
				+1, -1, -1, +0, +0, -1, 0, 0, //0 - R B N
				-1, -1, -1, +0, +0, -1, x, 0, //1 - L B N
				-1, +1, -1, +0, +0, -1, x, x, //2 - L T N
				+1, +1, -1, +0, +0, -1, 0, x, //3 - R T N
				//Far:
				+1, -1, +1, +0, +0, +1, x, 0, //4 - R B F
				-1, -1, +1, +0, +0, +1, 0, 0, //5 - L B F
				-1, +1, +1, +0, +0, +1, 0, x, //6 - L T F
				+1, +1, +1, +0, +0, +1, x, x, //7 - R T F
				//Right:
				+1, -1, -1, +1, +0, +0, x, 0, //0 - R B N //8
				+1, -1, +1, +1, +0, +0, 0, 0, //4 - R B F //9
				+1, +1, +1, +1, +0, +0, 0, x, //7 - R T F //10
				+1, +1, -1, +1, +0, +0, x, x, //3 - R T N //11
				//Left:
				-1, -1, +1, -1, +0, +0, x, 0, //5 - L B F //12
				-1, -1, -1, -1, +0, +0, 0, 0, //1 - L B N //13
				-1, +1, -1, -1, +0, +0, 0, x, //2 - L T N //14
				-1, +1, +1, -1, +0, +0, x, x, //6 - L T F //15
				//Top:
				-1, +1, -1, +0, +1, +0, 1, 0, //2 - L T N //16
				+1, +1, -1, +0, +1, +0, 0, 0, //3 - R T N //17
				+1, +1, +1, +0, +1, +0, 0, 1, //7 - R T F //18
				-1, +1, +1, +0, +1, +0, 1, 1, //6 - L T F //19
				//Bottom:
				-1, -1, +1, +0, -1, +0, 1, 0, //5 - L B F //20
				+1, -1, +1, +0, -1, +0, 0, 0, //4 - R B F //21
				+1, -1, -1, +0, -1, +0, 0, 1, //0 - R B N //22
				-1, -1, -1, +0, -1, +0, 1, 1, //1 - L B N //23
		}, new short[]{
				//Near:
				1, 0, 3,
				1, 3, 2,
				//Far:
				4, 5, 6,
				4, 6, 7,
				//Right:
				8, 9, 10,
				8, 10, 11,
				//Left:
				12, 13, 14,
				12, 14, 15,
				//Top:
				16, 17, 18,
				16, 18, 19,
				//Bottom:
				20, 21, 22,
				20, 22, 23,
		});
	}
	
	@Override
	protected void init()
	{
		//Position:
		GL30.glVertexAttribPointer(0, 3, GL30.GL_FLOAT, false, 8 * Float.BYTES, 0);
		GL30.glEnableVertexAttribArray(0);
		//Normal:
		GL30.glVertexAttribPointer(1, 3, GL30.GL_FLOAT, false, 8 * Float.BYTES, 3 * Float.BYTES);
		GL30.glEnableVertexAttribArray(1);
		//Texture:
		GL30.glVertexAttribPointer(2, 3, GL30.GL_FLOAT, false, 8 * Float.BYTES, 6 * Float.BYTES);
		GL30.glEnableVertexAttribArray(2);
	}
}

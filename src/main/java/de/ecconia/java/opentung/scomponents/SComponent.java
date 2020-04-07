package de.ecconia.java.opentung.scomponents;

import de.ecconia.java.opentung.libwrap.VAOWrapper;

public abstract class SComponent
{
	protected float[] vertices;
	protected short[] indices;
	
	private VAOWrapper vao;
	
	protected void upload()
	{
		vao = new VAOWrapper(vertices, indices);
	}
	
	public void draw()
	{
		vao.use();
		vao.draw();
	}
	
	protected void placeCube(float cx, float cy, float cz, float hw, float hh, float hd, float r, float g, float b, IntHolder o)
	{
		//Front:
		addVertex(cx - hw, cy - hh, cz - hd, r, g, b, +0, +0, -1, o.getAndInc(9)); //2 Left  Bottom Front
		addVertex(cx + hw, cy - hh, cz - hd, r, g, b, +0, +0, -1, o.getAndInc(9)); //1 Right Bottom Front
		addVertex(cx + hw, cy + hh, cz - hd, r, g, b, +0, +0, -1, o.getAndInc(9)); //5 Right Top    Front
		addVertex(cx - hw, cy + hh, cz - hd, r, g, b, +0, +0, -1, o.getAndInc(9)); //6 Left  Top    Front
		//Right:
		addVertex(cx + hw, cy - hh, cz - hd, r, g, b, +1, +0, +0, o.getAndInc(9)); //1 Right Bottom Front
		addVertex(cx + hw, cy - hh, cz + hd, r, g, b, +1, +0, +0, o.getAndInc(9)); //0 Right Bottom Back
		addVertex(cx + hw, cy + hh, cz + hd, r, g, b, +1, +0, +0, o.getAndInc(9)); //4 Right Top    Back
		addVertex(cx + hw, cy + hh, cz - hd, r, g, b, +1, +0, +0, o.getAndInc(9)); //5 Right Top    Front
		//Back:
		addVertex(cx + hw, cy - hh, cz + hd, r, g, b, +0, +0, +1, o.getAndInc(9)); //0 Right Bottom Back
		addVertex(cx - hw, cy - hh, cz + hd, r, g, b, +0, +0, +1, o.getAndInc(9)); //3 Left  Bottom Back
		addVertex(cx - hw, cy + hh, cz + hd, r, g, b, +0, +0, +1, o.getAndInc(9)); //7 Left  Top    Back
		addVertex(cx + hw, cy + hh, cz + hd, r, g, b, +0, +0, +1, o.getAndInc(9)); //4 Right Top    Back
		//Left:
		addVertex(cx - hw, cy - hh, cz + hd, r, g, b, -1, +0, +0, o.getAndInc(9)); //3 Left  Bottom Back
		addVertex(cx - hw, cy - hh, cz - hd, r, g, b, -1, +0, +0, o.getAndInc(9)); //2 Left  Bottom Front
		addVertex(cx - hw, cy + hh, cz - hd, r, g, b, -1, +0, +0, o.getAndInc(9)); //6 Left  Top    Front
		addVertex(cx - hw, cy + hh, cz + hd, r, g, b, -1, +0, +0, o.getAndInc(9)); //7 Left  Top    Back
		//Up:
		addVertex(cx - hw, cy + hh, cz - hd, r, g, b, +0, +1, +0, o.getAndInc(9)); //6 Left  Top    Front
		addVertex(cx + hw, cy + hh, cz - hd, r, g, b, +0, +1, +0, o.getAndInc(9)); //5 Right Top    Front
		addVertex(cx + hw, cy + hh, cz + hd, r, g, b, +0, +1, +0, o.getAndInc(9)); //4 Right Top    Back
		addVertex(cx - hw, cy + hh, cz + hd, r, g, b, +0, +1, +0, o.getAndInc(9)); //7 Left  Top    Back
		//Down:
		addVertex(cx - hw, cy - hh, cz - hd, r, g, b, +0, -1, +0, o.getAndInc(9)); //2 Left  Bottom Front
		addVertex(cx + hw, cy - hh, cz - hd, r, g, b, +0, -1, +0, o.getAndInc(9)); //1 Right Bottom Front
		addVertex(cx + hw, cy - hh, cz + hd, r, g, b, +0, -1, +0, o.getAndInc(9)); //0 Right Bottom Back
		addVertex(cx - hw, cy - hh, cz + hd, r, g, b, +0, -1, +0, o.getAndInc(9)); //3 Left  Bottom Back
		
	}
	
	protected void placeCubeIndices(IntHolder o, int offset, Integer skip)
	{
		addIndices(0, 1, 2, 3, o.getAndInc(6), offset); //Front
		addIndices(4, 5, 6, 7, o.getAndInc(6), offset); //Right
		addIndices(8, 9, 10, 11, o.getAndInc(6), offset); //Back
		addIndices(12, 13, 14, 15, o.getAndInc(6), offset); //Left
		addIndices(16, 17, 18, 19, o.getAndInc(6), offset); //Up
		addIndices(20, 21, 22, 23, o.getAndInc(6), offset); //Bot
	}
	
	private void addIndices(int a, int b, int c, int d, int o, int indexOffset)
	{
		indices[o++] = (short) (indexOffset + a);
		indices[o++] = (short) (indexOffset + b);
		indices[o++] = (short) (indexOffset + c);
		indices[o++] = (short) (indexOffset + a);
		indices[o++] = (short) (indexOffset + d);
		indices[o] = (short) (indexOffset + c);
	}
	
	private void addVertex(float x, float y, float z, float r, float g, float b, float nx, float ny, float nz, int o)
	{
		vertices[o++] = x; //X
		vertices[o++] = y; //Y
		vertices[o++] = z; //Z
		vertices[o++] = r; //R
		vertices[o++] = g; //G
		vertices[o++] = b; //B
		vertices[o++] = nx; //NormalX
		vertices[o++] = ny; //NormalY
		vertices[o] = nz; //NormalZ
	}
	
	protected static class IntHolder
	{
		private int value = 0;
		
		public int getAndInc(int amount)
		{
			int val = value;
			value += amount;
			return val;
		}
	}
}

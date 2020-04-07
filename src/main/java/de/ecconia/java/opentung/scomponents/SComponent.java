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
		addVertex(cx + hw, cy - hh, cz + hd, r, g, b, o.getAndInc(6)); //0 Right Bottom Back
		addVertex(cx + hw, cy - hh, cz - hd, r, g, b, o.getAndInc(6)); //1 Right Bottom Front
		addVertex(cx - hw, cy - hh, cz - hd, r, g, b, o.getAndInc(6)); //2 Left  Bottom Front
		addVertex(cx - hw, cy - hh, cz + hd, r, g, b, o.getAndInc(6)); //3 Left  Bottom Back
		addVertex(cx + hw, cy + hh, cz + hd, r, g, b, o.getAndInc(6)); //4 Right Top    Back
		addVertex(cx + hw, cy + hh, cz - hd, r, g, b, o.getAndInc(6)); //5 Right Top    Front
		addVertex(cx - hw, cy + hh, cz - hd, r, g, b, o.getAndInc(6)); //6 Left  Top    Front
		addVertex(cx - hw, cy + hh, cz + hd, r, g, b, o.getAndInc(6)); //7 Left  Top    Back
	}
	
	protected void placeCubeIndices(IntHolder o, int offset, Integer skip)
	{
		//            1     2    3 First triangle
		//            1     -    3     2 Second triangle
		addIndices(2, 1, 5, 6, o.getAndInc(6), offset); //Front
		addIndices(1, 0, 4, 5, o.getAndInc(6), offset); //Right
		addIndices(0, 3, 7, 4, o.getAndInc(6), offset); //Back
		addIndices(3, 2, 6, 7, o.getAndInc(6), offset); //Left
		addIndices(6, 5, 4, 7, o.getAndInc(6), offset); //Up
		addIndices(2, 1, 0, 3, o.getAndInc(6), offset); //Bot
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
	
	private void addVertex(float x, float y, float z, float r, float g, float b, int o)
	{
		vertices[o++] = x; //X
		vertices[o++] = y; //Y
		vertices[o++] = z; //Z
		vertices[o++] = r; //R
		vertices[o++] = g; //G
		vertices[o] = b; //B
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

package de.ecconia.java.opentung.models;

import de.ecconia.java.opentung.libwrap.vaos.SolidVAO;
import de.ecconia.java.opentung.libwrap.vaos.DynamicBoardVAO;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;
import de.ecconia.java.opentung.libwrap.vaos.LabelVAO;
import de.ecconia.java.opentung.libwrap.vaos.LineVAO;
import de.ecconia.java.opentung.libwrap.vaos.WireVAO;

public abstract class GenericModel
{
	protected float[] vertices;
	protected short[] indices;
	
	private GenericVAO vao;
	
	protected void upload()
	{
		upload(ShaderType.CubicColored);
	}
	
	protected void upload(ShaderType type)
	{
		if(type == ShaderType.CubicColored)
		{
			vao = new SolidVAO(vertices, indices);
		}
		else if(type == ShaderType.DynamicBoard)
		{
			vao = new DynamicBoardVAO(vertices, indices);
		}
		else if(type == ShaderType.SimpleLines)
		{
			vao = new LineVAO(vertices, indices);
		}
		else if(type == ShaderType.WireShader)
		{
			vao = new WireVAO(vertices, indices);
		}
		else if(type == ShaderType.LabelShader)
		{
			vao = new LabelVAO(vertices, indices);
		}
	}
	
	public void draw()
	{
		vao.use();
		vao.draw();
	}
	
	protected void placeCube(float cx, float cy, float cz, float hw, float hh, float hd, float r, float g, float b, IntHolder o, Direction skip)
	{
		//Front:
		if(skip != Direction.zN)
		{
			addVertex(cx - hw, cy - hh, cz - hd, r, g, b, +0, +0, -1, o.getAndInc(9)); //2 Left  Bottom Front
			addVertex(cx + hw, cy - hh, cz - hd, r, g, b, +0, +0, -1, o.getAndInc(9)); //1 Right Bottom Front
			addVertex(cx + hw, cy + hh, cz - hd, r, g, b, +0, +0, -1, o.getAndInc(9)); //5 Right Top    Front
			addVertex(cx - hw, cy + hh, cz - hd, r, g, b, +0, +0, -1, o.getAndInc(9)); //6 Left  Top    Front
		}
		//Right:
		if(skip != Direction.xP)
		{
			addVertex(cx + hw, cy - hh, cz - hd, r, g, b, +1, +0, +0, o.getAndInc(9)); //1 Right Bottom Front
			addVertex(cx + hw, cy - hh, cz + hd, r, g, b, +1, +0, +0, o.getAndInc(9)); //0 Right Bottom Back
			addVertex(cx + hw, cy + hh, cz + hd, r, g, b, +1, +0, +0, o.getAndInc(9)); //4 Right Top    Back
			addVertex(cx + hw, cy + hh, cz - hd, r, g, b, +1, +0, +0, o.getAndInc(9)); //5 Right Top    Front
		}
		//Back:
		if(skip != Direction.zP)
		{
			addVertex(cx + hw, cy - hh, cz + hd, r, g, b, +0, +0, +1, o.getAndInc(9)); //0 Right Bottom Back
			addVertex(cx - hw, cy - hh, cz + hd, r, g, b, +0, +0, +1, o.getAndInc(9)); //3 Left  Bottom Back
			addVertex(cx - hw, cy + hh, cz + hd, r, g, b, +0, +0, +1, o.getAndInc(9)); //7 Left  Top    Back
			addVertex(cx + hw, cy + hh, cz + hd, r, g, b, +0, +0, +1, o.getAndInc(9)); //4 Right Top    Back
		}
		// Left:
		if(skip != Direction.xN)
		{
			addVertex(cx - hw, cy - hh, cz + hd, r, g, b, -1, +0, +0, o.getAndInc(9)); //3 Left  Bottom Back
			addVertex(cx - hw, cy - hh, cz - hd, r, g, b, -1, +0, +0, o.getAndInc(9)); //2 Left  Bottom Front
			addVertex(cx - hw, cy + hh, cz - hd, r, g, b, -1, +0, +0, o.getAndInc(9)); //6 Left  Top    Front
			addVertex(cx - hw, cy + hh, cz + hd, r, g, b, -1, +0, +0, o.getAndInc(9)); //7 Left  Top    Back
		}
		//Up:
		if(skip != Direction.yP)
		{
			addVertex(cx - hw, cy + hh, cz - hd, r, g, b, +0, +1, +0, o.getAndInc(9)); //6 Left  Top    Front
			addVertex(cx + hw, cy + hh, cz - hd, r, g, b, +0, +1, +0, o.getAndInc(9)); //5 Right Top    Front
			addVertex(cx + hw, cy + hh, cz + hd, r, g, b, +0, +1, +0, o.getAndInc(9)); //4 Right Top    Back
			addVertex(cx - hw, cy + hh, cz + hd, r, g, b, +0, +1, +0, o.getAndInc(9)); //7 Left  Top    Back
		}
		//Down:
		if(skip != Direction.yN)
		{
			addVertex(cx - hw, cy - hh, cz - hd, r, g, b, +0, -1, +0, o.getAndInc(9)); //2 Left  Bottom Front
			addVertex(cx + hw, cy - hh, cz - hd, r, g, b, +0, -1, +0, o.getAndInc(9)); //1 Right Bottom Front
			addVertex(cx + hw, cy - hh, cz + hd, r, g, b, +0, -1, +0, o.getAndInc(9)); //0 Right Bottom Back
			addVertex(cx - hw, cy - hh, cz + hd, r, g, b, +0, -1, +0, o.getAndInc(9)); //3 Left  Bottom Back
		}
	}
	
	protected void placeCubeIndices(IntHolder o, int indexOffset, Direction direction)
	{
		int i = 0;
		if(direction != Direction.zN)
		{
			addIndices(i++, i++, i++, i++, o.getAndInc(6), indexOffset); //Front
		}
		if(direction != Direction.xP)
		{
			addIndices(i++, i++, i++, i++, o.getAndInc(6), indexOffset); //Right
		}
		if(direction != Direction.zP)
		{
			addIndices(i++, i++, i++, i++, o.getAndInc(6), indexOffset); //Back
		}
		if(direction != Direction.xN)
		{
			addIndices(i++, i++, i++, i++, o.getAndInc(6), indexOffset); //Left
		}
		if(direction != Direction.yP)
		{
			addIndices(i++, i++, i++, i++, o.getAndInc(6), indexOffset); //Up
		}
		if(direction != Direction.yN)
		{
			addIndices(i++, i++, i++, i++, o.getAndInc(6), indexOffset); //Bot
		}
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
		vertices[o++] = nx; //NormalX
		vertices[o++] = ny; //NormalY
		vertices[o++] = nz; //NormalZ
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
	
	protected enum Direction
	{
		xP,
		xN,
		yP,
		yN,
		zP,
		zN;
	}
	
	protected enum ShaderType
	{
		CubicColored,
		DynamicBoard,
		SimpleLines,
		WireShader,
		LabelShader,
	}
}

package de.ecconia.java.opentung.scomponents;

public class SimpleDynamicBoard extends SComponent
{
	//2496 bytes
	public SimpleDynamicBoard()
	{
		//Create:
		IntHolder offset = new IntHolder(); //Vertices array offset
		vertices = new float[6 * 4 * 9];
		placeCube(0, 0, 0, 1f, 0.075f, 1f, offset); //Base
		
		offset = new IntHolder(); //Indices array offset
		indices = new short[6 * 6];
		placeCubeIndices(offset, 0, null); //Base
		
		upload(ShaderType.DynamicBoard);
	}
	
	private void placeCube(float cx, float cy, float cz, float hw, float hh, float hd, IntHolder o)
	{
		final float t = 0.01f;
		//Front:
		addVertex(cx - hw, cy - hh, cz - hd, 0, 0, 1, +0, +0, -1, o.getAndInc(9)); //2 Left  Bottom Front
		addVertex(cx + hw, cy - hh, cz - hd, t, 0, 1, +0, +0, -1, o.getAndInc(9)); //1 Right Bottom Front
		addVertex(cx + hw, cy + hh, cz - hd, t, t, 1, +0, +0, -1, o.getAndInc(9)); //5 Right Top    Front
		addVertex(cx - hw, cy + hh, cz - hd, 0, t, 1, +0, +0, -1, o.getAndInc(9)); //6 Left  Top    Front
		//Right:
		addVertex(cx + hw, cy - hh, cz - hd, 0, 0, 1, +1, +0, +0, o.getAndInc(9)); //1 Right Bottom Front
		addVertex(cx + hw, cy - hh, cz + hd, t, 0, 1, +1, +0, +0, o.getAndInc(9)); //0 Right Bottom Back
		addVertex(cx + hw, cy + hh, cz + hd, t, t, 1, +1, +0, +0, o.getAndInc(9)); //4 Right Top    Back
		addVertex(cx + hw, cy + hh, cz - hd, 0, t, 1, +1, +0, +0, o.getAndInc(9)); //5 Right Top    Front
		//Back:
		addVertex(cx + hw, cy - hh, cz + hd, 0, 0, 1, +0, +0, +1, o.getAndInc(9)); //0 Right Bottom Back
		addVertex(cx - hw, cy - hh, cz + hd, t, 0, 1, +0, +0, +1, o.getAndInc(9)); //3 Left  Bottom Back
		addVertex(cx - hw, cy + hh, cz + hd, t, t, 1, +0, +0, +1, o.getAndInc(9)); //7 Left  Top    Back
		addVertex(cx + hw, cy + hh, cz + hd, 0, t, 1, +0, +0, +1, o.getAndInc(9)); //4 Right Top    Back
		// Left:
		addVertex(cx - hw, cy - hh, cz + hd, 0, 0, 1, -1, +0, +0, o.getAndInc(9)); //3 Left  Bottom Back
		addVertex(cx - hw, cy - hh, cz - hd, t, 0, 1, -1, +0, +0, o.getAndInc(9)); //2 Left  Bottom Front
		addVertex(cx - hw, cy + hh, cz - hd, t, t, 1, -1, +0, +0, o.getAndInc(9)); //6 Left  Top    Front
		addVertex(cx - hw, cy + hh, cz + hd, 0, t, 1, -1, +0, +0, o.getAndInc(9)); //7 Left  Top    Back
		//Up:
		addVertex(cx - hw, cy + hh, cz - hd, 0, 0, 0, +0, +1, +0, o.getAndInc(9)); //6 Left  Top    Front
		addVertex(cx + hw, cy + hh, cz - hd, 1, 0, 0, +0, +1, +0, o.getAndInc(9)); //5 Right Top    Front
		addVertex(cx + hw, cy + hh, cz + hd, 1, 1, 0, +0, +1, +0, o.getAndInc(9)); //4 Right Top    Back
		addVertex(cx - hw, cy + hh, cz + hd, 0, 1, 0, +0, +1, +0, o.getAndInc(9)); //7 Left  Top    Back
		//Down:
		addVertex(cx - hw, cy - hh, cz - hd, 0, 0, 0, +0, -1, +0, o.getAndInc(9)); //2 Left  Bottom Front
		addVertex(cx + hw, cy - hh, cz - hd, 1, 0, 0, +0, -1, +0, o.getAndInc(9)); //1 Right Bottom Front
		addVertex(cx + hw, cy - hh, cz + hd, 1, 1, 0, +0, -1, +0, o.getAndInc(9)); //0 Right Bottom Back
		addVertex(cx - hw, cy - hh, cz + hd, 0, 1, 0, +0, -1, +0, o.getAndInc(9)); //3 Left  Bottom Back
	}
	
	private void addVertex(float x, float y, float z, float tx, float ty, float flag, float nx, float ny, float nz, int o)
	{
		vertices[o++] = x; //X
		vertices[o++] = y; //Y
		vertices[o++] = z; //Z
		vertices[o++] = nx; //NormalX
		vertices[o++] = ny; //NormalY
		vertices[o++] = nz; //NormalZ
		vertices[o++] = tx; //TexX
		vertices[o++] = ty; //TexY
		vertices[o] = flag; //FlagNotSide
	}
}

package de.ecconia.java.opentung.models;

public class DynamicWireModel extends GenericModel
{
	//? bytes, calc it yourself man.
	public DynamicWireModel()
	{
		//Create:
		IntHolder offset = new IntHolder(); //Vertices array offset
		vertices = new float[4 * 4 * 6];
		placeCube(0, 0, 0, 0.025f, 0.01f, 1f, offset); //Base
		
		offset = new IntHolder(); //Indices array offset
		indices = new short[4 * 6];
		placeCubeIndices(offset); //Base
		
		upload(ShaderType.WireShader);
	}
	
	protected void placeCube(float cx, float cy, float cz, float hw, float hh, float hd, IntHolder o)
	{
		//Right:
		addVertex(cx + hw, cy - hh, cz - hd, 1, 0, 0, o.getAndInc(6)); //1 Right Bottom Front
		addVertex(cx + hw, cy - hh, cz + hd, 1, 0, 0, o.getAndInc(6)); //0 Right Bottom Back
		addVertex(cx + hw, cy + hh, cz + hd, 1, 0, 0, o.getAndInc(6)); //4 Right Top    Back
		addVertex(cx + hw, cy + hh, cz - hd, 1, 0, 0, o.getAndInc(6)); //5 Right Top    Front
		// Left:
		addVertex(cx - hw, cy - hh, cz + hd, -1, 0, 0, o.getAndInc(6)); //3 Left  Bottom Back
		addVertex(cx - hw, cy - hh, cz - hd, -1, 0, 0, o.getAndInc(6)); //2 Left  Bottom Front
		addVertex(cx - hw, cy + hh, cz - hd, -1, 0, 0, o.getAndInc(6)); //6 Left  Top    Front
		addVertex(cx - hw, cy + hh, cz + hd, -1, 0, 0, o.getAndInc(6)); //7 Left  Top    Back
		//Up:
		addVertex(cx - hw, cy + hh, cz - hd, 0, 1, 0, o.getAndInc(6)); //6 Left  Top    Front
		addVertex(cx + hw, cy + hh, cz - hd, 0, 1, 0, o.getAndInc(6)); //5 Right Top    Front
		addVertex(cx + hw, cy + hh, cz + hd, 0, 1, 0, o.getAndInc(6)); //4 Right Top    Back
		addVertex(cx - hw, cy + hh, cz + hd, 0, 1, 0, o.getAndInc(6)); //7 Left  Top    Back
		//Down:
		addVertex(cx - hw, cy - hh, cz - hd, 0, -1, 0, o.getAndInc(6)); //2 Left  Bottom Front
		addVertex(cx + hw, cy - hh, cz - hd, 0, -1, 0, o.getAndInc(6)); //1 Right Bottom Front
		addVertex(cx + hw, cy - hh, cz + hd, 0, -1, 0, o.getAndInc(6)); //0 Right Bottom Back
		addVertex(cx - hw, cy - hh, cz + hd, 0, -1, 0, o.getAndInc(6)); //3 Left  Bottom Back
	}
	
	protected void placeCubeIndices(IntHolder o)
	{
		int i = 0;
		addIndices(i++, i++, i++, i++, o.getAndInc(6), 0); //Right
		addIndices(i++, i++, i++, i++, o.getAndInc(6), 0); //Left
		addIndices(i++, i++, i++, i++, o.getAndInc(6), 0); //Up
		addIndices(i++, i++, i++, i, o.getAndInc(6), 0); //Bot
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
	
	private void addVertex(float x, float y, float z, float nx, float ny, float nz, int o)
	{
		vertices[o++] = x; //X
		vertices[o++] = y; //Y
		vertices[o++] = z; //Z
		vertices[o++] = nx; //NormalX
		vertices[o++] = ny; //NormalY
		vertices[o] = nz; //NormalZ
	}
}
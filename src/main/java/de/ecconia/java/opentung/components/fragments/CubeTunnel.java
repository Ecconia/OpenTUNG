package de.ecconia.java.opentung.components.fragments;

import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.libwrap.meshes.MeshTypeThing;
import de.ecconia.java.opentung.math.Quaternion;
import de.ecconia.java.opentung.math.Vector3;

public class CubeTunnel extends CubeFull
{
	private final Direction openDirection;
	
	public CubeTunnel(Vector3 position, Vector3 size, Direction openDirection)
	{
		super(position, size, null);
		
		this.openDirection = openDirection;
	}
	
	@Override
	public int getVCount()
	{
		//Faces * Vertices * Data
		return 4 * 4 * 9;
	}
	
	@Override
	public int getICount()
	{
		//Faces * Triangles * Indices
		return 4 * 2 * 3;
	}
	
	@Override
	public void generateModel(float[] vertices, ModelHolder.IntHolder offsetV, short[] indices, ModelHolder.IntHolder offsetI, ModelHolder.IntHolder indexOffset, ModelHolder.TestModelType type, Vector3 offset)
	{
		Vector3 realPosition = position.add(offset);
		Vector3 min = realPosition.subtract(size);
		Vector3 max = realPosition.add(size);
		if(openDirection != Direction.YPos && openDirection != Direction.YNeg)
		{
			//Up:
			genVertex(vertices, offsetV, min.getX(), max.getY(), min.getZ(), 0, 1, 0);
			genVertex(vertices, offsetV, max.getX(), max.getY(), min.getZ(), 0, 1, 0);
			genVertex(vertices, offsetV, max.getX(), max.getY(), max.getZ(), 0, 1, 0);
			genVertex(vertices, offsetV, min.getX(), max.getY(), max.getZ(), 0, 1, 0);
			genIndex(indices, offsetI, indexOffset.getAndInc(4));
			//Down
			genVertex(vertices, offsetV, min.getX(), min.getY(), min.getZ(), 0, -1, 0);
			genVertex(vertices, offsetV, max.getX(), min.getY(), min.getZ(), 0, -1, 0);
			genVertex(vertices, offsetV, max.getX(), min.getY(), max.getZ(), 0, -1, 0);
			genVertex(vertices, offsetV, min.getX(), min.getY(), max.getZ(), 0, -1, 0);
			genIndex(indices, offsetI, indexOffset.getAndInc(4));
		}
		if(openDirection != Direction.XPos && openDirection != Direction.XNeg)
		{
			//Right:
			genVertex(vertices, offsetV, max.getX(), min.getY(), min.getZ(), 1, 0, 0);
			genVertex(vertices, offsetV, max.getX(), min.getY(), max.getZ(), 1, 0, 0);
			genVertex(vertices, offsetV, max.getX(), max.getY(), max.getZ(), 1, 0, 0);
			genVertex(vertices, offsetV, max.getX(), max.getY(), min.getZ(), 1, 0, 0);
			genIndex(indices, offsetI, indexOffset.getAndInc(4));
			//Left:
			genVertex(vertices, offsetV, min.getX(), min.getY(), max.getZ(), -1, 0, 0);
			genVertex(vertices, offsetV, min.getX(), min.getY(), min.getZ(), -1, 0, 0);
			genVertex(vertices, offsetV, min.getX(), max.getY(), min.getZ(), -1, 0, 0);
			genVertex(vertices, offsetV, min.getX(), max.getY(), max.getZ(), -1, 0, 0);
			genIndex(indices, offsetI, indexOffset.getAndInc(4));
		}
		if(openDirection != Direction.ZPos && openDirection != Direction.ZNeg)
		{
			//Forward:
			genVertex(vertices, offsetV, min.getX(), min.getY(), min.getZ(), 0, 0, 1);
			genVertex(vertices, offsetV, max.getX(), min.getY(), min.getZ(), 0, 0, 1);
			genVertex(vertices, offsetV, max.getX(), max.getY(), min.getZ(), 0, 0, 1);
			genVertex(vertices, offsetV, min.getX(), max.getY(), min.getZ(), 0, 0, 1);
			genIndex(indices, offsetI, indexOffset.getAndInc(4));
			//Back:
			genVertex(vertices, offsetV, max.getX(), min.getY(), max.getZ(), 0, 0, -1);
			genVertex(vertices, offsetV, min.getX(), min.getY(), max.getZ(), 0, 0, -1);
			genVertex(vertices, offsetV, min.getX(), max.getY(), max.getZ(), 0, 0, -1);
			genVertex(vertices, offsetV, max.getX(), max.getY(), max.getZ(), 0, 0, -1);
			genIndex(indices, offsetI, indexOffset.getAndInc(4));
		}
	}
	
	public void generateWireMeshEntry(float[] vertices, int verticesIndex, short[] indices, int indicesIndex, ModelHolder.IntHolder vertexCounter, float length, Vector3 color, Vector3 position, Quaternion rotation, MeshTypeThing type)
	{
		Vector3 size = new Vector3(this.size.getX(), this.size.getY(), this.size.getZ() * length * 0.5f);
		Vector3 min = this.position.subtract(size);
		Vector3 max = this.position.add(size);
		
		Vector3 normal;
		ModelHolder.IntHolder offsetV = new ModelHolder.IntHolder(verticesIndex);
		//Position Normal Coord Color
		if(openDirection != Direction.YPos && openDirection != Direction.YNeg)
		{
			//Up:
			normal = rotation.inverse().multiply(new Vector3(0, 1, 0));
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), max.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), max.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), max.getY(), max.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), max.getY(), max.getZ()), normal, color, type);
			genIndex(indices, indicesIndex + 6 * 0, vertexCounter.getAndInc(4));
			//Down
			normal = rotation.inverse().multiply(new Vector3(0, -1, 0));
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), min.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), min.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), min.getY(), max.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), min.getY(), max.getZ()), normal, color, type);
			genIndex(indices, indicesIndex + 6 * 1, vertexCounter.getAndInc(4));
		}
		if(openDirection != Direction.XPos && openDirection != Direction.XNeg)
		{
			//Right:
			normal = rotation.inverse().multiply(new Vector3(1, 0, 0));
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), min.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), min.getY(), max.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), max.getY(), max.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), max.getY(), min.getZ()), normal, color, type);
			genIndex(indices, indicesIndex + 6 * 2, vertexCounter.getAndInc(4));
			//Left:
			normal = rotation.inverse().multiply(new Vector3(-1, 0, 0));
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), min.getY(), max.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), min.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), max.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), max.getY(), max.getZ()), normal, color, type);
			genIndex(indices, indicesIndex + 6 * 3, vertexCounter.getAndInc(4));
		}
		if(openDirection != Direction.ZPos && openDirection != Direction.ZNeg)
		{
			//Forward:
			normal = rotation.inverse().multiply(new Vector3(0, 0, 1));
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), min.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), min.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), max.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), max.getY(), min.getZ()), normal, color, type);
			genIndex(indices, indicesIndex + 6 * 4, vertexCounter.getAndInc(4));
			//Back:
			normal = rotation.inverse().multiply(new Vector3(0, 0, -1));
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), min.getY(), max.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), min.getY(), max.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), max.getY(), max.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), max.getY(), max.getZ()), normal, color, type);
			genIndex(indices, indicesIndex + 6 * 5, vertexCounter.getAndInc(4));
		}
	}
	
	protected void genVertex(float[] vertices, ModelHolder.IntHolder offsetV,
	                         Vector3 gPos, Quaternion rot, Vector3 oPos, Vector3 normal, Vector3 color, MeshTypeThing type)
	{
		Vector3 position = rot.inverse().multiply(oPos).add(gPos);
		//Position
		vertices[offsetV.getAndInc()] = (float) position.getX();
		vertices[offsetV.getAndInc()] = (float) position.getY();
		vertices[offsetV.getAndInc()] = (float) position.getZ();
		if(type.usesNormals())
		{
			//Normal
			vertices[offsetV.getAndInc()] = (float) normal.getX();
			vertices[offsetV.getAndInc()] = (float) normal.getY();
			vertices[offsetV.getAndInc()] = (float) normal.getZ();
		}
//		if(type.usesTextures()) //Doesn't have a texture.
//		{
//			//Coord
//			vertices[offsetV.getAndInc()] = tx;
//			vertices[offsetV.getAndInc()] = ty;
//		}
		if(type.usesColor())
		{
			//Color
			vertices[offsetV.getAndInc()] = (float) color.getX();
			vertices[offsetV.getAndInc()] = (float) color.getY();
			vertices[offsetV.getAndInc()] = (float) color.getZ();
		}
	}
	
	protected void genIndex(short[] indices, int offsetI, int index)
	{
		indices[offsetI + 0] = (short) (index + 0);
		indices[offsetI + 1] = (short) (index + 1);
		indices[offsetI + 2] = (short) (index + 2);
		indices[offsetI + 3] = (short) (index + 0);
		indices[offsetI + 4] = (short) (index + 3);
		indices[offsetI + 5] = (short) (index + 2);
	}
}

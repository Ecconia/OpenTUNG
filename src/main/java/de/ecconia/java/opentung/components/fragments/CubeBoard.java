package de.ecconia.java.opentung.components.fragments;

import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.components.meta.Part;
import de.ecconia.java.opentung.meshing.MeshTypeThing;
import de.ecconia.java.opentung.util.math.Quaternion;
import de.ecconia.java.opentung.util.math.Vector3;

public class CubeBoard extends CubeFull
{
	public CubeBoard(Vector3 position, Vector3 size)
	{
		this(position, size, null);
	}
	
	public CubeBoard(Vector3 position, Vector3 size, ModelMapper mapper)
	{
		//TBI: Temporarily set color:
		super(position, size, Color.boardDefault, mapper);
	}
	
	@Override
	public int getVCount()
	{
		//Faces * Vertices * Data
		return 6 * 4 * 9;
	}
	
	@Override
	public int getICount()
	{
		//Faces * Triangles * Indices
		return 6 * 2 * 3;
	}
	
	@Override
	public void generateMeshEntry(Part component, float[] vertices, ModelHolder.IntHolder offsetV, int[] indices, ModelHolder.IntHolder indicesIndex, ModelHolder.IntHolder vertexCounter, Color colorOverwrite, Vector3 position, Quaternion rotation, Vector3 placementOffset, MeshTypeThing type)
	{
		Vector3 color = this.color;
		if(colorOverwrite != null)
		{
			color = colorOverwrite.asVector();
		}
		
		Vector3 size = mapper == null ? this.size : mapper.getMappedSize(this.getSize(), component);
		Vector3 min = this.position.subtract(size);
		Vector3 max = this.position.add(size);
		
		float t = 0.01f;
		int x = ((CompBoard) component).getX();
		int z = ((CompBoard) component).getZ();
		
		Vector3 normal;
		//Up:
		normal = rotation.inverse().multiply(new Vector3(0, 1, 0));
		genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), max.getY(), min.getZ()), normal, x, 0, color, type);
		genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), max.getY(), min.getZ()), normal, 0, 0, color, type);
		genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), max.getY(), max.getZ()), normal, 0, z, color, type);
		genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), max.getY(), max.getZ()), normal, x, z, color, type);
		genIndex(indices, indicesIndex.getAndInc(6), vertexCounter.getAndInc(4));
		//Down:
		normal = rotation.inverse().multiply(new Vector3(0, -1, 0));
		genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), min.getY(), min.getZ()), normal, 0, 0, color, type);
		genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), min.getY(), min.getZ()), normal, x, 0, color, type);
		genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), min.getY(), max.getZ()), normal, x, z, color, type);
		genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), min.getY(), max.getZ()), normal, 0, z, color, type);
		genIndex(indices, indicesIndex.getAndInc(6), vertexCounter.getAndInc(4));
		//Right:
		normal = rotation.inverse().multiply(new Vector3(1, 0, 0));
		genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), min.getY(), min.getZ()), normal, t, 0, color, type);
		genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), min.getY(), max.getZ()), normal, 0, 0, color, type);
		genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), max.getY(), max.getZ()), normal, 0, t, color, type);
		genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), max.getY(), min.getZ()), normal, t, t, color, type);
		genIndex(indices, indicesIndex.getAndInc(6), vertexCounter.getAndInc(4));
		//Left:
		normal = rotation.inverse().multiply(new Vector3(-1, 0, 0));
		genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), min.getY(), max.getZ()), normal, t, 0, color, type);
		genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), min.getY(), min.getZ()), normal, 0, 0, color, type);
		genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), max.getY(), min.getZ()), normal, 0, t, color, type);
		genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), max.getY(), max.getZ()), normal, t, t, color, type);
		genIndex(indices, indicesIndex.getAndInc(6), vertexCounter.getAndInc(4));
		//Forward:
		normal = rotation.inverse().multiply(new Vector3(0, 0, 1));
		genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), min.getY(), min.getZ()), normal, t, 0, color, type);
		genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), min.getY(), min.getZ()), normal, 0, 0, color, type);
		genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), max.getY(), min.getZ()), normal, 0, t, color, type);
		genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), max.getY(), min.getZ()), normal, t, t, color, type);
		genIndex(indices, indicesIndex.getAndInc(6), vertexCounter.getAndInc(4));
		//Back:
		normal = rotation.inverse().multiply(new Vector3(0, 0, -1));
		genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), min.getY(), max.getZ()), normal, t, 0, color, type);
		genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), min.getY(), max.getZ()), normal, 0, 0, color, type);
		genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), max.getY(), max.getZ()), normal, 0, t, color, type);
		genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), max.getY(), max.getZ()), normal, t, t, color, type);
		genIndex(indices, indicesIndex.getAndInc(6), vertexCounter.getAndInc(4));
	}
	
	protected void genVertex(float[] vertices, ModelHolder.IntHolder offsetV,
	                         Vector3 gPos, Quaternion rot, Vector3 oPos, Vector3 normal, float tx, float ty, Vector3 color, MeshTypeThing type)
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
		if(type.usesTextures())
		{
			//Coord
			vertices[offsetV.getAndInc()] = tx;
			vertices[offsetV.getAndInc()] = ty;
		}
		if(type.usesColor())
		{
			//Color
			vertices[offsetV.getAndInc()] = (float) color.getX();
			vertices[offsetV.getAndInc()] = (float) color.getY();
			vertices[offsetV.getAndInc()] = (float) color.getZ();
		}
	}
	
	protected void genIndex(int[] indices, int offsetI, int index)
	{
		indices[offsetI + 0] = (index + 0);
		indices[offsetI + 1] = (index + 1);
		indices[offsetI + 2] = (index + 2);
		indices[offsetI + 3] = (index + 0);
		indices[offsetI + 4] = (index + 2);
		indices[offsetI + 5] = (index + 3);
	}
}

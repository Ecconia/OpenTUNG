package de.ecconia.java.opentung.components.fragments;

import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.libwrap.meshes.MeshTypeThing;
import de.ecconia.java.opentung.math.Quaternion;
import de.ecconia.java.opentung.math.Vector3;

public class CubeTunnel extends CubeFull
{
	private final Direction openDirection;
	
	public CubeTunnel(Vector3 position, Vector3 size, Direction openDirection)
	{
		this(position, size, openDirection, null, null);
	}
	
	public CubeTunnel(Vector3 position, Vector3 size, Direction openDirection, ModelMapper mapper)
	{
		this(position, size, openDirection, null, mapper);
	}
	
	public CubeTunnel(Vector3 position, Vector3 size, Direction openDirection, Color color)
	{
		this(position, size, openDirection, color, null);
	}
	
	public CubeTunnel(Vector3 position, Vector3 size, Direction openDirection, Color color, ModelMapper mapper)
	{
		super(position, size, color, mapper);
		
		this.openDirection = openDirection;
	}
	
	@Override
	public int getFacesCount()
	{
		return 4;
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
	public void generateMeshEntry(Component instance, float[] vertices, ModelHolder.IntHolder offsetV, int[] indices, ModelHolder.IntHolder indicesIndex, ModelHolder.IntHolder vertexCounter, Vector3 color, Vector3 position, Quaternion rotation, Vector3 placementOffset, MeshTypeThing type)
	{
		if(color == null && this.color != null)
		{
			color = this.color.asVector();
		}
		
		Vector3 size = mapper == null ? this.size : mapper.getMappedSize(this.size, instance);
		Vector3 min = this.position.subtract(size);
		Vector3 max = this.position.add(size);
		
		Vector3 normal;
		//Position Normal Coord Color
		if(openDirection != Direction.YPos && openDirection != Direction.YNeg)
		{
			//Up:
			normal = rotation.inverse().multiply(new Vector3(0, 1, 0));
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), max.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), max.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), max.getY(), max.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), max.getY(), max.getZ()), normal, color, type);
			genIndex(indices, indicesIndex.getAndInc(6), vertexCounter.getAndInc(4));
			//Down
			normal = rotation.inverse().multiply(new Vector3(0, -1, 0));
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), min.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), min.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), min.getY(), max.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), min.getY(), max.getZ()), normal, color, type);
			genIndex(indices, indicesIndex.getAndInc(6), vertexCounter.getAndInc(4));
		}
		if(openDirection != Direction.XPos && openDirection != Direction.XNeg)
		{
			//Right:
			normal = rotation.inverse().multiply(new Vector3(1, 0, 0));
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), min.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), min.getY(), max.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), max.getY(), max.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), max.getY(), min.getZ()), normal, color, type);
			genIndex(indices, indicesIndex.getAndInc(6), vertexCounter.getAndInc(4));
			//Left:
			normal = rotation.inverse().multiply(new Vector3(-1, 0, 0));
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), min.getY(), max.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), min.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), max.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), max.getY(), max.getZ()), normal, color, type);
			genIndex(indices, indicesIndex.getAndInc(6), vertexCounter.getAndInc(4));
		}
		if(openDirection != Direction.ZPos && openDirection != Direction.ZNeg)
		{
			//Forward:
			normal = rotation.inverse().multiply(new Vector3(0, 0, 1));
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), min.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), min.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), max.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), max.getY(), min.getZ()), normal, color, type);
			genIndex(indices, indicesIndex.getAndInc(6), vertexCounter.getAndInc(4));
			//Back:
			normal = rotation.inverse().multiply(new Vector3(0, 0, -1));
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), min.getY(), max.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), min.getY(), max.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), max.getY(), max.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), max.getY(), max.getZ()), normal, color, type);
			genIndex(indices, indicesIndex.getAndInc(6), vertexCounter.getAndInc(4));
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

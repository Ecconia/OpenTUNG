package de.ecconia.java.opentung.components.fragments;

import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.libwrap.meshes.MeshTypeThing;
import de.ecconia.java.opentung.math.Quaternion;
import de.ecconia.java.opentung.math.Vector3;

public class CubeFull extends Meshable
{
	protected final Vector3 position;
	protected final Vector3 size;
	
	protected ModelMapper mapper;
	protected Color color;
	
	public CubeFull(Vector3 position, Vector3 size, Color color, ModelMapper mapper)
	{
		this.position = position;
		this.size = size.divide(2); //Collision as well as shader only use half the value.
		this.mapper = mapper;
		this.color = color;
	}
	
	public CubeFull(Vector3 position, Vector3 size, Color color)
	{
		this(position, size, color, null);
	}
	
	public int getFacesCount()
	{
		return 6;
	}
	
	public boolean contains(Vector3 probe)
	{
		return !(probe.getX() < position.getX() - size.getX()
				|| probe.getX() > position.getX() + size.getX()
				|| probe.getY() < position.getY() - size.getY()
				|| probe.getY() > position.getY() + size.getY()
				|| probe.getZ() < position.getZ() - size.getZ()
				|| probe.getZ() > position.getZ() + size.getZ());
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
	
	public ModelMapper getMapper()
	{
		return mapper;
	}
	
	protected void genIndex(short[] indices, ModelHolder.IntHolder offsetI, int index)
	{
		indices[offsetI.getAndInc()] = (short) (index + 0);
		indices[offsetI.getAndInc()] = (short) (index + 1);
		indices[offsetI.getAndInc()] = (short) (index + 2);
		indices[offsetI.getAndInc()] = (short) (index + 0);
		indices[offsetI.getAndInc()] = (short) (index + 2);
		indices[offsetI.getAndInc()] = (short) (index + 3);
	}
	
	protected void genVertex(float[] vertices, ModelHolder.IntHolder offsetV,
	                         double x, double y, double z, float nx, float ny, float nz)
	{
		vertices[offsetV.getAndInc()] = (float) x;
		vertices[offsetV.getAndInc()] = (float) y;
		vertices[offsetV.getAndInc()] = (float) z;
		vertices[offsetV.getAndInc()] = nx;
		vertices[offsetV.getAndInc()] = ny;
		vertices[offsetV.getAndInc()] = nz;
		if(color != null)
		{
			vertices[offsetV.getAndInc()] = (float) color.getR();
			vertices[offsetV.getAndInc()] = (float) color.getG();
			vertices[offsetV.getAndInc()] = (float) color.getB();
		}
	}
	
	public Color getColor()
	{
		return color;
	}
	
	public void setColor(Color color)
	{
		this.color = color;
	}
	
	public Vector3 getPosition()
	{
		return position;
	}
	
	public Vector3 getSize()
	{
		return size;
	}
	
	public void generateMeshEntry(Component instance, float[] vertices, ModelHolder.IntHolder offsetV, int[] indices, ModelHolder.IntHolder indicesIndex, ModelHolder.IntHolder vertexCounter, Vector3 color, Vector3 position, Quaternion rotation, Vector3 placementOffset, MeshTypeThing type)
	{
		if(color == null && this.color != null)
		{
			color = this.color.asVector();
		}
		
		Vector3 size = new Vector3(this.size.getX(), this.size.getY(), this.size.getZ());
		Vector3 min = this.position.add(placementOffset).subtract(size);
		Vector3 max = this.position.add(placementOffset).add(size);
		
		Vector3 normal;
		//Position Normal Coord Color
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

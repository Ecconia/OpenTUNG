package de.ecconia.java.opentung.components.fragments;

import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.libwrap.meshes.MeshTypeThing;
import de.ecconia.java.opentung.math.Quaternion;
import de.ecconia.java.opentung.math.Vector3;

public class CubeOpenRotated extends CubeFull
{
	protected final Direction direction;
	protected final Quaternion rotation;
	
	public CubeOpenRotated(Quaternion rotation, Vector3 position, Vector3 size, Direction openDirection)
	{
		this(rotation, position, size, openDirection, null);
	}
	
	public CubeOpenRotated(Quaternion rotation, Vector3 position, Vector3 size, Direction openDirection, Color color)
	{
		super(position, size, color);
		
		this.rotation = rotation;
		this.direction = openDirection;
	}
	
	public Quaternion getRotation()
	{
		return rotation;
	}
	
	@Override
	public int getFacesCount()
	{
		return 5;
	}
	
	@Override
	public int getVCount()
	{
		//Faces * Vertices * Data
		return 5 * 4 * 9;
	}
	
	@Override
	public int getICount()
	{
		//Faces * Triangles * Indices
		return 5 * 2 * 3;
	}
	
	@Override
	public boolean contains(Vector3 probe)
	{
		probe = rotation.inverse().multiply(probe);
		return super.contains(probe);
	}
	
	@Override
	public void generateModel(float[] vertices, ModelHolder.IntHolder offsetV, short[] indices, ModelHolder.IntHolder offsetI, ModelHolder.IntHolder indexOffset, ModelHolder.TestModelType type, Vector3 offset)
	{
		Vector3 realPosition = position;
		Vector3 min = realPosition.subtract(size);
		Vector3 max = realPosition.add(size);
		if(direction != Direction.YPos)
		{
			//Up:
			genVertex(vertices, offsetV, rotation, offset, new Vector3(min.getX(), max.getY(), min.getZ()), 0, 1, 0);
			genVertex(vertices, offsetV, rotation, offset, new Vector3(max.getX(), max.getY(), min.getZ()), 0, 1, 0);
			genVertex(vertices, offsetV, rotation, offset, new Vector3(max.getX(), max.getY(), max.getZ()), 0, 1, 0);
			genVertex(vertices, offsetV, rotation, offset, new Vector3(min.getX(), max.getY(), max.getZ()), 0, 1, 0);
			genIndex(indices, offsetI, indexOffset.getAndInc(4));
		}
		if(direction != Direction.YNeg)
		{
			//Down
			genVertex(vertices, offsetV, rotation, offset, new Vector3(max.getX(), min.getY(), min.getZ()), 0, -1, 0);
			genVertex(vertices, offsetV, rotation, offset, new Vector3(min.getX(), min.getY(), min.getZ()), 0, -1, 0);
			genVertex(vertices, offsetV, rotation, offset, new Vector3(min.getX(), min.getY(), max.getZ()), 0, -1, 0);
			genVertex(vertices, offsetV, rotation, offset, new Vector3(max.getX(), min.getY(), max.getZ()), 0, -1, 0);
			genIndex(indices, offsetI, indexOffset.getAndInc(4));
		}
		if(direction != Direction.XPos)
		{
			//Right:
			genVertex(vertices, offsetV, rotation, offset, new Vector3(max.getX(), min.getY(), min.getZ()), 1, 0, 0);
			genVertex(vertices, offsetV, rotation, offset, new Vector3(max.getX(), min.getY(), max.getZ()), 1, 0, 0);
			genVertex(vertices, offsetV, rotation, offset, new Vector3(max.getX(), max.getY(), max.getZ()), 1, 0, 0);
			genVertex(vertices, offsetV, rotation, offset, new Vector3(max.getX(), max.getY(), min.getZ()), 1, 0, 0);
			genIndex(indices, offsetI, indexOffset.getAndInc(4));
		}
		if(direction != Direction.XNeg)
		{
			//Left:
			genVertex(vertices, offsetV, rotation, offset, new Vector3(min.getX(), min.getY(), max.getZ()), -1, 0, 0);
			genVertex(vertices, offsetV, rotation, offset, new Vector3(min.getX(), min.getY(), min.getZ()), -1, 0, 0);
			genVertex(vertices, offsetV, rotation, offset, new Vector3(min.getX(), max.getY(), min.getZ()), -1, 0, 0);
			genVertex(vertices, offsetV, rotation, offset, new Vector3(min.getX(), max.getY(), max.getZ()), -1, 0, 0);
			genIndex(indices, offsetI, indexOffset.getAndInc(4));
		}
		if(direction != Direction.ZNeg)
		{
			//Forward:
			genVertex(vertices, offsetV, rotation, offset, new Vector3(min.getX(), min.getY(), min.getZ()), 0, 0, 1);
			genVertex(vertices, offsetV, rotation, offset, new Vector3(max.getX(), min.getY(), min.getZ()), 0, 0, 1);
			genVertex(vertices, offsetV, rotation, offset, new Vector3(max.getX(), max.getY(), min.getZ()), 0, 0, 1);
			genVertex(vertices, offsetV, rotation, offset, new Vector3(min.getX(), max.getY(), min.getZ()), 0, 0, 1);
			genIndex(indices, offsetI, indexOffset.getAndInc(4));
		}
		if(direction != Direction.ZPos)
		{
			//Back:
			genVertex(vertices, offsetV, rotation, offset, new Vector3(max.getX(), min.getY(), max.getZ()), 0, 0, -1);
			genVertex(vertices, offsetV, rotation, offset, new Vector3(min.getX(), min.getY(), max.getZ()), 0, 0, -1);
			genVertex(vertices, offsetV, rotation, offset, new Vector3(min.getX(), max.getY(), max.getZ()), 0, 0, -1);
			genVertex(vertices, offsetV, rotation, offset, new Vector3(max.getX(), max.getY(), max.getZ()), 0, 0, -1);
			genIndex(indices, offsetI, indexOffset.getAndInc(4));
		}
	}
	
	protected void genVertex(float[] vertices, ModelHolder.IntHolder offsetV,
	                         Quaternion rotation, Vector3 offset, Vector3 position, float nx, float ny, float nz)
	{
		position = rotation.multiply(position).add(offset);
		
		vertices[offsetV.getAndInc()] = (float) position.getX();
		vertices[offsetV.getAndInc()] = (float) position.getY();
		vertices[offsetV.getAndInc()] = (float) position.getZ();
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
	
	@Override
	public void generateMeshEntry(float[] vertices, ModelHolder.IntHolder offsetV, int[] indices, ModelHolder.IntHolder indicesIndex, ModelHolder.IntHolder vertexCounter, Vector3 color, Vector3 position, Quaternion rotation, Vector3 placementOffset, MeshTypeThing type)
	{
		if(color == null && this.color != null)
		{
			color = this.color.asVector();
		}
		
		Vector3 size = new Vector3(this.size.getX(), this.size.getY(), this.size.getZ());
		Vector3 min = this.position.subtract(size);
		Vector3 max = this.position.add(size);
		
		Vector3 normal;
		//Position Normal Coord Color
		if(direction != Direction.YPos)
		{
			//Up:
			normal = rotation.inverse().multiply(new Vector3(0, 1, 0));
			genVertex(vertices, offsetV, this.rotation, placementOffset, position, rotation, new Vector3(min.getX(), max.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, this.rotation, placementOffset, position, rotation, new Vector3(max.getX(), max.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, this.rotation, placementOffset, position, rotation, new Vector3(max.getX(), max.getY(), max.getZ()), normal, color, type);
			genVertex(vertices, offsetV, this.rotation, placementOffset, position, rotation, new Vector3(min.getX(), max.getY(), max.getZ()), normal, color, type);
			genIndex(indices, indicesIndex.getAndInc(6), vertexCounter.getAndInc(4));
		}
		if(direction != Direction.YNeg)
		{
			//Down
			normal = rotation.inverse().multiply(new Vector3(0, -1, 0));
			genVertex(vertices, offsetV, this.rotation, placementOffset, position, rotation, new Vector3(max.getX(), min.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, this.rotation, placementOffset, position, rotation, new Vector3(min.getX(), min.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, this.rotation, placementOffset, position, rotation, new Vector3(min.getX(), min.getY(), max.getZ()), normal, color, type);
			genVertex(vertices, offsetV, this.rotation, placementOffset, position, rotation, new Vector3(max.getX(), min.getY(), max.getZ()), normal, color, type);
			genIndex(indices, indicesIndex.getAndInc(6), vertexCounter.getAndInc(4));
		}
		if(direction != Direction.XPos)
		{
			//Right:
			normal = rotation.inverse().multiply(new Vector3(1, 0, 0));
			genVertex(vertices, offsetV, this.rotation, placementOffset, position, rotation, new Vector3(max.getX(), min.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, this.rotation, placementOffset, position, rotation, new Vector3(max.getX(), min.getY(), max.getZ()), normal, color, type);
			genVertex(vertices, offsetV, this.rotation, placementOffset, position, rotation, new Vector3(max.getX(), max.getY(), max.getZ()), normal, color, type);
			genVertex(vertices, offsetV, this.rotation, placementOffset, position, rotation, new Vector3(max.getX(), max.getY(), min.getZ()), normal, color, type);
			genIndex(indices, indicesIndex.getAndInc(6), vertexCounter.getAndInc(4));
		}
		if(direction != Direction.XNeg)
		{
			//Left:
			normal = rotation.inverse().multiply(new Vector3(-1, 0, 0));
			genVertex(vertices, offsetV, this.rotation, placementOffset, position, rotation, new Vector3(min.getX(), min.getY(), max.getZ()), normal, color, type);
			genVertex(vertices, offsetV, this.rotation, placementOffset, position, rotation, new Vector3(min.getX(), min.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, this.rotation, placementOffset, position, rotation, new Vector3(min.getX(), max.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, this.rotation, placementOffset, position, rotation, new Vector3(min.getX(), max.getY(), max.getZ()), normal, color, type);
			genIndex(indices, indicesIndex.getAndInc(6), vertexCounter.getAndInc(4));
		}
		if(direction != Direction.ZNeg)
		{
			//Forward:
			normal = rotation.inverse().multiply(new Vector3(0, 0, 1));
			genVertex(vertices, offsetV, this.rotation, placementOffset, position, rotation, new Vector3(min.getX(), min.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, this.rotation, placementOffset, position, rotation, new Vector3(max.getX(), min.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, this.rotation, placementOffset, position, rotation, new Vector3(max.getX(), max.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, this.rotation, placementOffset, position, rotation, new Vector3(min.getX(), max.getY(), min.getZ()), normal, color, type);
			genIndex(indices, indicesIndex.getAndInc(6), vertexCounter.getAndInc(4));
		}
		if(direction != Direction.ZPos)
		{
			//Back:
			normal = rotation.inverse().multiply(new Vector3(0, 0, -1));
			genVertex(vertices, offsetV, this.rotation, placementOffset, position, rotation, new Vector3(max.getX(), min.getY(), max.getZ()), normal, color, type);
			genVertex(vertices, offsetV, this.rotation, placementOffset, position, rotation, new Vector3(min.getX(), min.getY(), max.getZ()), normal, color, type);
			genVertex(vertices, offsetV, this.rotation, placementOffset, position, rotation, new Vector3(min.getX(), max.getY(), max.getZ()), normal, color, type);
			genVertex(vertices, offsetV, this.rotation, placementOffset, position, rotation, new Vector3(max.getX(), max.getY(), max.getZ()), normal, color, type);
			genIndex(indices, indicesIndex.getAndInc(6), vertexCounter.getAndInc(4));
		}
	}
	
	protected void genVertex(float[] vertices, ModelHolder.IntHolder offsetV,
	                         Quaternion localRotation, Vector3 placementOffset, Vector3 globalPos, Quaternion componentRotation, Vector3 oPos, Vector3 normal, Vector3 color, MeshTypeThing type)
	{
		Vector3 compoentPosition = localRotation.multiply(oPos);
		Vector3 position = componentRotation.inverse().multiply(compoentPosition.add(placementOffset)).add(globalPos);
		
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
}

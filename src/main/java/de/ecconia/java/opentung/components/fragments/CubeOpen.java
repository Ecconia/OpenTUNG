package de.ecconia.java.opentung.components.fragments;

import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.libwrap.meshes.MeshTypeThing;
import de.ecconia.java.opentung.math.Quaternion;
import de.ecconia.java.opentung.math.Vector3;

public class CubeOpen extends CubeFull
{
	protected final Direction direction;
	
	public CubeOpen(Vector3 position, Vector3 size, Direction openDirection)
	{
		this(position, size, openDirection, null);
	}
	
	public CubeOpen(Vector3 position, Vector3 size, Direction openDirection, Color color)
	{
		super(position, size, color);
		
		this.direction = openDirection;
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
	public void generateModel(float[] vertices, ModelHolder.IntHolder offsetV, short[] indices, ModelHolder.IntHolder offsetI, ModelHolder.IntHolder indexOffset, ModelHolder.TestModelType type, Vector3 offset)
	{
		Vector3 realPosition = position.add(offset);
		Vector3 min = realPosition.subtract(size);
		Vector3 max = realPosition.add(size);
		if(direction != Direction.YPos)
		{
			//Up:
			genVertex(vertices, offsetV, min.getX(), max.getY(), min.getZ(), 0, 1, 0);
			genVertex(vertices, offsetV, max.getX(), max.getY(), min.getZ(), 0, 1, 0);
			genVertex(vertices, offsetV, max.getX(), max.getY(), max.getZ(), 0, 1, 0);
			genVertex(vertices, offsetV, min.getX(), max.getY(), max.getZ(), 0, 1, 0);
			genIndex(indices, offsetI, indexOffset.getAndInc(4));
		}
		if(direction != Direction.YNeg)
		{
			//Down
			genVertex(vertices, offsetV, max.getX(), min.getY(), min.getZ(), 0, -1, 0);
			genVertex(vertices, offsetV, min.getX(), min.getY(), min.getZ(), 0, -1, 0);
			genVertex(vertices, offsetV, min.getX(), min.getY(), max.getZ(), 0, -1, 0);
			genVertex(vertices, offsetV, max.getX(), min.getY(), max.getZ(), 0, -1, 0);
			genIndex(indices, offsetI, indexOffset.getAndInc(4));
		}
		if(direction != Direction.XPos)
		{
			//Right:
			genVertex(vertices, offsetV, max.getX(), min.getY(), min.getZ(), 1, 0, 0);
			genVertex(vertices, offsetV, max.getX(), min.getY(), max.getZ(), 1, 0, 0);
			genVertex(vertices, offsetV, max.getX(), max.getY(), max.getZ(), 1, 0, 0);
			genVertex(vertices, offsetV, max.getX(), max.getY(), min.getZ(), 1, 0, 0);
			genIndex(indices, offsetI, indexOffset.getAndInc(4));
		}
		if(direction != Direction.XNeg)
		{
			//Left:
			genVertex(vertices, offsetV, min.getX(), min.getY(), max.getZ(), -1, 0, 0);
			genVertex(vertices, offsetV, min.getX(), min.getY(), min.getZ(), -1, 0, 0);
			genVertex(vertices, offsetV, min.getX(), max.getY(), min.getZ(), -1, 0, 0);
			genVertex(vertices, offsetV, min.getX(), max.getY(), max.getZ(), -1, 0, 0);
			genIndex(indices, offsetI, indexOffset.getAndInc(4));
		}
		if(direction != Direction.ZNeg)
		{
			//Forward:
			genVertex(vertices, offsetV, min.getX(), min.getY(), min.getZ(), 0, 0, 1);
			genVertex(vertices, offsetV, max.getX(), min.getY(), min.getZ(), 0, 0, 1);
			genVertex(vertices, offsetV, max.getX(), max.getY(), min.getZ(), 0, 0, 1);
			genVertex(vertices, offsetV, min.getX(), max.getY(), min.getZ(), 0, 0, 1);
			genIndex(indices, offsetI, indexOffset.getAndInc(4));
		}
		if(direction != Direction.ZPos)
		{
			//Back:
			genVertex(vertices, offsetV, max.getX(), min.getY(), max.getZ(), 0, 0, -1);
			genVertex(vertices, offsetV, min.getX(), min.getY(), max.getZ(), 0, 0, -1);
			genVertex(vertices, offsetV, min.getX(), max.getY(), max.getZ(), 0, 0, -1);
			genVertex(vertices, offsetV, max.getX(), max.getY(), max.getZ(), 0, 0, -1);
			genIndex(indices, offsetI, indexOffset.getAndInc(4));
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
		Vector3 min = this.position.add(placementOffset).subtract(size);
		Vector3 max = this.position.add(placementOffset).add(size);
		
		Vector3 normal;
		//Position Normal Coord Color
		if(direction != Direction.YPos)
		{
			//Up:
			normal = rotation.inverse().multiply(new Vector3(0, 1, 0));
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), max.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), max.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), max.getY(), max.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), max.getY(), max.getZ()), normal, color, type);
			genIndex(indices, indicesIndex.getAndInc(6), vertexCounter.getAndInc(4));
		}
		if(direction != Direction.YNeg)
		{
			//Down
			normal = rotation.inverse().multiply(new Vector3(0, -1, 0));
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), min.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), min.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), min.getY(), max.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), min.getY(), max.getZ()), normal, color, type);
			genIndex(indices, indicesIndex.getAndInc(6), vertexCounter.getAndInc(4));
		}
		if(direction != Direction.XPos)
		{
			//Right:
			normal = rotation.inverse().multiply(new Vector3(1, 0, 0));
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), min.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), min.getY(), max.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), max.getY(), max.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), max.getY(), min.getZ()), normal, color, type);
			genIndex(indices, indicesIndex.getAndInc(6), vertexCounter.getAndInc(4));
		}
		if(direction != Direction.XNeg)
		{
			//Left:
			normal = rotation.inverse().multiply(new Vector3(-1, 0, 0));
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), min.getY(), max.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), min.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), max.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), max.getY(), max.getZ()), normal, color, type);
			genIndex(indices, indicesIndex.getAndInc(6), vertexCounter.getAndInc(4));
		}
		if(direction != Direction.ZNeg)
		{
			//Forward:
			normal = rotation.inverse().multiply(new Vector3(0, 0, 1));
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), min.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), min.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), max.getY(), min.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), max.getY(), min.getZ()), normal, color, type);
			genIndex(indices, indicesIndex.getAndInc(6), vertexCounter.getAndInc(4));
		}
		if(direction != Direction.ZPos)
		{
			//Back:
			normal = rotation.inverse().multiply(new Vector3(0, 0, -1));
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), min.getY(), max.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), min.getY(), max.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(min.getX(), max.getY(), max.getZ()), normal, color, type);
			genVertex(vertices, offsetV, position, rotation, new Vector3(max.getX(), max.getY(), max.getZ()), normal, color, type);
			genIndex(indices, indicesIndex.getAndInc(6), vertexCounter.getAndInc(4));
		}
	}
}

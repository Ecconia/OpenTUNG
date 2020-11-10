package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.conductor.CompWireRaw;
import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.CubeTunnel;
import de.ecconia.java.opentung.components.fragments.Direction;
import de.ecconia.java.opentung.components.fragments.ModelMapper;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelBuilder;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.components.meta.Part;
import de.ecconia.java.opentung.meshing.MeshTypeThing;
import de.ecconia.java.opentung.util.math.Vector3;

public class CompSnappingWire extends CompWireRaw
{
	public static final ModelHolder modelHolder = new ModelBuilder()
			.setPlacementOffset(new Vector3(0.0, 0.0, 0.0))
			.addSolid(new CubeTunnel(new Vector3(0.0, 0.0, 0.0), new Vector3(0.05, 0.02, 2.0), Direction.ZPos, Color.snappingPeg, new ModelMapper()
			{
				@Override
				public Vector3 getMappedSize(Vector3 size, Part component)
				{
					return new Vector3(size.getX(), size.getY(), size.getZ() * ((CompSnappingWire) component).getLength() * 0.5f);
				}
			}))
			.build();
	
	@Override
	public ModelHolder getModelHolder()
	{
		return modelHolder;
	}
	
	//### Non-Static ###
	
	public CompSnappingWire(Component parent)
	{
		super(parent);
	}
	
	@Override
	public int getWholeMeshEntryVCount(MeshTypeThing type)
	{
		return type == MeshTypeThing.Solid ? 4 * 4 * (3 + 3 + 3) : 0;
	}
	
	@Override
	public int getWholeMeshEntryICount(MeshTypeThing type)
	{
		return type == MeshTypeThing.Solid ? 4 * (3 * 2) : 0;
	}
	
	@Override
	public void insertMeshData(float[] vertices, ModelHolder.IntHolder verticesIndex, int[] indices, ModelHolder.IntHolder indicesIndex, ModelHolder.IntHolder vertexCounter, MeshTypeThing type)
	{
		if(type == MeshTypeThing.Solid)
		{
			//TODO: This is very ungeneric. Well the model is known.
			((CubeFull) getModelHolder().getSolid().get(0)).generateMeshEntry(this, vertices, verticesIndex, indices, indicesIndex, vertexCounter, null, getPosition(), getRotation(), modelHolder.getPlacementOffset(), type);
		}
	}
}

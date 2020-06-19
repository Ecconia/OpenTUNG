package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeTunnel;
import de.ecconia.java.opentung.components.fragments.Direction;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.libwrap.meshes.MeshTypeThing;
import de.ecconia.java.opentung.math.Vector3;

public class CompSnappingWire extends CompWireRaw
{
	public static final ModelHolder modelHolder = new ModelHolder();
	
	static
	{
		modelHolder.setPlacementOffset(new Vector3(0.0, 0.0, 0.0));
		modelHolder.addSolid(new CubeTunnel(new Vector3(0.0, 0.0, 0.0), new Vector3(0.05, 0.02, 2.0), Direction.ZPos, Color.snappingPeg));
	}
	
	public static void initGL()
	{
		modelHolder.generateTestModel(ModelHolder.TestModelType.Wire, true);
	}
	
	@Override
	public ModelHolder getModelHolder()
	{
		return modelHolder;
	}
	
	//### Non-Static ###
	
	public CompSnappingWire(CompContainer parent)
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
			//TODO: This is super ungeneric, beware.
			((CubeTunnel) getModelHolder().getSolid().get(0)).generateWireMeshEntry(vertices, verticesIndex, indices, indicesIndex, vertexCounter, getLength(), null, getPosition(), getRotation(), type);
		}
	}
}

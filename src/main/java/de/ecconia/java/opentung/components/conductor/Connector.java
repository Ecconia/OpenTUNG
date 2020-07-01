package de.ecconia.java.opentung.components.conductor;

import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.components.meta.Part;
import de.ecconia.java.opentung.libwrap.meshes.MeshTypeThing;
import de.ecconia.java.opentung.math.Quaternion;
import de.ecconia.java.opentung.math.Vector3;
import de.ecconia.java.opentung.simulation.Cluster;
import de.ecconia.java.opentung.simulation.Clusterable;
import de.ecconia.java.opentung.simulation.Wire;
import java.util.ArrayList;
import java.util.List;

public abstract class Connector extends Part implements Clusterable
{
	private final CubeFull model;
	private final List<Wire> wires = new ArrayList<>();
	
	private Cluster cluster;
	
	public Connector(Component parent, CubeFull model)
	{
		super(parent);
		this.model = model;
		
		setRotation(Quaternion.angleAxis(0, Vector3.yp));
	}
	
	@Override
	public void setCluster(Cluster cluster)
	{
		this.cluster = cluster;
	}
	
	@Override
	public boolean hasCluster()
	{
		return cluster != null;
	}
	
	@Override
	public Cluster getCluster()
	{
		return cluster;
	}
	
	public void addWire(Wire wire)
	{
		wires.add(wire);
	}
	
	public List<Wire> getWires()
	{
		return wires;
	}
	
	public CubeFull getModel()
	{
		return model;
	}
	
	public boolean contains(Vector3 probe)
	{
		return model.contains(probe);
	}
	
	@Override
	public int getWholeMeshEntryVCount(MeshTypeThing type)
	{
		if(type == MeshTypeThing.Conductor || type == MeshTypeThing.Raycast)
		{
			return model.getFacesCount() * 4 * type.getFloatCount();
		}
		else
		{
			throw new RuntimeException("Wrong meshing type, for this stage of the project. Fix the code here. Type: " + type.name());
		}
	}
	
	@Override
	public int getWholeMeshEntryICount(MeshTypeThing type)
	{
		if(type == MeshTypeThing.Conductor || type == MeshTypeThing.Raycast)
		{
			return model.getFacesCount() * 4 * (2 * 3);
		}
		else
		{
			throw new RuntimeException("Wrong meshing type, for this stage of the project. Fix the code here. Type: " + type.name());
		}
	}
	
	@Override
	public void insertMeshData(float[] vertices, ModelHolder.IntHolder verticesOffset, int[] indices, ModelHolder.IntHolder indicesOffset, ModelHolder.IntHolder vertexCounter, MeshTypeThing type)
	{
		if(type == MeshTypeThing.Conductor || type == MeshTypeThing.Raycast)
		{
			Color color = null;
			if(type.colorISID())
			{
				int id = getRayID();
				int r = id & 0xFF;
				int g = (id & 0xFF00) >> 8;
				int b = (id & 0xFF0000) >> 16;
				color = new Color(r, g, b);
			}
			
			model.generateMeshEntry(this, vertices, verticesOffset, indices, indicesOffset, vertexCounter, color, position, rotation, getParent().getModelHolder().getPlacementOffset(), type);
		}
		else
		{
			throw new RuntimeException("Wrong meshing type, for this stage of the project. Fix the code here. Type: " + type.name());
		}
	}
}

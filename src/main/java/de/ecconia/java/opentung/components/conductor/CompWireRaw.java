package de.ecconia.java.opentung.components.conductor;

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
import de.ecconia.java.opentung.simulation.Cluster;
import de.ecconia.java.opentung.simulation.InheritingCluster;
import de.ecconia.java.opentung.simulation.SimulationManager;
import de.ecconia.java.opentung.simulation.SourceCluster;
import de.ecconia.java.opentung.simulation.Wire;
import de.ecconia.java.opentung.util.math.Vector3;

public class CompWireRaw extends Component implements Wire
{
	public static final ModelHolder modelHolder = new ModelBuilder()
			.setPlacementOffset(new Vector3(0.0, 0.0, 0.0))
			.addConductor(new CubeTunnel(new Vector3(0.0, 0.0, 0.0), new Vector3(0.05, 0.02, 2.0), Direction.ZPos, new ModelMapper()
			{
				@Override
				public Vector3 getMappedSize(Vector3 size, Part component)
				{
					return new Vector3(size.getX(), size.getY(), size.getZ() * ((CompWireRaw) component).getLength() * 0.5f);
				}
			}))
			.build();
	
	@Override
	public ModelHolder getModelHolder()
	{
		return modelHolder;
	}
	
	//### Non-Static ###
	
	private float length;
	//TODO: Remove this redundancy: (Wires do not have to store their powered state, why save it then?)
	private boolean powered;
	
	public CompWireRaw(Component parent)
	{
		super(parent);
	}
	
	public void setPowered(boolean powered)
	{
		this.powered = powered;
	}
	
	public boolean isPowered()
	{
		return powered;
	}
	
	public void setLength(float length)
	{
		this.length = length;
	}
	
	public float getLength()
	{
		return length;
	}
	
	public Vector3 getEnd1()
	{
		Vector3 endPointer = new Vector3(0, 0, length / 2f);
		endPointer = getAlignmentGlobal().inverse().multiply(endPointer);
		return endPointer.add(getPositionGlobal());
	}
	
	public Vector3 getEnd2()
	{
		Vector3 endPointer = new Vector3(0, 0, length / 2f);
		endPointer = getAlignmentGlobal().inverse().multiply(endPointer).invert();
		return endPointer.add(getPositionGlobal());
	}
	
	public void insertMeshData(float[] vertices, ModelHolder.IntHolder verticesIndex, int[] indices, ModelHolder.IntHolder indicesIndex, ModelHolder.IntHolder vertexCounter, MeshTypeThing type)
	{
		//TODO: This is super ungeneric, beware.
		CubeFull shape = (CubeFull) getModelHolder().getConductors().get(0);
		
		shape.generateMeshEntry(this, vertices, verticesIndex, indices, indicesIndex, vertexCounter, Color.circuitON, getPositionGlobal(), getAlignmentGlobal(), modelHolder.getPlacementOffset(), type);
	}
	
	// ### SIMULATION ###
	
	//Connectors which the wire is connected to.
	private Connector connectorA, connectorB;
	
	@Override
	public void setConnectorA(Connector connectorA)
	{
		this.connectorA = connectorA;
	}
	
	@Override
	public Connector getConnectorA()
	{
		return connectorA;
	}
	
	@Override
	public void setConnectorB(Connector connectorB)
	{
		this.connectorB = connectorB;
	}
	
	@Override
	public Connector getConnectorB()
	{
		return connectorB;
	}
	
	@Override
	public Connector getOtherSide(Connector connector)
	{
		return connector == connectorA ? connectorB : connectorA;
	}
	
	//Cluster, stores the cluster which this wire is part of.
	private Cluster cluster;
	
	@Override
	public void setCluster(Cluster cluster)
	{
		this.cluster = cluster;
	}
	
	@Override
	public Cluster getCluster()
	{
		return cluster;
	}
	
	@Override
	public boolean hasCluster()
	{
		return cluster != null;
	}
	
	@Override
	public void leftClicked(SimulationManager simulation)
	{
		if(cluster instanceof SourceCluster)
		{
			System.out.println("Source#" + cluster.hashCode() + " Drains: " + ((SourceCluster) cluster).getDrains().size() + " Active: " + cluster.isActive());
		}
		else
		{
			System.out.println("Drain#" + cluster.hashCode() + " Sources: " + ((InheritingCluster) cluster).getSources().size() + " Active: " + cluster.isActive());
		}
	}
	
	@Override
	public Component copy()
	{
		CompWireRaw copy = new CompWireRaw(null);
		copy.setAlignmentGlobal(alignmentGlobal);
		copy.setPositionGlobal(positionGlobal);
		copy.setLength(length);
		return copy;
	}
}

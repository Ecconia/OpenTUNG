package de.ecconia.java.opentung.components.conductor;

import de.ecconia.java.opentung.components.fragments.CubeTunnel;
import de.ecconia.java.opentung.components.fragments.Direction;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.libwrap.meshes.MeshTypeThing;
import de.ecconia.java.opentung.math.Vector3;
import de.ecconia.java.opentung.simulation.Cluster;

public class CompWireRaw extends Component
{
	public static final ModelHolder modelHolder = new ModelHolder();
	
	static
	{
		modelHolder.setPlacementOffset(new Vector3(0.0, 0.0, 0.0));
		modelHolder.addMeta(new CubeTunnel(new Vector3(0.0, 0.0, 0.0), new Vector3(0.05, 0.02, 2.0), Direction.ZPos));
	}
	
	public static void initGL()
	{
		modelHolder.generateTestModel(ModelHolder.TestModelType.Wire);
	}
	
	@Override
	public ModelHolder getModelHolder()
	{
		return modelHolder;
	}
	
	//### Non-Static ###
	
	private float length;
	private boolean powered;
	
	public CompWireRaw(CompContainer parent)
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
		endPointer = getRotation().inverse().multiply(endPointer);
		return endPointer.add(getPosition());
	}
	
	public Vector3 getEnd2()
	{
		Vector3 endPointer = new Vector3(0, 0, length / 2f);
		endPointer = getRotation().inverse().multiply(endPointer).invert();
		return endPointer.add(getPosition());
	}
	
	public void insertMeshData(float[] vertices, ModelHolder.IntHolder verticesIndex, int[] indices, ModelHolder.IntHolder indicesIndex, ModelHolder.IntHolder vertexCounter, MeshTypeThing type)
	{
		//TODO: This is super ungeneric, beware.
		CubeTunnel shape = (CubeTunnel) getModelHolder().getConductors().get(0);
		
		Vector3 color = new Vector3(1, 0, 0);
		if(type.colorISID())
		{
			int id = getRayID();
			int r = id & 0xFF;
			int g = (id & 0xFF00) >> 8;
			int b = (id & 0xFF0000) >> 16;
			color = new Vector3((float) r / 255f, (float) g / 255f, (float) b / 255f);
		}
		shape.generateWireMeshEntry(vertices, verticesIndex, indices, indicesIndex, vertexCounter, length, color, getPosition(), getRotation(), type);
	}
	
	// ### SIMULATION ###
	
	//Connectors which the wire is connected to.
	private Connector connectorA, connectorB;
	
	public void setConnectorA(Connector connectorA)
	{
		this.connectorA = connectorA;
	}
	
	public Connector getConnectorA()
	{
		return connectorA;
	}
	
	public void setConnectorB(Connector connectorB)
	{
		this.connectorB = connectorB;
	}
	
	public Connector getConnectorB()
	{
		return connectorB;
	}
	
	public Connector getOtherSide(Connector connector)
	{
		return connector == connectorA ? connectorB : connectorA;
	}
	
	//Cluster, stores the cluster which this wire is part of.
	private Cluster cluster;
	
	public void setCluster(Cluster cluster)
	{
		this.cluster = cluster;
	}
	
	public Cluster getCluster()
	{
		return cluster;
	}
	
	public boolean hasCluster()
	{
		return cluster != null;
	}
	
	//Is set, when wire is connected to a Blob with one or two ends.
	private boolean isBlobby;
	
	private void setBlobby()
	{
		isBlobby = true;
	}
	
	public boolean isBlobby()
	{
		return isBlobby;
	}
}

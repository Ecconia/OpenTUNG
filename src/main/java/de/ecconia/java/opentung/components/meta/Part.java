package de.ecconia.java.opentung.components.meta;

import de.ecconia.java.opentung.meshing.MeshTypeThing;
import de.ecconia.java.opentung.util.math.Quaternion;
import de.ecconia.java.opentung.util.math.Vector3;
import de.ecconia.java.opentung.simulation.SimulationManager;

public abstract class Part
{
	//Main data:
	protected Quaternion rotation;
	protected Vector3 position;
	
	//Custom data:
	private Component parent;
	
	public Part(Component parent)
	{
		this.parent = parent;
	}
	
	public PlaceableInfo getInfo()
	{
		return null;
	}
	
	//Meshable section:
	
	public abstract int getWholeMeshEntryVCount(MeshTypeThing type);
	
	public abstract int getWholeMeshEntryICount(MeshTypeThing type);
	
	public abstract void insertMeshData(float[] vertices, ModelHolder.IntHolder verticesOffset, int[] indices, ModelHolder.IntHolder indicesOffset, ModelHolder.IntHolder vertexCounter, MeshTypeThing type);
	
	//Getter/Setter:
	
	public void setPosition(Vector3 position)
	{
		this.position = position;
	}
	
	public Vector3 getPosition()
	{
		return position;
	}
	
	public void setRotation(Quaternion rotation)
	{
		this.rotation = rotation;
	}
	
	public Quaternion getRotation()
	{
		return rotation;
	}
	
	public void setParent(Component parent)
	{
		this.parent = parent;
	}
	
	public Component getParent()
	{
		return parent;
	}
	
	//Interaction:
	
	/**
	 * Warning, executed from InputThread (only call simulation).
	 */
	public void leftClicked(SimulationManager simulation)
	{
	}
}

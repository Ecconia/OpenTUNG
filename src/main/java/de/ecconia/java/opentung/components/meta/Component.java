package de.ecconia.java.opentung.components.meta;

import de.ecconia.java.opentung.MinMaxBox;
import de.ecconia.java.opentung.components.conductor.Blot;
import de.ecconia.java.opentung.components.conductor.Connector;
import de.ecconia.java.opentung.components.conductor.Peg;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.Meshable;
import de.ecconia.java.opentung.libwrap.meshes.MeshTypeThing;
import de.ecconia.java.opentung.math.Quaternion;
import de.ecconia.java.opentung.math.Vector3;
import de.ecconia.java.opentung.simulation.SimulationManager;
import java.util.ArrayList;
import java.util.List;

public abstract class Component
{
	//Bounds:
	protected MinMaxBox connectorBounds;
	
	//Main data:
	private Quaternion rotation;
	private Vector3 position;
	
	//Custom data:
	private CompContainer parent;
	private int rayID;
	
	public Component(CompContainer parent)
	{
		this.parent = parent;
	}
	
	public void setPosition(Vector3 position)
	{
		this.position = position;
	}
	
	public void setRotation(Quaternion rotation)
	{
		this.rotation = rotation;
	}
	
	public Vector3 getPosition()
	{
		return position;
	}
	
	public Quaternion getRotation()
	{
		return rotation;
	}
	
	public void setParent(CompContainer parent)
	{
		this.parent = parent;
	}
	
	public CompContainer getParent()
	{
		return parent;
	}
	
	public abstract ModelHolder getModelHolder();
	
	public void createConnectorBounds()
	{
		for(Peg peg : pegs)
		{
			addConnectorBox(peg.getModel());
		}
		for(Blot blot : blots)
		{
			addConnectorBox(blot.getModel());
		}
	}
	
	protected void addConnectorBox(CubeFull box)
	{
		Vector3 mPosition = box.getPosition().add(getModelHolder().getPlacementOffset());
		Vector3 mSize = box.getSize();
		Vector3 min = mPosition.subtract(mSize);
		Vector3 max = mPosition.add(mSize);
		
		Vector3 a = min;
		Vector3 b = new Vector3(min.getX(), min.getY(), max.getZ());
		Vector3 c = new Vector3(min.getX(), max.getY(), min.getZ());
		Vector3 d = new Vector3(min.getX(), max.getY(), max.getZ());
		Vector3 e = new Vector3(max.getX(), min.getY(), min.getZ());
		Vector3 f = new Vector3(max.getX(), min.getY(), max.getZ());
		Vector3 g = new Vector3(max.getX(), max.getY(), min.getZ());
		Vector3 h = max;
		
		Vector3 position = this.position;
		a = rotation.inverse().multiply(a).add(position);
		b = rotation.inverse().multiply(b).add(position);
		c = rotation.inverse().multiply(c).add(position);
		d = rotation.inverse().multiply(d).add(position);
		e = rotation.inverse().multiply(e).add(position);
		f = rotation.inverse().multiply(f).add(position);
		g = rotation.inverse().multiply(g).add(position);
		h = rotation.inverse().multiply(h).add(position);
		
		if(connectorBounds == null)
		{
			connectorBounds = new MinMaxBox(a);
		}
		else
		{
			connectorBounds.expand(a);
		}
		connectorBounds.expand(b);
		connectorBounds.expand(c);
		connectorBounds.expand(d);
		connectorBounds.expand(e);
		connectorBounds.expand(f);
		connectorBounds.expand(g);
		connectorBounds.expand(h);
	}
	
	public Connector getConnectorAt(String debug, Vector3 absolutePoint)
	{
		if(connectorBounds == null || !connectorBounds.contains(absolutePoint))
		{
			return null;
		}
		
		Vector3 localPoint = rotation.multiply(absolutePoint.subtract(position)).subtract(getModelHolder().getPlacementOffset());
		for(Peg peg : pegs)
		{
			if(peg.contains(localPoint))
			{
				return peg;
			}
		}
		for(Blot blot : blots)
		{
			if(blot.contains(localPoint))
			{
				return blot;
			}
		}
		return null;
	}
	
	public void setRayCastID(int id)
	{
		this.rayID = id;
	}
	
	public int getRayID()
	{
		return rayID;
	}
	
	//Meshable section:
	
	public int getWholeMeshEntryVCount(MeshTypeThing type)
	{
		int attributeAmount;
		if(type == MeshTypeThing.Conductor || type == MeshTypeThing.Display)
		{
			attributeAmount = 3 + 3; //Position + Normal
		}
		else if(type == MeshTypeThing.Raycast)
		{
			attributeAmount = 3 + 3; //Position + Color
		}
		else if(type == MeshTypeThing.Solid)
		{
			attributeAmount = 3 + 3 + 3; //Position + Normal + Color
		}
		else
		{
			throw new RuntimeException("Wrong meshing type, for this stage of the project. Fix the code here.");
		}
		
		int amount = 0;
		if(type == MeshTypeThing.Conductor)
		{
			for(Peg peg : pegs)
			{
				amount += peg.getModel().getFacesCount() * 4 * attributeAmount;
			}
			for(Blot blot : blots)
			{
				amount += blot.getModel().getFacesCount() * 4 * attributeAmount;
			}
		}
		else if(type == MeshTypeThing.Display)
		{
			for(Meshable m : getModelHolder().getColorables())
			{
				amount += ((CubeFull) m).getFacesCount() * 4 * attributeAmount;
			}
		}
		else
		{
			for(Meshable m : getModelHolder().getSolid())
			{
				amount += getModelHolder().getSolid().size() * ((CubeFull) m).getFacesCount() * 4 * attributeAmount;
			}
			
			if(type == MeshTypeThing.Raycast)
			{
				for(Peg peg : pegs)
				{
					amount += peg.getModel().getFacesCount() * 4 * attributeAmount;
				}
				for(Blot blot : blots)
				{
					amount += blot.getModel().getFacesCount() * 4 * attributeAmount;
				}
				for(Meshable m : getModelHolder().getColorables())
				{
					amount += ((CubeFull) m).getFacesCount() * 4 * attributeAmount;
				}
			}
		}
		
		return amount;
	}
	
	public int getWholeMeshEntryICount(MeshTypeThing type)
	{
		if(!(type == MeshTypeThing.Raycast || type == MeshTypeThing.Solid || type == MeshTypeThing.Conductor || type == MeshTypeThing.Display))
		{
			throw new RuntimeException("Wrong meshing type, for this stage of the project. Fix the code here.");
		}
		
		int amount = 0;
		if(type == MeshTypeThing.Conductor)
		{
			for(Peg peg : pegs)
			{
				amount += peg.getModel().getFacesCount() * (2 * 3);
			}
			for(Blot blot : blots)
			{
				amount += blot.getModel().getFacesCount() * (2 * 3);
			}
		}
		else if(type == MeshTypeThing.Display)
		{
			for(Meshable m : getModelHolder().getColorables())
			{
				amount += ((CubeFull) m).getFacesCount() * (2 * 3);
			}
		}
		else
		{
			for(Meshable m : getModelHolder().getSolid())
			{
				amount += ((CubeFull) m).getFacesCount() * (2 * 3);
			}
			
			if(type == MeshTypeThing.Raycast)
			{
				for(Peg peg : pegs)
				{
					amount += peg.getModel().getFacesCount() * (2 * 3);
				}
				for(Blot blot : blots)
				{
					amount += blot.getModel().getFacesCount() * (2 * 3);
				}
				for(Meshable m : getModelHolder().getColorables())
				{
					amount += ((CubeFull) m).getFacesCount() * (2 * 3);
				}
			}
		}
		
		return amount;
	}
	
	public void insertMeshData(float[] vertices, ModelHolder.IntHolder verticesOffset, int[] indices, ModelHolder.IntHolder indicesOffset, ModelHolder.IntHolder vertexCounter, MeshTypeThing type)
	{
		if(type == MeshTypeThing.Conductor)
		{
			for(Peg peg : pegs)
			{
				peg.getModel().generateMeshEntry(vertices, verticesOffset, indices, indicesOffset, vertexCounter, null, position, rotation, getModelHolder().getPlacementOffset(), type);
			}
			for(Blot blot : blots)
			{
				blot.getModel().generateMeshEntry(vertices, verticesOffset, indices, indicesOffset, vertexCounter, null, position, rotation, getModelHolder().getPlacementOffset(), type);
			}
		}
		else if(type == MeshTypeThing.Display)
		{
			for(Meshable m : getModelHolder().getColorables())
			{
				((CubeFull) m).generateMeshEntry(vertices, verticesOffset, indices, indicesOffset, vertexCounter, null, position, rotation, getModelHolder().getPlacementOffset(), type);
			}
		}
		else if(type == MeshTypeThing.Raycast || type == MeshTypeThing.Solid)
		{
			Vector3 color = null;
			if(type.colorISID())
			{
				int id = getRayID();
				int r = id & 0xFF;
				int g = (id & 0xFF00) >> 8;
				int b = (id & 0xFF0000) >> 16;
				color = new Vector3((float) r / 255f, (float) g / 255f, (float) b / 255f);
			}
			
			for(Meshable m : getModelHolder().getSolid())
			{
				((CubeFull) m).generateMeshEntry(vertices, verticesOffset, indices, indicesOffset, vertexCounter, color, position, rotation, getModelHolder().getPlacementOffset(), type);
			}
			
			if(type == MeshTypeThing.Raycast)
			{
				for(Peg peg : pegs)
				{
					peg.getModel().generateMeshEntry(vertices, verticesOffset, indices, indicesOffset, vertexCounter, color, position, rotation, getModelHolder().getPlacementOffset(), type);
				}
				for(Blot blot : blots)
				{
					blot.getModel().generateMeshEntry(vertices, verticesOffset, indices, indicesOffset, vertexCounter, color, position, rotation, getModelHolder().getPlacementOffset(), type);
				}
				for(Meshable m : getModelHolder().getColorables())
				{
					((CubeFull) m).generateMeshEntry(vertices, verticesOffset, indices, indicesOffset, vertexCounter, color, position, rotation, getModelHolder().getPlacementOffset(), type);
				}
			}
		}
		else
		{
			throw new RuntimeException("Wrong meshing type, for this stage of the project. Fix the code here.");
		}
	}
	
	//Conductors:
	
	protected final List<Peg> pegs = new ArrayList<>();
	protected final List<Blot> blots = new ArrayList<>();
	
	public List<Peg> getPegs()
	{
		return pegs;
	}
	
	public List<Blot> getBlots()
	{
		return blots;
	}
	
	//Interaction:
	
	/**
	 * Warning, executed from InputThread (only call simulation).
	 */
	public void rightClicked(SimulationManager simulation)
	{
	}
}

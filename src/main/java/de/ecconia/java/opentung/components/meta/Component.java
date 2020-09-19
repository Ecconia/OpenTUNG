package de.ecconia.java.opentung.components.meta;

import de.ecconia.java.opentung.MinMaxBox;
import de.ecconia.java.opentung.components.conductor.Blot;
import de.ecconia.java.opentung.components.conductor.Connector;
import de.ecconia.java.opentung.components.conductor.Peg;
import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.CubeOpenRotated;
import de.ecconia.java.opentung.components.fragments.Meshable;
import de.ecconia.java.opentung.libwrap.meshes.MeshTypeThing;
import de.ecconia.java.opentung.math.Quaternion;
import de.ecconia.java.opentung.math.Vector3;
import java.util.ArrayList;
import java.util.List;

public abstract class Component extends Part
{
	//Bounds:
	protected MinMaxBox connectorBounds;
	
	//Connector:
	protected final List<Peg> pegs = new ArrayList<>();
	protected final List<Blot> blots = new ArrayList<>();
	
	public Component(Component parent)
	{
		super(parent);
		for(CubeFull cube : getModelHolder().getPegModels())
		{
			pegs.add(new Peg(this, cube));
		}
		List<CubeFull> blotModels = getModelHolder().getBlotModels();
		for(int i = 0; i < blotModels.size(); i++)
		{
			CubeFull cube = blotModels.get(i);
			blots.add(new Blot(this, i, cube));
		}
	}
	
	//TODO: Find a better long-term for this code. Currently the peg's get the injection from here.
	//^This is dependant on how interaction uses the methods below.
	@Override
	public void setPosition(Vector3 position)
	{
		super.setPosition(position);
		for(Peg peg : pegs)
		{
			peg.setPosition(position);
		}
		for(Blot blot : blots)
		{
			blot.setPosition(position);
		}
	}
	
	@Override
	public void setRotation(Quaternion rotation)
	{
		super.setRotation(rotation);
		for(Peg peg : pegs)
		{
			peg.setRotation(rotation);
		}
		for(Blot blot : blots)
		{
			blot.setRotation(rotation);
		}
	}
	
	public List<Peg> getPegs()
	{
		return pegs;
	}
	
	public List<Blot> getBlots()
	{
		return blots;
	}
	
	//ModelHolder getter:
	
	public abstract ModelHolder getModelHolder();
	
	public void init()
	{
		//Can be used my components to setup internal wires.
	}
	
	//Meshable section:
	
	@Override
	public int getWholeMeshEntryVCount(MeshTypeThing type)
	{
		if(!(type == MeshTypeThing.Raycast || type == MeshTypeThing.Solid || type == MeshTypeThing.Display))
		{
			throw new RuntimeException("Wrong meshing type, for this stage of the project. Fix the code here.");
		}
		
		int attributeAmount = type.getFloatCount();
		int amount = 0;
		if(type == MeshTypeThing.Display)
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
				for(Meshable m : getModelHolder().getColorables())
				{
					amount += ((CubeFull) m).getFacesCount() * 4 * attributeAmount;
				}
			}
		}
		
		return amount;
	}
	
	@Override
	public int getWholeMeshEntryICount(MeshTypeThing type)
	{
		if(!(type == MeshTypeThing.Raycast || type == MeshTypeThing.Solid || type == MeshTypeThing.Display))
		{
			throw new RuntimeException("Wrong meshing type, for this stage of the project. Fix the code here.");
		}
		
		int amount = 0;
		if(type == MeshTypeThing.Display)
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
				for(Meshable m : getModelHolder().getColorables())
				{
					amount += ((CubeFull) m).getFacesCount() * (2 * 3);
				}
			}
		}
		
		return amount;
	}
	
	@Override
	public void insertMeshData(float[] vertices, ModelHolder.IntHolder verticesOffset, int[] indices, ModelHolder.IntHolder indicesOffset, ModelHolder.IntHolder vertexCounter, MeshTypeThing type)
	{
		if(type == MeshTypeThing.Display)
		{
			for(Meshable m : getModelHolder().getColorables())
			{
				((CubeFull) m).generateMeshEntry(this, vertices, verticesOffset, indices, indicesOffset, vertexCounter, null, position, rotation, getModelHolder().getPlacementOffset(), type);
			}
		}
		else if(type == MeshTypeThing.Raycast || type == MeshTypeThing.Solid)
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
			
			for(Meshable m : getModelHolder().getSolid())
			{
				((CubeFull) m).generateMeshEntry(this, vertices, verticesOffset, indices, indicesOffset, vertexCounter, color, position, rotation, getModelHolder().getPlacementOffset(), type);
			}
			
			if(type == MeshTypeThing.Raycast)
			{
				for(Meshable m : getModelHolder().getColorables())
				{
					((CubeFull) m).generateMeshEntry(this, vertices, verticesOffset, indices, indicesOffset, vertexCounter, color, position, rotation, getModelHolder().getPlacementOffset(), type);
				}
			}
		}
		else
		{
			throw new RuntimeException("Wrong meshing type, for this stage of the project. Fix the code here.");
		}
	}
	
	//### non property ###
	
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
		Vector3 mPosition = box.getPosition();
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
		
		if(box instanceof CubeOpenRotated)
		{
			Quaternion rotation = ((CubeOpenRotated) box).getRotation();
			
			a = rotation.multiply(a);
			b = rotation.multiply(b);
			c = rotation.multiply(c);
			d = rotation.multiply(d);
			e = rotation.multiply(e);
			f = rotation.multiply(f);
			g = rotation.multiply(g);
			h = rotation.multiply(h);
		}
		
		a = a.add(getModelHolder().getPlacementOffset());
		b = b.add(getModelHolder().getPlacementOffset());
		c = c.add(getModelHolder().getPlacementOffset());
		d = d.add(getModelHolder().getPlacementOffset());
		e = e.add(getModelHolder().getPlacementOffset());
		f = f.add(getModelHolder().getPlacementOffset());
		g = g.add(getModelHolder().getPlacementOffset());
		h = h.add(getModelHolder().getPlacementOffset());
		
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
}

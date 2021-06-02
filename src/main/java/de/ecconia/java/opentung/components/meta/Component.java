package de.ecconia.java.opentung.components.meta;

import de.ecconia.java.opentung.components.conductor.Connector;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.CubeOpenRotated;
import de.ecconia.java.opentung.components.fragments.Meshable;
import de.ecconia.java.opentung.meshing.ColorMeshBag;
import de.ecconia.java.opentung.meshing.ConductorMeshBag;
import de.ecconia.java.opentung.meshing.MeshBag;
import de.ecconia.java.opentung.meshing.MeshTypeThing;
import de.ecconia.java.opentung.util.MinMaxBox;
import de.ecconia.java.opentung.util.math.Quaternion;
import de.ecconia.java.opentung.util.math.Vector3;

public abstract class Component extends Part
{
	//Bounds:
	private MinMaxBox ownBounds;
	//Every component has connector bounds, a LogicComponent has connectors. And a container might hold LogicComponents.
	protected MinMaxBox connectorBounds;
	
	public MinMaxBox getBounds()
	{
		return getOwnBounds();
	}
	
	public MinMaxBox getOwnBounds()
	{
		if(ownBounds == null)
		{
			createOwnBounds();
		}
		
		return ownBounds;
	}
	
	public void updateBoundsDeep()
	{
		createOwnBounds();
	}
	
	public Component(Component parent)
	{
		super(parent);
	}
	
	//ModelHolder getter:
	
	public abstract ModelHolder getModelHolder();
	
	public void init()
	{
		//Can be used my components to setup internal wires.
	}
	
	public void initClusters()
	{
		//Used when copying components and creating new components (but not on save-file load).
	}
	
	//Meshable section:
	
	@Override
	public int getWholeMeshEntryVCount(MeshTypeThing type)
	{
		if(!(type == MeshTypeThing.Solid || type == MeshTypeThing.Display))
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
		}
		
		return amount;
	}
	
	@Override
	public int getWholeMeshEntryICount(MeshTypeThing type)
	{
		if(!(type == MeshTypeThing.Solid || type == MeshTypeThing.Display))
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
		else if(type == MeshTypeThing.Solid)
		{
			for(Meshable m : getModelHolder().getSolid())
			{
				((CubeFull) m).generateMeshEntry(this, vertices, verticesOffset, indices, indicesOffset, vertexCounter, null, position, rotation, getModelHolder().getPlacementOffset(), type);
			}
		}
		else
		{
			throw new RuntimeException("Wrong meshing type, for this stage of the project. Fix the code here.");
		}
	}
	
	//### non property ###
	
	public void createOwnBounds()
	{
		ownBounds = null; //Reset and don't expand it further.
		for(Meshable m : getModelHolder().getSolid())
		{
			ownBounds = expandMinMaxBox(ownBounds, (CubeFull) m);
		}
		for(Meshable m : getModelHolder().getColorables())
		{
			ownBounds = expandMinMaxBox(ownBounds, (CubeFull) m);
		}
		for(Meshable m : getModelHolder().getConductors())
		{
			ownBounds = expandMinMaxBox(ownBounds, (CubeFull) m);
		}
	}
	
	protected MinMaxBox expandMinMaxBox(MinMaxBox mmBox, CubeFull box)
	{
		Vector3 mPosition = box.getPosition();
		Vector3 mSize = box.getSize();
		if(box.getMapper() != null)
		{
			mSize = box.getMapper().getMappedSize(mSize, this);
		}
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
		
		if(mmBox == null)
		{
			mmBox = new MinMaxBox(a);
		}
		else
		{
			mmBox.expand(a);
		}
		mmBox.expand(b);
		mmBox.expand(c);
		mmBox.expand(d);
		mmBox.expand(e);
		mmBox.expand(f);
		mmBox.expand(g);
		mmBox.expand(h);
		
		return mmBox;
	}
	
	public void createConnectorBounds()
	{
		//By default a component has no connectors, thus no bounds for them.
	}
	
	public Connector getConnectorAt(Vector3 absolutePoint)
	{
		return null; //By default a component has no connectors, thus find none.
	}
	
	//Meshing:
	
	private MeshBag solidMeshBag;
	
	public MeshBag getSolidMeshBag()
	{
		return solidMeshBag;
	}
	
	public void setSolidMeshBag(MeshBag solidMeshBag)
	{
		this.solidMeshBag = solidMeshBag;
	}
	
	private ConductorMeshBag conductorMeshBag;
	
	public ConductorMeshBag getConductorMeshBag()
	{
		return conductorMeshBag;
	}
	
	public void setConductorMeshBag(ConductorMeshBag conductorMeshBag)
	{
		this.conductorMeshBag = conductorMeshBag;
	}
	
	private ColorMeshBag colorMeshBag;
	
	public ColorMeshBag getColorMeshBag()
	{
		return colorMeshBag;
	}
	
	public void setColorMeshBag(ColorMeshBag colorMeshBag)
	{
		this.colorMeshBag = colorMeshBag;
	}
	
	public Component copy()
	{
		Component copy = getInfo().instance(null);
		copy.setRotation(rotation);
		copy.setPosition(position);
		copy.init();
		return copy;
	}
}

package de.ecconia.java.opentung.components.meta;

import de.ecconia.java.opentung.MinMaxBox;
import de.ecconia.java.opentung.Port;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.Meshable;
import de.ecconia.java.opentung.math.Quaternion;
import de.ecconia.java.opentung.math.Vector3;

public abstract class Component
{
	//Bounds:
	protected MinMaxBox connectorBounds;
	
	//Main data:
	private Quaternion rotation;
	private Vector3 position;
	
	//Custom data:
	private CompContainer parent;
	
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
		for(Meshable m : getModelHolder().getConnectors())
		{
			addConnectorBox((CubeFull) m);
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
	
	public Port getPortAt(String debug, Vector3 absolutePoint)
	{
		if(connectorBounds == null || !connectorBounds.contains(absolutePoint))
		{
			return null;
		}
		
		Vector3 localPoint = rotation.multiply(absolutePoint.subtract(position)).subtract(getModelHolder().getPlacementOffset());
		for(int i = 0; i < getModelHolder().getConnectors().size(); i++)
		{
			CubeFull cube = (CubeFull) getModelHolder().getConnectors().get(i);
			if(cube.contains(localPoint))
			{
				return new Port(this, i);
			}
		}
		return null;
	}
}

package de.ecconia.java.opentung.components.meta;

import de.ecconia.java.opentung.components.conductor.CompWireRaw;
import de.ecconia.java.opentung.components.conductor.Connector;
import de.ecconia.java.opentung.util.MinMaxBox;
import de.ecconia.java.opentung.util.math.Vector3;
import java.util.ArrayList;
import java.util.List;

public abstract class CompContainer extends Component
{
	//Bounds:
	private MinMaxBox bounds;
	
	@Override
	public MinMaxBox getBounds()
	{
		if(bounds == null)
		{
			createBounds();
		}
		
		return bounds;
	}
	
	public void updateBounds()
	{
		createBounds(); //Updates the bounds.
		Component parent = getParent();
		if(parent != null)
		{
			//Cast, cause if this is not a part, this must be a container.
			((CompContainer) parent).updateBounds();
		}
	}
	
	@Override
	public void updateBoundsDeep()
	{
		createOwnBounds();
		bounds = new MinMaxBox(getOwnBounds());
		for(Component child : children)
		{
			child.updateBoundsDeep();
			bounds.expand(child.getBounds());
		}
	}
	
	//Raw data:
	private final List<Component> children = new ArrayList<>();
	
	public CompContainer(CompContainer parent)
	{
		super(parent);
	}
	
	public List<Component> getChildren()
	{
		return children;
	}
	
	public void addChild(Component child)
	{
		children.add(child);
	}
	
	@Override
	public void createConnectorBounds()
	{
		for(Component child : children)
		{
			if(child instanceof CompWireRaw)
			{
				continue;
			}
			
			child.createConnectorBounds();
			if(child.connectorBounds == null)
			{
				continue;
			}
			
			if(connectorBounds == null)
			{
				connectorBounds = new MinMaxBox(child.connectorBounds);
			}
			else
			{
				connectorBounds.expand(child.connectorBounds);
			}
		}
	}
	
	@Override
	public Connector getConnectorAt(Vector3 absolutePoint)
	{
		if(connectorBounds == null || !connectorBounds.contains(absolutePoint))
		{
			return null;
		}
		
		Connector foundConnector = null;
		for(Component child : children)
		{
			if(!(child instanceof CompWireRaw))
			{
				Connector curConnector = child.getConnectorAt(absolutePoint);
				if(curConnector != null)
				{
					if(foundConnector != null)
					{
						throw new RuntimeException("Could not import TungBoard, two child components claim to have a port for wire end.");
					}
					else
					{
						foundConnector = curConnector;
					}
				}
			}
		}
		
		return foundConnector;
	}
	
	public void remove(Component component)
	{
		children.remove(component);
		//TODO: recalculate bounds, if needed.
	}
	
	public void createBounds()
	{
		bounds = new MinMaxBox(getOwnBounds());
		for(Component child : children)
		{
			bounds.expand(child.getBounds());
		}
	}
	
	public boolean isEmpty()
	{
		return children.isEmpty();
	}
}

package de.ecconia.java.opentung.components.meta;

import de.ecconia.java.opentung.MinMaxBox;
import de.ecconia.java.opentung.components.CompSnappingPeg;
import de.ecconia.java.opentung.components.conductor.CompWireRaw;
import de.ecconia.java.opentung.components.conductor.Connector;
import de.ecconia.java.opentung.math.Vector3;
import java.util.ArrayList;
import java.util.List;

public abstract class CompContainer extends Component
{
	//Bounds:
	protected MinMaxBox snappingPegBounds;
	
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
	
	public void createSnappingPegBounds()
	{
		for(Component child : children)
		{
			if(child instanceof CompSnappingPeg)
			{
				CompSnappingPeg peg = (CompSnappingPeg) child;
				
				peg.createSnappingPegBounds();
				if(snappingPegBounds == null)
				{
					snappingPegBounds = new MinMaxBox(peg.getSnappingPegBounds());
				}
				else
				{
					snappingPegBounds.expand(peg.getSnappingPegBounds());
				}
			}
			else if(child instanceof CompContainer)
			{
				CompContainer cont = (CompContainer) child;
				
				cont.createSnappingPegBounds();
				if(cont.snappingPegBounds != null)
				{
					if(snappingPegBounds == null)
					{
						snappingPegBounds = new MinMaxBox(cont.snappingPegBounds);
					}
					else
					{
						snappingPegBounds.expand(cont.snappingPegBounds);
					}
				}
			}
		}
	}
	
	public void getSnappingPegsAt(Vector3 absolutePoint, List<CompSnappingPeg> collector)
	{
		if(snappingPegBounds == null || !snappingPegBounds.contains(absolutePoint))
		{
			return;
		}
		
		for(Component child : children)
		{
			//TODO: Add interface.
			if(child instanceof CompSnappingPeg)
			{
				((CompSnappingPeg) child).getSnappingPegsAt(absolutePoint, collector);
			}
			else if(child instanceof CompContainer)
			{
				((CompContainer) child).getSnappingPegsAt(absolutePoint, collector);
			}
		}
	}
	
	public Connector getConnectorAt(String debug, Vector3 absolutePoint)
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
				Connector curConnector = child.getConnectorAt(debug + "  ", absolutePoint);
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
	
	public boolean isEmpty()
	{
		return children.isEmpty();
	}
}

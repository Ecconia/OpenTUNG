package de.ecconia.java.opentung.components.meta;

import de.ecconia.java.opentung.MinMaxBox;
import de.ecconia.java.opentung.Port;
import de.ecconia.java.opentung.components.CompWireRaw;
import de.ecconia.java.opentung.math.Vector3;
import java.util.ArrayList;
import java.util.List;

public abstract class CompContainer extends Component
{
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
	
	public Port getPortAt(String debug, Vector3 absolutePoint)
	{
		if(connectorBounds == null || !connectorBounds.contains(absolutePoint))
		{
			return null;
		}
		
		Port foundPort = null;
		for(Component child : children)
		{
			if(!(child instanceof CompWireRaw))
			{
				Port curPort = child.getPortAt(debug + "  ", absolutePoint);
				if(curPort != null)
				{
					if(foundPort != null)
					{
						throw new RuntimeException("Could not import TungBoard, two child components claim to have a port for wire end.");
					}
					else
					{
						foundPort = curPort;
					}
				}
			}
		}
		
		return foundPort;
	}
}

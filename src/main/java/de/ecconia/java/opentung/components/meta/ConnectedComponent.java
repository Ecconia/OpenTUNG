package de.ecconia.java.opentung.components.meta;

import de.ecconia.java.opentung.components.conductor.Blot;
import de.ecconia.java.opentung.components.conductor.Connector;
import de.ecconia.java.opentung.components.conductor.Peg;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.simulation.Cluster;
import de.ecconia.java.opentung.simulation.InheritingCluster;
import de.ecconia.java.opentung.simulation.SourceCluster;
import de.ecconia.java.opentung.util.math.Quaternion;
import de.ecconia.java.opentung.util.math.Vector3;
import java.util.ArrayList;
import java.util.List;

public abstract class ConnectedComponent extends Component
{
	//Connector:
	protected final List<Peg> pegs = new ArrayList<>();
	protected final List<Blot> blots = new ArrayList<>();
	//Stores all pegs first, then all blots:
	protected final List<Connector> connectors = new ArrayList<>();
	
	public ConnectedComponent(Component parent)
	{
		super(parent);
		
		for(CubeFull cube : getModelHolder().getPegModels())
		{
			Peg peg = new Peg(this, cube);
			pegs.add(peg);
			connectors.add(peg);
		}
		List<CubeFull> blotModels = getModelHolder().getBlotModels();
		for(int i = 0; i < blotModels.size(); i++)
		{
			CubeFull cube = blotModels.get(i);
			Blot blot = new Blot(this, i, cube);
			blots.add(blot);
			connectors.add(blot);
		}
	}
	
	@Override
	public void initClusters()
	{
		//TBI: This form of initialization more the whole initial cluster situation onto the components.
		// Works well for default components. But modders would have to write that too...
		for(Peg peg : pegs)
		{
			Cluster cluster = new InheritingCluster();
			cluster.addConnector(peg);
			peg.setCluster(cluster);
		}
		for(Blot blot : blots)
		{
			Cluster cluster = new SourceCluster(blot);
			cluster.addConnector(blot);
			blot.setCluster(cluster);
		}
	}
	
	//TODO: Find a better long-term for this code. Currently the peg's get the injection from here.
	//^This is dependant on how interaction uses the methods below.
	@Override
	public void setPositionGlobal(Vector3 position)
	{
		super.setPositionGlobal(position);
		for(Connector connector : connectors)
		{
			connector.setPositionGlobal(position);
		}
	}
	
	@Override
	public void setAlignmentGlobal(Quaternion rotation)
	{
		super.setAlignmentGlobal(rotation);
		for(Connector connector : connectors)
		{
			connector.setAlignmentGlobal(rotation);
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
	
	public List<Connector> getConnectors()
	{
		return connectors;
	}
	
	//Connector bounds code:
	
	@Override
	public void createConnectorBounds()
	{
		for(Connector connector : connectors)
		{
			connectorBounds = expandMinMaxBox(connectorBounds, connector.getModel());
		}
	}
	
	@Override
	public Connector getConnectorAt(Vector3 absolutePoint)
	{
		if(connectorBounds == null || !connectorBounds.contains(absolutePoint))
		{
			return null;
		}
		
		Vector3 localPoint = alignmentGlobal.multiply(absolutePoint.subtract(positionGlobal)).subtract(getModelHolder().getPlacementOffset());
		for(Connector connector : connectors)
		{
			if(connector.contains(localPoint))
			{
				return connector;
			}
		}
		return null;
	}
}

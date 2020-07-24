package de.ecconia.java.opentung.simulation;

import de.ecconia.java.opentung.BoardUniverse;
import de.ecconia.java.opentung.components.conductor.Blot;
import de.ecconia.java.opentung.components.conductor.Connector;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class WireHelper
{
	private static final int SourceBlot = 0;
	private static final int SourceActive = 1;
	private static final int DrainOFF = 2;
	private static final int DrainActive = 3;
	
	public static void placeWire(SimulationManager simulation, BoardUniverse board, Connector connectorA, Connector connectorB, Wire newWire)
	{
		Cluster clusterA = connectorA.getCluster();
		Cluster clusterB = connectorB.getCluster();
		
		newWire.setConnectorA(connectorA);
		newWire.setConnectorB(connectorB);
		connectorA.addWire(newWire);
		connectorB.addWire(newWire);
		
		//Both clusters are the same, trivial case:
		//Note: Two blots are never in the same cluster.
		if(clusterA == clusterB)
		{
			newWire.setCluster(clusterA);
			clusterA.addWire(newWire);
			
			return;
		}
		//Two different cluster types:
		
		Cluster wireCluster = null;
		
		int typeA = getClusterType(connectorA, clusterA);
		int typeB = getClusterType(connectorB, clusterB);
		switch(typeA * 4 + typeB)
		{
			case SourceBlot * 4 + SourceBlot:
			{
				//Don't allow this connection.
				System.out.println("Blot-Blot connections are not allowed, cause pointless.");
				return;
			}
			case SourceBlot * 4 + SourceActive:
			{
				wireCluster = pwSourceBlotAndSourceActive(board, simulation, connectorA, connectorB);
				break;
			}
			case SourceBlot * 4 + DrainOFF:
			{
				wireCluster = pwSourceAndDrainOFF(board, simulation, connectorA, connectorB);
				break;
			}
			case SourceBlot * 4 + DrainActive:
			{
				wireCluster = pwSourceBlotAndDrainActive(board, simulation, connectorA, connectorB);
				break;
			}
			case SourceActive * 4 + SourceBlot:
			{
				wireCluster = pwSourceBlotAndSourceActive(board, simulation, connectorB, connectorA);
				break;
			}
			case SourceActive * 4 + SourceActive:
			{
				wireCluster = pwSourceActiveAndSourceActive(board, simulation, connectorA, connectorB);
				break;
			}
			case SourceActive * 4 + DrainOFF:
			{
				wireCluster = pwSourceAndDrainOFF(board, simulation, connectorA, connectorB);
				break;
			}
			case SourceActive * 4 + DrainActive:
			{
				wireCluster = pwSourceActiveAndDrainActive(board, simulation, connectorA, connectorB);
				break;
			}
			case DrainOFF * 4 + SourceBlot:
			{
				wireCluster = pwSourceAndDrainOFF(board, simulation, connectorB, connectorA);
				break;
			}
			case DrainOFF * 4 + SourceActive:
			{
				wireCluster = pwSourceAndDrainOFF(board, simulation, connectorB, connectorA);
				break;
			}
			case DrainOFF * 4 + DrainOFF:
			{
				//Append A to B (deletes A)
				board.deleteCluster(clusterB.getId());
				Prototype merge = new Prototype(clusterB);
				merge.mergeInto(clusterA);
				wireCluster = clusterA;
				break;
			}
			case DrainOFF * 4 + DrainActive:
			{
				wireCluster = pwDrainOffAndDrainActive(board, simulation, connectorA, connectorB);
				break;
			}
			case DrainActive * 4 + SourceBlot:
			{
				wireCluster = pwSourceBlotAndDrainActive(board, simulation, connectorB, connectorA);
				break;
			}
			case DrainActive * 4 + SourceActive:
			{
				wireCluster = pwSourceActiveAndDrainActive(board, simulation, connectorB, connectorA);
				break;
			}
			case DrainActive * 4 + DrainOFF:
			{
				wireCluster = pwDrainOffAndDrainActive(board, simulation, connectorB, connectorA);
				break;
			}
			case DrainActive * 4 + DrainActive:
			{
				wireCluster = pwDrainActiveAndDrainActive(board, simulation, connectorA, connectorB);
				break;
			}
		}
		
		newWire.setCluster(wireCluster);
		wireCluster.addWire(newWire);
	}
	
	private static Cluster pwDrainActiveAndDrainActive(BoardUniverse board, SimulationManager simulation, Connector drainActive1, Connector drainActive2)
	{
		InheritingCluster drainActive1Cluster = (InheritingCluster) drainActive1.getCluster();
		InheritingCluster drainActive2Cluster = (InheritingCluster) drainActive2.getCluster();
		
		List<Connector> oldConnectors = new ArrayList<>(drainActive2Cluster.getConnectors());
		boolean on1 = drainActive1Cluster.isActive();
		boolean on2 = drainActive2Cluster.isActive();
		
		//Merge A and B
		for(SourceCluster source : drainActive1Cluster.getSources())
		{
			source.removeDrain(drainActive1Cluster);
			source.addDrain(drainActive2Cluster);
			drainActive2Cluster.addSource(source);
			if(source.isActive())
			{
				drainActive2Cluster.oneIn(simulation);
			}
		}
		Prototype merge = splitNonSourcePartFromCluster(drainActive1);
		merge.mergeInto(drainActive2Cluster);
		
		//If just one of A/B is ON, then update the other
		if(on1 != on2)
		{
			if(on1)
			{
				for(Connector connector : oldConnectors)
				{
					if(connector.getParent() instanceof Updateable)
					{
						simulation.updateNextTick((Updateable) connector.getParent());
					}
				}
			}
			else
			{
				merge.scheduleUpdateable(simulation);
			}
		}
		
		return drainActive1Cluster;
	}
	
	private static Cluster pwDrainOffAndDrainActive(BoardUniverse board, SimulationManager simulation, Connector drainOff, Connector drainActive)
	{
		InheritingCluster drainActiveCluster = (InheritingCluster) drainActive.getCluster();
		InheritingCluster drainOffCluster = (InheritingCluster) drainOff.getCluster();
		
		//Append A to B (deletes A)
		Prototype merge = new Prototype(drainOffCluster);
		merge.mergeInto(drainActiveCluster);
		
		//If B is ON, update A
		if(drainActiveCluster.isActive())
		{
			merge.scheduleUpdateable(simulation);
		}
		
		return drainActiveCluster;
	}
	
	private static Cluster pwSourceActiveAndDrainActive(BoardUniverse board, SimulationManager simulation, Connector sourceActive, Connector drainActive)
	{
		SourceCluster sourceActiveCluster = (SourceCluster) sourceActive.getCluster();
		InheritingCluster drainActiveCluster = (InheritingCluster) drainActive.getCluster();
		
		//Split A off its Blot, append it to B
		Prototype splitA = splitNonSourcePartFromCluster(sourceActive);
		splitA.mergeInto(drainActiveCluster);
		
		//Add A as source to B
		drainActiveCluster.addSource(sourceActiveCluster);
		sourceActiveCluster.addDrain(drainActiveCluster);
		
		//If just one of A/B is ON, then update the other
		if(sourceActiveCluster.isActive())
		{
			drainActiveCluster.oneIn(simulation);
		}
		else if(drainActiveCluster.isActive())
		{
			splitA.scheduleUpdateable(simulation);
		}
		
		return drainActiveCluster;
	}
	
	private static Cluster pwSourceActiveAndSourceActive(BoardUniverse board, SimulationManager simulation, Connector sourceActive1, Connector sourceActive2)
	{
		SourceCluster sourceActive1Cluster = (SourceCluster) sourceActive1.getCluster();
		SourceCluster sourceActive2Cluster = (SourceCluster) sourceActive2.getCluster();
		
		//Split A and B off their Blots and make their merge a Cluster C with A and B as source
		InheritingCluster newCluster = new InheritingCluster(board.getNewClusterID());
		newCluster.addSource(sourceActive1Cluster);
		newCluster.addSource(sourceActive2Cluster);
		sourceActive1Cluster.addDrain(newCluster);
		sourceActive2Cluster.addDrain(newCluster);
		
		Prototype splitA = splitNonSourcePartFromCluster(sourceActive1);
		Prototype splitB = splitNonSourcePartFromCluster(sourceActive2);
		splitA.mergeInto(newCluster);
		splitB.mergeInto(newCluster);
		
		//Update C according to its sources
		if(sourceActive1Cluster.isActive())
		{
			newCluster.oneIn(simulation);
		}
		if(sourceActive2Cluster.isActive())
		{
			newCluster.oneIn(simulation);
		}
		
		return newCluster;
	}
	
	private static Cluster pwSourceBlotAndSourceActive(BoardUniverse board, SimulationManager simulation, Connector sourceBlot, Connector sourceActive)
	{
		SourceCluster sourceBlotCluster = (SourceCluster) sourceBlot.getCluster();
		SourceCluster sourceActiveCluster = (SourceCluster) sourceActive.getCluster();
		
		//Split B off its Blot, make it a new cluster C, let C have A and B as source
		InheritingCluster newCluster = new InheritingCluster(board.getNewClusterID());
		newCluster.addSource(sourceBlotCluster);
		newCluster.addSource(sourceActiveCluster);
		sourceBlotCluster.addDrain(newCluster);
		sourceActiveCluster.addDrain(newCluster);
		
		Prototype splitted = splitNonSourcePartFromCluster(sourceActive);
		splitted.mergeInto(newCluster);
		
		//Update C according to its sources
		if(sourceBlotCluster.isActive())
		{
			newCluster.oneIn(simulation);
		}
		if(sourceActiveCluster.isActive())
		{
			newCluster.oneIn(simulation);
		}
		
		return sourceBlotCluster;
	}
	
	private static Cluster pwSourceBlotAndDrainActive(BoardUniverse board, SimulationManager simulation, Connector sourceBlot, Connector drainActive)
	{
		SourceCluster sourceBlotCluster = (SourceCluster) sourceBlot.getCluster();
		InheritingCluster drainActiveCluster = (InheritingCluster) drainActive.getCluster();
		
		//Add A as source to B
		drainActiveCluster.addSource(sourceBlotCluster);
		sourceBlotCluster.addDrain(drainActiveCluster);
		
		//If A is ON, update B
		if(sourceBlotCluster.isActive())
		{
			drainActiveCluster.oneIn(simulation);
		}
		
		return sourceBlotCluster;
	}
	
	private static Cluster pwSourceAndDrainOFF(BoardUniverse board, SimulationManager simulation, Connector source, Connector drainOff)
	{
		SourceCluster sourceCluster = (SourceCluster) source.getCluster();
		InheritingCluster drainOffCluster = (InheritingCluster) drainOff.getCluster();
		
		//Append B to A (deletes B)
		board.deleteCluster(drainOffCluster.getId());
		Prototype movedClusterParts = new Prototype(drainOffCluster);
		movedClusterParts.mergeInto(sourceCluster);
		
		//If A is ON, update B
		if(sourceCluster.isActive())
		{
			movedClusterParts.scheduleUpdateable(simulation);
		}
		
		return sourceCluster;
	}
	
	//Preconditions:
	// - All parts have a cluster
	// - startpoint is not a Blot
	//Postcondition:
	// - cluster doesn't contain the splitted parts anymore
	// - splitted parts don't have a cluster anymore
	private static Prototype splitNonSourcePartFromCluster(Connector startPoint)
	{
		Prototype prototype = new Prototype();
		Cluster startCluster = startPoint.getCluster();
		
		LinkedList<Connector> connectors = new LinkedList<>();
		startPoint.setCluster(null);
		prototype.addConnector(startPoint);
		connectors.addLast(startPoint);
		
		while(!connectors.isEmpty())
		{
			Connector current = connectors.removeFirst();
			
			for(Wire wire : current.getWires())
			{
				if(wire.getCluster() == null)
				{
					continue;
				}
				
				Connector otherSide = wire.getOtherSide(current);
				//If the other side is not a Blot, it is still part of what has to be striped.
				//If it is a Blot, don't stripe it including wires.
				//It actually does not matter which cluster that Blot has.
				if(!(otherSide instanceof Blot))
				{
					wire.setCluster(null);
					prototype.addWire(wire);
					if(otherSide.getCluster() != null)
					{
						otherSide.setCluster(null);
						prototype.addConnector(otherSide);
						connectors.add(otherSide);
					}
				}
			}
		}
		
		for(Wire wire : prototype.getWires())
		{
			startCluster.remove(wire);
		}
		for(Connector connector : prototype.getConnectors())
		{
			startCluster.remove(connector);
		}
		
		return prototype;
	}
	
	private static class Prototype
	{
		private final List<Wire> wires = new ArrayList<>();
		private final List<Connector> connectors = new ArrayList<>();
		
		public Prototype()
		{
		}
		
		public Prototype(Cluster cluster)
		{
			for(Wire wire : cluster.getWires())
			{
				wire.setCluster(null);
				wires.add(wire);
			}
			for(Connector connector : cluster.getConnectors())
			{
				connector.setCluster(null);
				connectors.add(connector);
			}
		}
		
		public void addWire(Wire wire)
		{
			wires.add(wire);
		}
		
		public void addConnector(Connector connector)
		{
			connectors.add(connector);
		}
		
		public List<Connector> getConnectors()
		{
			return connectors;
		}
		
		public List<Wire> getWires()
		{
			return wires;
		}
		
		public void mergeInto(Cluster cluster)
		{
			for(Connector drainConnector : connectors)
			{
				drainConnector.setCluster(cluster);
				cluster.addConnector(drainConnector);
			}
			for(Wire wire : wires)
			{
				wire.setCluster(cluster);
				cluster.addWire(wire);
			}
		}
		
		public void scheduleUpdateable(SimulationManager simulation)
		{
			//Must be pegs!
			for(Connector connector : connectors)
			{
				if(connector.getParent() instanceof Updateable)
				{
					simulation.updateNextTick((Updateable) connector.getParent());
				}
			}
		}
	}
	
	private static int getClusterType(Connector connector, Cluster cluster)
	{
		if(connector instanceof Blot)
		{
			return SourceBlot;
		}
		else if(cluster instanceof SourceCluster)
		{
			return SourceActive;
		}
		else if(((InheritingCluster) cluster).getSources().size() == 0)
		{
			return DrainOFF;
		}
		else
		{
			return DrainActive;
		}
	}
}

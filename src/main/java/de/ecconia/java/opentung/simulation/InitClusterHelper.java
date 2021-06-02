package de.ecconia.java.opentung.simulation;

import de.ecconia.java.opentung.components.conductor.Blot;
import de.ecconia.java.opentung.components.conductor.Connector;
import de.ecconia.java.opentung.components.conductor.Peg;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InitClusterHelper
{
	public static void createBlottyCluster(Blot blot)
	{
		//Precondition: No blot can have a cluster at this point.
		Cluster cluster = new SourceCluster(blot);
		cluster.addConnector(blot);
		blot.setCluster(cluster);
		
		if(blot.getWires().isEmpty())
		{
			return;
		}
		
		for(Wire wire : blot.getWires())
		{
			Connector otherSide = wire.getOtherSide(blot);
			if(otherSide instanceof Blot)
			{
				System.out.println("WARNING: Circuit contains Blot-Blot connection which is not allowed.");
				wire.setCluster(new InheritingCluster()); //Required for maintenance mode. Every conductor needs a cluster.
				continue;
			}
			cluster.addWire(wire);
			wire.setCluster(cluster);
		}
		
		List<Wire> wires = new ArrayList<>(cluster.getWires());
		for(Wire wire : wires)
		{
			Connector otherSide = wire.getOtherSide(blot);
			expandBlottyCluster(cluster, otherSide);
		}
	}
	
	private static void expandBlottyCluster(Cluster cluster, Connector start)
	{
		Set<Wire> additionalWires = new HashSet<>();
		Set<Connector> additionalConnectors = new HashSet<>();
		boolean abort = false;
		
		List<Connector> connectorsToProbe = new ArrayList<>();
		connectorsToProbe.add(start);
		
		while(!connectorsToProbe.isEmpty())
		{
			Connector connector = connectorsToProbe.remove(0);
			//If we did check this wire already, may happen on loop.
			if(additionalConnectors.contains(connector))
			{
				continue;
			}
			if(connector instanceof Blot)
			{
				//Another source has been found, abort.
				abort = true;
				break;
			}
			//If the other component has a cluster already we need to know which
			if(connector.getCluster() != null)
			{
				if(connector.getCluster() != cluster)
				{
					//That cluster was not our cluster, we have a clash - abort.
					abort = true;
					break;
				}
				else
				{
					//It was our cluster - loop detected, ignore this component.
					//Dough should never happen, but rather check it than not check it.
					continue;
				}
			}
			
			//Connector was never handled at this point, cool add it to the set.
			additionalConnectors.add(connector);
			
			//Check each wire connected to that connector...
			for(Wire wireAtConnector : connector.getWires())
			{
				//If we did check this wire already, may happen on loop.
				if(additionalWires.contains(wireAtConnector))
				{
					continue;
				}
				//If the other wire has a cluster already we need to know which
				if(wireAtConnector.getCluster() != null)
				{
					if(wireAtConnector.getCluster() != cluster)
					{
						//That cluster was not our cluster, we have a clash - abort.
						abort = true;
						break;
					}
					else
					{
						//It was our cluster - loop detected, ignore this wire.
						//Only relevant for the wires directly connected to the blot, other wires are in the set.
						continue;
					}
				}
				
				//Wire was never handled at this point, cool add it to the set.
				additionalWires.add(wireAtConnector);
				//Get the other side's connector and enqueue it.
				Connector otherSide = wireAtConnector.getOtherSide(connector);
				connectorsToProbe.add(otherSide);
			}
		}
		
		//No other input source has been found, add all found components to this cluster.
		if(!abort)
		{
			for(Wire wire : additionalWires)
			{
				wire.setCluster(cluster);
				cluster.addWire(wire);
			}
			for(Connector connector : additionalConnectors)
			{
				connector.setCluster(cluster);
				cluster.addConnector(connector);
			}
		}
	}
	
	public static void createPeggyCluster(Peg peg)
	{
		InheritingCluster cluster = new InheritingCluster();
		
		List<Connector> connectorsToProbe = new ArrayList<>();
		connectorsToProbe.add(peg);
		
		while(!connectorsToProbe.isEmpty())
		{
			Connector connector = connectorsToProbe.remove(0);
			//The component made it into this queue, thus it must be unconnected yet.
			cluster.addConnector(connector);
			connector.setCluster(cluster);
			
			for(Wire wire : connector.getWires())
			{
				//Check if its a source-cluster, cause only these don't vore everything.
				if(wire.hasCluster())
				{
					if(wire.getCluster() != cluster)
					{
						//Assume this is a source-cluster, it has to be one.
						SourceCluster sourceCluster = (SourceCluster) wire.getCluster();
						cluster.addSource(sourceCluster);
						sourceCluster.addDrain(cluster);
					}
					continue;
				}
				
				wire.setCluster(cluster);
				cluster.addWire(wire);
				
				Connector otherSide = wire.getOtherSide(connector);
				if(otherSide.hasCluster())
				{
					if(otherSide.getCluster() == cluster)
					{
						continue;
					}
					else
					{
						throw new RuntimeException("Encountered other connector-cluster while expanding a peggy cluster.");
					}
				}
				//Assume that the otherSide has no cluster
				connectorsToProbe.add(otherSide);
			}
		}
	}
}

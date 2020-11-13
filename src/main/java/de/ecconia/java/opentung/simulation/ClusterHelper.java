package de.ecconia.java.opentung.simulation;

import de.ecconia.java.opentung.components.conductor.Blot;
import de.ecconia.java.opentung.components.conductor.Connector;
import de.ecconia.java.opentung.components.conductor.Peg;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.core.BoardUniverse;
import de.ecconia.java.opentung.meshing.ConductorMeshBag;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ClusterHelper
{
	private static final int SourceBlot = 0;
	private static final int SourceActive = 1;
	private static final int DrainOFF = 2;
	private static final int DrainActive = 3;
	
	//Wire Placement:
	
	public static void placeWire(SimulationManager simulation, BoardUniverse board, Connector connectorA, Connector connectorB, Wire newWire, Map<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> updates)
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
				wireCluster = pwSourceBlotAndSourceActive(simulation, connectorA, connectorB, updates);
				break;
			}
			case SourceBlot * 4 + DrainOFF:
			{
				wireCluster = pwSourceAndDrainOFF(simulation, connectorA, connectorB, updates);
				break;
			}
			case SourceBlot * 4 + DrainActive:
			{
				wireCluster = pwSourceBlotAndDrainActive(simulation, connectorA, connectorB, updates);
				break;
			}
			case SourceActive * 4 + SourceBlot:
			{
				wireCluster = pwSourceBlotAndSourceActive(simulation, connectorB, connectorA, updates);
				break;
			}
			case SourceActive * 4 + SourceActive:
			{
				wireCluster = pwSourceActiveAndSourceActive(simulation, connectorA, connectorB, updates);
				break;
			}
			case SourceActive * 4 + DrainOFF:
			{
				wireCluster = pwSourceAndDrainOFF(simulation, connectorA, connectorB, updates);
				break;
			}
			case SourceActive * 4 + DrainActive:
			{
				wireCluster = pwSourceActiveAndDrainActive(simulation, connectorA, connectorB, updates);
				break;
			}
			case DrainOFF * 4 + SourceBlot:
			{
				wireCluster = pwSourceAndDrainOFF(simulation, connectorB, connectorA, updates);
				break;
			}
			case DrainOFF * 4 + SourceActive:
			{
				wireCluster = pwSourceAndDrainOFF(simulation, connectorB, connectorA, updates);
				break;
			}
			case DrainOFF * 4 + DrainOFF:
			{
				//Append A to B (deletes A)
				//TBI: Should something be done when deleting?
//				board.deleteCluster(clusterB.getId());
				Prototype merge = new Prototype(clusterB);
				merge.mergeInto(clusterA, updates);
				wireCluster = clusterA;
				break;
			}
			case DrainOFF * 4 + DrainActive:
			{
				wireCluster = pwDrainOffAndDrainActive(simulation, connectorA, connectorB, updates);
				break;
			}
			case DrainActive * 4 + SourceBlot:
			{
				wireCluster = pwSourceBlotAndDrainActive(simulation, connectorB, connectorA, updates);
				break;
			}
			case DrainActive * 4 + SourceActive:
			{
				wireCluster = pwSourceActiveAndDrainActive(simulation, connectorB, connectorA, updates);
				break;
			}
			case DrainActive * 4 + DrainOFF:
			{
				wireCluster = pwDrainOffAndDrainActive(simulation, connectorB, connectorA, updates);
				break;
			}
			case DrainActive * 4 + DrainActive:
			{
				wireCluster = pwDrainActiveAndDrainActive(simulation, connectorA, connectorB, updates);
				break;
			}
		}
		
		newWire.setCluster(wireCluster);
		wireCluster.addWire(newWire);
	}
	
	private static Cluster pwDrainActiveAndDrainActive(SimulationManager simulation, Connector drainActive1, Connector drainActive2, Map<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> updates)
	{
		InheritingCluster drainActive1Cluster = (InheritingCluster) drainActive1.getCluster();
		InheritingCluster drainActive2Cluster = (InheritingCluster) drainActive2.getCluster();
		
		List<Connector> oldConnectors = new ArrayList<>(drainActive2Cluster.getConnectors());
		boolean on1 = drainActive1Cluster.isActive();
		boolean on2 = drainActive2Cluster.isActive();
		
		//Merge A and B
		for(SourceCluster source : drainActive1Cluster.getSources())
		{
			source.remove(drainActive1Cluster);
			source.addDrain(drainActive2Cluster);
			drainActive2Cluster.addSource(source);
			if(source.isActive())
			{
				drainActive2Cluster.oneIn(simulation);
			}
		}
		Prototype merge = splitNonSourcePartFromCluster(drainActive1);
		merge.mergeInto(drainActive2Cluster, updates);
		
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
	
	private static Cluster pwDrainOffAndDrainActive(SimulationManager simulation, Connector drainOff, Connector drainActive, Map<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> updates)
	{
		InheritingCluster drainActiveCluster = (InheritingCluster) drainActive.getCluster();
		InheritingCluster drainOffCluster = (InheritingCluster) drainOff.getCluster();
		
		//Append A to B (deletes A)
		Prototype merge = new Prototype(drainOffCluster);
		merge.mergeInto(drainActiveCluster, updates);
		
		//If B is ON, update A
		if(drainActiveCluster.isActive())
		{
			merge.scheduleUpdateable(simulation);
		}
		
		return drainActiveCluster;
	}
	
	private static Cluster pwSourceActiveAndDrainActive(SimulationManager simulation, Connector sourceActive, Connector drainActive, Map<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> updates)
	{
		SourceCluster sourceActiveCluster = (SourceCluster) sourceActive.getCluster();
		InheritingCluster drainActiveCluster = (InheritingCluster) drainActive.getCluster();
		
		//Split A off its Blot, append it to B
		Prototype splitA = splitNonSourcePartFromCluster(sourceActive);
		splitA.mergeInto(drainActiveCluster, updates);
		
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
	
	private static Cluster pwSourceActiveAndSourceActive(SimulationManager simulation, Connector sourceActive1, Connector sourceActive2, Map<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> updates)
	{
		SourceCluster sourceActive1Cluster = (SourceCluster) sourceActive1.getCluster();
		SourceCluster sourceActive2Cluster = (SourceCluster) sourceActive2.getCluster();
		
		//Split A and B off their Blots and make their merge a Cluster C with A and B as source
		InheritingCluster newCluster = new InheritingCluster();
		newCluster.addSource(sourceActive1Cluster);
		newCluster.addSource(sourceActive2Cluster);
		sourceActive1Cluster.addDrain(newCluster);
		sourceActive2Cluster.addDrain(newCluster);
		
		Prototype splitA = splitNonSourcePartFromCluster(sourceActive1);
		Prototype splitB = splitNonSourcePartFromCluster(sourceActive2);
		splitA.mergeInto(newCluster, updates);
		splitB.mergeInto(newCluster, updates);
		
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
	
	private static Cluster pwSourceBlotAndSourceActive(SimulationManager simulation, Connector sourceBlot, Connector sourceActive, Map<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> updates)
	{
		SourceCluster sourceBlotCluster = (SourceCluster) sourceBlot.getCluster();
		SourceCluster sourceActiveCluster = (SourceCluster) sourceActive.getCluster();
		
		//Split B off its Blot, make it a new cluster C, let C have A and B as source
		InheritingCluster newCluster = new InheritingCluster();
		newCluster.addSource(sourceBlotCluster);
		sourceBlotCluster.addDrain(newCluster);
		
		Prototype splitted = splitNonSourcePartFromCluster(sourceActive);
		splitted.mergeInto(newCluster, updates);
		
		//We want to store these guys multiple times, for each wire-connection.
		for(Wire sourceWire : splitted.getBlotWires())
		{
			newCluster.addSource(sourceActiveCluster);
			sourceActiveCluster.addDrain(newCluster);
		}
		
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
	
	private static Cluster pwSourceBlotAndDrainActive(SimulationManager simulation, Connector sourceBlot, Connector drainActive, Map<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> updates)
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
	
	private static Cluster pwSourceAndDrainOFF(SimulationManager simulation, Connector source, Connector drainOff, Map<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> updates)
	{
		SourceCluster sourceCluster = (SourceCluster) source.getCluster();
		InheritingCluster drainOffCluster = (InheritingCluster) drainOff.getCluster();
		
		//Append B to A (deletes B)
		//TBI: Should something be done when deleting?
//		board.deleteCluster(drainOffCluster.getId());
		Prototype movedClusterParts = new Prototype(drainOffCluster);
		movedClusterParts.mergeInto(sourceCluster, updates);
		
		//If A is ON, update B
		if(sourceCluster.isActive())
		{
			movedClusterParts.scheduleUpdateable(simulation);
		}
		
		return sourceCluster;
	}
	
	//Removal:
	
	public static void removeWire(SimulationManager simulation, Wire wireToDelete, Map<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> updates)
	{
		Connector a = wireToDelete.getConnectorA();
		Connector b = wireToDelete.getConnectorB();
		
		//If one side is a blot, the other side can be a Source/Drain cluster. Both cases are handled here:
		boolean aIsBlot = a instanceof Blot;
		if(aIsBlot || (b instanceof Blot))
		{
			Blot blot = (Blot) (aIsBlot ? a : b);
			SourceCluster blotCluster = (SourceCluster) blot.getCluster();
			Peg other = (Peg) (aIsBlot ? b : a);
			
			blotCluster.remove(wireToDelete);
			blot.remove(wireToDelete);
			other.remove(wireToDelete);
			
			if(other.getCluster() instanceof SourceCluster)
			{
				Prototype split = splitNonSourcePartFromCluster(other);
				
				if(split.getBlotWires().isEmpty())
				{
					InheritingCluster newCluster = new InheritingCluster();
					split.mergeInto(newCluster, updates);
					//Can't have any sources now, cause we are about to delete the only source.
					if(blotCluster.isActive())
					{
						split.scheduleUpdateable(simulation);
					}
				}
				else
				{
					//There are more connections to the blot, thus just restore the split.
					split.mergeInto(blotCluster, updates);
				}
			}
			else
			{
				InheritingCluster otherCluster = (InheritingCluster) other.getCluster();
				otherCluster.remove(blotCluster);
				blotCluster.remove(otherCluster);
				
				if(otherCluster.getSources().size() == 1)
				{
					Prototype split = splitNonSourcePartFromCluster(other);
					Wire sourceWire = split.getBlotWires().get(0);
					Connector sourceConnector = sourceWire.getConnectorA().getCluster() instanceof SourceCluster ? sourceWire.getConnectorA() : sourceWire.getConnectorB();
					SourceCluster source = (SourceCluster) sourceConnector.getCluster();
					
					split.mergeInto(source, updates);
					source.remove(otherCluster);
					
					if(source.isActive() != otherCluster.isActive())
					{
						split.scheduleUpdateable(simulation);
					}
					
					//TBI: Should something be done when deleting?
//				    board.deleteCluster(otherCluster.getId());
				}
				else
				{
					//No need to remove from source cluster.
					if(blotCluster.isActive())
					{
						otherCluster.oneOut(simulation);
					}
				}
			}
			
			return;
		}
		
		//If one side is a Source cluster, the other side must be the same cluster! That case is handled here:
		if(a.getCluster() instanceof SourceCluster)
		{
			SourceCluster cluster = (SourceCluster) a.getCluster();
			//Remove wire before tracing.
			cluster.remove(wireToDelete);
			a.remove(wireToDelete);
			b.remove(wireToDelete);
			//Split both and handle both independently:
			Prototype splitA = splitNonSourcePartFromCluster(a);
			boolean bSideHasNoCluster = b.getCluster() == null;
			if(splitA.getBlotWires().isEmpty())
			{
				InheritingCluster newCluster = new InheritingCluster();
				splitA.mergeInto(newCluster, updates);
				//Can't have any sources now, cause we are about to delete the only source.
				if(cluster.isActive())
				{
					splitA.scheduleUpdateable(simulation);
				}
			}
			else
			{
				//There are more connections to the origin, thus just restore the split.
				splitA.mergeInto(cluster, updates);
			}
			//If the B side has no cluster anymore, it must have been splitted off while splitting off the A side. Both are still the same cluster and still connected, no further action needed.
			if(!bSideHasNoCluster)
			{
				Prototype splitB = splitNonSourcePartFromCluster(b);
				if(splitB.getBlotWires().isEmpty())
				{
					InheritingCluster newCluster = new InheritingCluster();
					splitB.mergeInto(newCluster, updates);
					//Can't have any sources now, cause we are about to delete the only source.
					if(cluster.isActive())
					{
						splitB.scheduleUpdateable(simulation);
					}
				}
				else
				{
					//There are more connections to the origin, thus just restore the split.
					splitB.mergeInto(cluster, updates);
				}
			}
			
			return;
		}
		
		//Both remaining wires can only belong to the same Drain Cluster.
		{
			InheritingCluster cluster = (InheritingCluster) a.getCluster();
			//Remove wire before tracing.
			cluster.remove(wireToDelete);
			a.remove(wireToDelete);
			b.remove(wireToDelete);
			//Split both and handle both independently:
			Prototype splitA = splitNonSourcePartFromCluster(a);
			if(b.getCluster() == null)
			{
				//Both are in the same split, thus no changes have to be made.
				splitA.mergeInto(cluster, updates); //Restore/Undo
				return; //We are done here.
			}
			//TBI: Should something be done when deleting?
//			board.deleteCluster(cluster.getId());
			
			if(splitA.getBlotWires().size() == 1)
			{
				Wire sourceWire = splitA.getBlotWires().get(0);
				Connector sourceConnector = sourceWire.getConnectorA().getCluster() instanceof SourceCluster ? sourceWire.getConnectorA() : sourceWire.getConnectorB();
				SourceCluster source = (SourceCluster) sourceConnector.getCluster();
				
				source.remove(cluster);
				splitA.mergeInto(source, updates);
				
				if(source.isActive() != cluster.isActive())
				{
					splitA.scheduleUpdateable(simulation);
				}
			}
			else
			{
				//Creating new cluster for A side:
				InheritingCluster aCluster = new InheritingCluster();
				splitA.mergeInto(aCluster, updates);
				if(splitA.getBlotWires().isEmpty())
				{
					//Can't have any sources now, must be off.
					if(cluster.isActive())
					{
						splitA.scheduleUpdateable(simulation);
					}
				}
				else
				{
					for(Wire sourceWire : splitA.getBlotWires())
					{
						Connector sourceConnector = sourceWire.getConnectorA().getCluster() instanceof SourceCluster ? sourceWire.getConnectorA() : sourceWire.getConnectorB();
						SourceCluster source = (SourceCluster) sourceConnector.getCluster();
						
						source.remove(cluster);
						source.addDrain(aCluster);
						aCluster.addSource(source);
						if(source.isActive())
						{
							aCluster.oneIn(simulation);
						}
					}
					
					if(aCluster.isActive() != cluster.isActive())
					{
						//The state of the cluster is different now, update.
						splitA.scheduleUpdateable(simulation);
					}
				}
			}
			
			Prototype splitB = splitNonSourcePartFromCluster(b);
			if(splitB.getBlotWires().size() == 1)
			{
				Wire sourceWire = splitB.getBlotWires().get(0);
				Connector sourceConnector = sourceWire.getConnectorA().getCluster() instanceof SourceCluster ? sourceWire.getConnectorA() : sourceWire.getConnectorB();
				SourceCluster source = (SourceCluster) sourceConnector.getCluster();
				
				source.remove(cluster);
				splitB.mergeInto(source, updates);
				
				if(source.isActive() != cluster.isActive())
				{
					splitB.scheduleUpdateable(simulation);
				}
			}
			else
			{
				//Creating new cluster for B side:
				InheritingCluster bCluster = new InheritingCluster();
				splitB.mergeInto(bCluster, updates);
				if(splitB.getBlotWires().isEmpty())
				{
					//Can't have any sources now, must be off.
					if(cluster.isActive())
					{
						splitB.scheduleUpdateable(simulation);
					}
				}
				else
				{
					for(Wire sourceWire : splitB.getBlotWires())
					{
						Connector sourceConnector = sourceWire.getConnectorA().getCluster() instanceof SourceCluster ? sourceWire.getConnectorA() : sourceWire.getConnectorB();
						SourceCluster source = (SourceCluster) sourceConnector.getCluster();
						
						source.remove(cluster);
						source.addDrain(bCluster);
						bCluster.addSource(source);
						if(source.isActive())
						{
							bCluster.oneIn(simulation);
						}
					}
					
					if(bCluster.isActive() != cluster.isActive())
					{
						//The state of the cluster is different now, update.
						splitB.scheduleUpdateable(simulation);
					}
				}
			}
		}
	}
	
	public static void removePeg(SimulationManager simulation, Peg peg, Map<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> updates)
	{
		Cluster cluster = peg.getCluster();
		cluster.remove(peg);
		if(cluster instanceof SourceCluster)
		{
			//Note: Only one single Blot in this cluster. Find and handle appropriately.
			SourceCluster source = (SourceCluster) cluster;
			
			//First remove all the wires which could lead to this peg, so that later traces will never pass it.
			boolean directSourceFound = false;
			for(Wire wire : peg.getWires())
			{
				Connector otherSide = wire.getOtherSide(peg);
				otherSide.remove(wire);
				source.remove(wire);
				
				if(otherSide instanceof Blot)
				{
					directSourceFound = true;
				}
			}
			
			if(directSourceFound)
			{
				for(Wire wire : peg.getWires())
				{
					//Must be Peg and same cluster.
					Connector otherSide = wire.getOtherSide(peg);
					if(otherSide instanceof Blot)
					{
						continue; //Nothing to do here, skip.
					}
					if(otherSide.getCluster() != source)
					{
						//Already handled.
						continue;
					}
					
					Prototype split = splitNonSourcePartFromCluster(otherSide); //There can't be any sources anymore.
					InheritingCluster newCluster = new InheritingCluster();
					split.mergeInto(newCluster, updates);
				}
			}
			else
			{
				List<Prototype> toReset = new ArrayList<>();
				for(Wire wire : peg.getWires())
				{
					Connector otherSide = wire.getOtherSide(peg);
					if(otherSide.getCluster() != source)
					{
						//Already handled and not other SourceCluster.
						continue;
					}
					
					Prototype split = splitNonSourcePartFromCluster(otherSide);
					
					if(split.getBlotWires().isEmpty())
					{
						//New and off section:
						InheritingCluster newCluster = new InheritingCluster();
						split.mergeInto(newCluster, updates);
						
						if(source.isActive())
						{
							split.scheduleUpdateable(simulation);
						}
					}
					else
					{
						//Still part of the original source-cluster
						toReset.add(split); //Add, so that it won't be scanned another time.
					}
				}
				
				for(Prototype split : toReset)
				{
					split.mergeInto(source, updates);
				}
			}
		}
		else
		{
			InheritingCluster drain = (InheritingCluster) cluster;
			//TBI: Should something be done when deleting?
//			board.deleteCluster(drain.getId());
			
			if(drain.getSources().isEmpty())
			{
				//First remove all the wires which could lead to this peg, so that later traces will never pass it.
				for(Wire wire : peg.getWires()) //TODO: Concurrent here!!!
				{
					Connector otherSide = wire.getOtherSide(peg);
					otherSide.remove(wire);
					drain.remove(wire);
				}
				
				for(Wire wire : peg.getWires())
				{
					//Must be Peg and same cluster.
					Connector otherSide = wire.getOtherSide(peg);
					if(otherSide.getCluster() != drain)
					{
						//Already handled.
						continue;
					}
					
					Prototype split = splitNonSourcePartFromCluster(otherSide);
					InheritingCluster newCluster = new InheritingCluster();
					split.mergeInto(newCluster, updates);
				}
			}
			else
			{
				//First remove all the wires which could lead to this peg, so that later traces will never pass it.
				for(Wire wire : peg.getWires())
				{
					Connector otherSide = wire.getOtherSide(peg);
					otherSide.remove(wire);
					wire.getCluster().remove(wire); //Might be a different cluster.
				}
				
				for(Wire wire : peg.getWires())
				{
					Connector otherSide = wire.getOtherSide(peg);
					if(otherSide instanceof Blot)
					{
						//Detected a direct source, remove:
						((SourceCluster) otherSide.getCluster()).remove(drain);
						continue;
					}
					if(otherSide.getCluster() != drain)
					{
						//Already handled and not other SourceCluster.
						continue;
					}
					
					Prototype split = splitNonSourcePartFromCluster(otherSide);
					
					if(split.getBlotWires().size() == 1)
					{
						Wire sourceWire = split.getBlotWires().get(0);
						Connector sourceConnector = sourceWire.getConnectorA().getCluster() instanceof SourceCluster ? sourceWire.getConnectorA() : sourceWire.getConnectorB();
						SourceCluster source = (SourceCluster) sourceConnector.getCluster();
						
						split.mergeInto(source, updates);
						source.remove(drain);
						
						if(source.isActive() != drain.isActive())
						{
							split.scheduleUpdateable(simulation);
						}
					}
					else
					{
						InheritingCluster newCluster = new InheritingCluster();
						split.mergeInto(newCluster, updates);
						
						for(Wire sourceWire : split.getBlotWires())
						{
							Connector sourceConnector = sourceWire.getConnectorA().getCluster() instanceof SourceCluster ? sourceWire.getConnectorA() : sourceWire.getConnectorB();
							SourceCluster source = (SourceCluster) sourceConnector.getCluster();
							
							source.remove(drain);
							source.addDrain(newCluster);
							newCluster.addSource(source);
							if(source.isActive())
							{
								newCluster.oneIn(simulation);
							}
						}
						
						if(newCluster.isActive() != cluster.isActive())
						{
							//The state of the cluster is different now, update.
							split.scheduleUpdateable(simulation);
						}
					}
				}
			}
		}
	}
	
	public static void removeBlot(SimulationManager simulation, Blot blot, Map<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> updates)
	{
		SourceCluster sourceCluster = (SourceCluster) blot.getCluster();
		//TBI: Should something be done when deleting?
//		board.deleteCluster(sourceCluster.getId());
		
		//Linked list stores all source-clusters which had been processed. In case that they had wires leading to the blot again - these should be skipped.
		LinkedList<Cluster> newClusters = new LinkedList<>();
		for(Wire blotWire : blot.getWires())
		{
			sourceCluster.remove(blotWire);
			
			Connector otherSide = blotWire.getOtherSide(blot);
			otherSide.remove(blotWire); //Remove the wire-connection from the other side.
			if(newClusters.contains(otherSide.getCluster()))
			{
				//Has been splitted off before, just remove it.
				continue;
			}
			
			if(otherSide.getCluster() instanceof SourceCluster) //otherSide.getCluster() == sourceCluster
			{
				//Same cluster, split off.
				Prototype split = splitNonSourcePartFromCluster(otherSide);
				
				for(Wire otherBlotWire : split.getBlotWires())
				{
					Connector a = otherBlotWire.getConnectorA();
					Connector b = otherBlotWire.getConnectorB();
					Connector otherSideConnector = null;
					if(a == blot)
					{
						otherSideConnector = b;
					}
					else if(b == blot)
					{
						otherSideConnector = a;
					}
					else
					{
						System.out.println("WARNING: Corrupted cluster network. SourceCluster with more than one Source.");
						continue;
					}
					if(otherSideConnector == blot)
					{
						System.out.println("WARNING: Corrupted cluster network. Blot connects to itself.");
					}
					
					otherSideConnector.remove(otherBlotWire);
				}
				
				InheritingCluster newCluster = new InheritingCluster();
				newClusters.add(newCluster);
				split.mergeInto(newCluster, updates);
				//Can't have any sources now, cause we are about to delete the only source.
				if(sourceCluster.isActive())
				{
					split.scheduleUpdateable(simulation);
				}
			}
			else
			{
				InheritingCluster otherSideCluster = (InheritingCluster) otherSide.getCluster();
				otherSideCluster.remove(sourceCluster);
				
				if(otherSideCluster.getSources().size() == 1)
				{
					Prototype split = splitNonSourcePartFromCluster(otherSide);
					Wire sourceWire = split.getBlotWires().get(0);
					Connector sourceConnector = sourceWire.getConnectorA().getCluster() instanceof SourceCluster ? sourceWire.getConnectorA() : sourceWire.getConnectorB();
					SourceCluster source = (SourceCluster) sourceConnector.getCluster();
					
					split.mergeInto(source, updates);
					source.remove(otherSideCluster);
					
					if(source.isActive() != otherSideCluster.isActive())
					{
						split.scheduleUpdateable(simulation);
					}
				}
				else
				{
					//No need to remove from source cluster.
					if(sourceCluster.isActive())
					{
						otherSideCluster.oneOut(simulation);
					}
				}
			}
		}
	}
	
	//Other:
	
	//Preconditions:
	// - All parts have a cluster
	// - startpoint is not a Blot
	//Postcondition:
	// - cluster doesn't contain the splitted parts anymore
	// - splitted parts don't have a cluster anymore
	private static Prototype splitNonSourcePartFromCluster(Connector startPoint)
	{
		Prototype prototype = new Prototype(startPoint);
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
				else
				{
					prototype.addBlotWire(wire);
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
		private final Cluster shrunkCluster;
		private final List<Wire> wires = new ArrayList<>();
		private final List<Connector> connectors = new ArrayList<>();
		private final List<Wire> blotWires = new ArrayList<>();
		
		public Prototype(Connector connector)
		{
			this.shrunkCluster = connector.getCluster();
		}
		
		public Prototype(Cluster cluster)
		{
			this.shrunkCluster = cluster;
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
		
		public void mergeInto(Cluster cluster, Map<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> updates)
		{
			for(Connector drainConnector : connectors)
			{
				drainConnector.setCluster(cluster);
				doMeshUpdate(drainConnector, cluster, updates);
				cluster.addConnector(drainConnector);
			}
			for(Wire wire : wires)
			{
				wire.setCluster(cluster);
				doMeshUpdate(wire, cluster, updates);
				cluster.addWire(wire);
			}
		}
		
		private void doMeshUpdate(Clusterable clusterable, Cluster cluster, Map<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> updates)
		{
			Component component;
			if(clusterable instanceof Component)
			{
				component = (Component) clusterable;
			}
			else if(clusterable instanceof Connector)
			{
				component = ((Connector) clusterable).getParent();
			}
			else
			{
				return; //Not visible, ditch.
			}
			ConductorMeshBag meshBag = component.getConductorMeshBag();
			if(meshBag == null)
			{
				return; //Currently invisible, ditch.
			}
			
			if(shrunkCluster != cluster)
			{
				//Dump all cases which don't change things.
				List<ConductorMeshBag.ConductorMBUpdate> list = updates.get(meshBag);
				if(list == null)
				{
					list = new ArrayList<>();
					updates.put(meshBag, list);
				}
				list.add(new ConductorMeshBag.ConductorMBUpdate(shrunkCluster, cluster));
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
		
		public void addBlotWire(Wire wire)
		{
			blotWires.add(wire);
		}
		
		public List<Wire> getBlotWires()
		{
			return blotWires;
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

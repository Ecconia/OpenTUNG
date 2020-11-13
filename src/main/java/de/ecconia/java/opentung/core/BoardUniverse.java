package de.ecconia.java.opentung.core;

import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.CompLabel;
import de.ecconia.java.opentung.components.CompSnappingPeg;
import de.ecconia.java.opentung.components.CompSnappingWire;
import de.ecconia.java.opentung.components.conductor.Blot;
import de.ecconia.java.opentung.components.conductor.CompWireRaw;
import de.ecconia.java.opentung.components.conductor.Connector;
import de.ecconia.java.opentung.components.conductor.Peg;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.raycast.WireRayCaster;
import de.ecconia.java.opentung.savefile.BoardAndWires;
import de.ecconia.java.opentung.settings.Settings;
import de.ecconia.java.opentung.simulation.Cluster;
import de.ecconia.java.opentung.simulation.InheritingCluster;
import de.ecconia.java.opentung.simulation.Powerable;
import de.ecconia.java.opentung.simulation.SimulationManager;
import de.ecconia.java.opentung.simulation.SourceCluster;
import de.ecconia.java.opentung.simulation.Updateable;
import de.ecconia.java.opentung.simulation.Wire;
import de.ecconia.java.opentung.util.Ansi;
import de.ecconia.java.opentung.util.math.Quaternion;
import de.ecconia.java.opentung.util.math.Vector3;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

public class BoardUniverse
{
	private final CompBoard rootBoard;
	
	private final List<CompWireRaw> wiresToRender = new ArrayList<>();
	private List<Component> componentsToRender = new ArrayList<>(); //TODO: Remove
	private final List<CompLabel> labelsToRender = new ArrayList<>();
	//TODO: remove.
	public final List<CompWireRaw> brokenWires = new ArrayList<>();
	
	//### OTHER ###
	
	private final SimulationManager simulation = new SimulationManager();
	
	public BoardUniverse(BoardAndWires bnw)
	{
		this(bnw.getBoard());
		this.wiresToRender.addAll(Arrays.asList(bnw.getWires()));
	}
	
	public BoardUniverse(CompBoard board)
	{
		this.rootBoard = board;
		
		System.out.println("[BoardImport] Sorting components.");
		//Sort Elements into Lists, for the 3D section to use:
		importComponent(board);
		
		System.out.println("[BoardImport] Creating SnappingPeg bounds.");
		board.createSnappingPegBounds();
		
		System.out.println("[BoardImport] Linking SnappingPegs.");
		//Connect snapping pegs:
		linkSnappingPegs(board);
	}
	
	public void startFinalizeImport(BlockingQueue<GPUTask> gpuTasks, WireRayCaster wireRayCaster)
	{
		Thread finalizeThread = new Thread(() -> {
			System.out.println("[BoardImport] Creating connector bounds.");
			rootBoard.createConnectorBounds();
			System.out.println("[BoardImport] Linking wires.");
			try
			{
				//TODO: Don't scan through components, if its not a tungboard file.
				linkWires(rootBoard, rootBoard);
			}
			catch(Exception e)
			{
				e.printStackTrace();
				System.out.println(Ansi.red + "Couldn't find wire ports... " + Ansi.r);
			}
			
			System.out.println("[BoardImport] Creating connection-clusters.");
			//Create clusters:
			for(Wire wire : wiresToRender)
			{
				wire.getConnectorA().addWire(wire);
				wire.getConnectorB().addWire(wire);
			}
			
			//Create blot clusters:
			for(Component comp : componentsToRender)
			{
				for(Blot blot : comp.getBlots())
				{
					createBlottyCluster(blot);
				}
			}
			for(Component comp : componentsToRender)
			{
				for(Peg peg : comp.getPegs())
				{
					if(!peg.hasCluster())
					{
						createPeggyCluster(peg);
					}
				}
			}
			
			System.out.println("[BoardImport] Initializing simulation.");
			//Update clusters:
			for(Component component : componentsToRender)
			{
				if(component instanceof Powerable)
				{
					((Powerable) component).forceUpdateOutput();
				}
			}
			
			//Send every updateable into the simulation, to let each component have the chance to resume a clock.
			for(Component comp : componentsToRender)
			{
				if(comp instanceof Updateable)
				{
					simulation.updateNextTick((Updateable) comp);
				}
			}
			componentsToRender.clear();
			componentsToRender = null; //End of usage. TODO transfer reference instead of storing it.
			
			long startWireProcessing = System.currentTimeMillis();
			for(CompWireRaw wire : wiresToRender)
			{
				wireRayCaster.addWire(wire);
			}
			System.out.println("[BoardImport] Sorting " + wiresToRender.size() + " wires took " + (System.currentTimeMillis() - startWireProcessing) + "ms.");
			
			try
			{
				gpuTasks.put((RenderPlane3D world3D) -> {
					world3D.refreshPostWorldLoad();
				});
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
		}, "BoardProcessingThread");
		finalizeThread.setDaemon(true);
		finalizeThread.start();
	}
	
	private void createPeggyCluster(Peg peg)
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
	
	private void createBlottyCluster(Blot blot)
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
	
	private void linkWires(CompContainer container, CompContainer scannable)
	{
		boolean maintenance = Settings.importMaintenanceMode;
		Connector placebo = null;
		if(maintenance)
		{
			placebo = new Connector(null, null)
			{
			};
		}
		
		for(Component component : container.getChildren())
		{
			if(component instanceof CompWireRaw)
			{
				CompWireRaw wire = (CompWireRaw) component;
				
				Connector connectorA = scannable.getConnectorAt("", wire.getEnd1());
				Connector connectorB = scannable.getConnectorAt("", wire.getEnd2());
				if(!maintenance)
				{
					if(connectorA == null || connectorB == null)
					{
						brokenWires.add(wire);
						throw new RuntimeException("Could not import TungBoard, cause some wires seem to end up outside of connectors.");
					}
					wire.setConnectorA(connectorA);
					wire.setConnectorB(connectorB);
				}
				else
				{
					if(connectorA == null && connectorB == null)
					{
						//Ignore this wire, it will never be accessed.
						wire.setConnectorA(placebo);
						wire.setConnectorB(placebo);
						wire.setCluster(new InheritingCluster()); //Assign empty cluster, just for the ID.
					}
					else if(connectorA == null)
					{
						wire.setConnectorA(connectorB);
						wire.setConnectorB(connectorB);
					}
					else if(connectorB == null)
					{
						wire.setConnectorA(connectorA);
						wire.setConnectorB(connectorA);
					}
					else
					{
						wire.setConnectorA(connectorA);
						wire.setConnectorB(connectorB);
					}
				}
			}
			else if(component instanceof CompContainer)
			{
				linkWires((CompContainer) component, scannable);
			}
		}
	}
	
	private void expandBlottyCluster(Cluster cluster, Connector start)
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
	
	public SimulationManager getSimulation()
	{
		return simulation;
	}
	
	private void linkSnappingPegs(CompBoard board)
	{
		List<CompSnappingPeg> snappingPegs = new ArrayList<>();
		for(Component comp : componentsToRender)
		{
			if(comp instanceof CompSnappingPeg)
			{
				snappingPegs.add((CompSnappingPeg) comp);
			}
		}
		List<CompSnappingPeg> collector = new ArrayList<>();
		for(CompSnappingPeg peg : snappingPegs)
		{
			if(peg.hasPartner())
			{
				continue;
			}
			
			Vector3 connectionPoint = peg.getConnectionPoint();
			board.getSnappingPegsAt(connectionPoint, collector);
			collector.remove(peg); //A peg should always find itself.
			if(!collector.isEmpty())
			{
				double maxDist = 10;
				CompSnappingPeg other = null;
				for(CompSnappingPeg otherPeg : collector)
				{
					Vector3 otherConnectionPoint = otherPeg.getConnectionPoint();
					Vector3 diff = otherConnectionPoint.subtract(connectionPoint);
					double distance = Math.sqrt(diff.dot(diff));
					
					if(distance < maxDist)
					{
						other = otherPeg;
						maxDist = distance;
					}
				}
				
				if(other != null && maxDist < 0.21) //Leave some room for the 0.205 case for now...
				{
					if(other.hasPartner())
					{
						System.out.println("!!!! Some snapping peg already has a partner!");
					}
					else
					{
						other.setPartner(peg);
						peg.setPartner(other);
						CompSnappingWire wire = new CompSnappingWire(peg.getParent());
						wire.setConnectorA(peg.getPegs().get(0));
						wire.setConnectorB(other.getPegs().get(0));
						wire.setLength((float) maxDist);
						Vector3 direction = other.getConnectionPoint().subtract(connectionPoint).divide(2); //Get half of it.
						wire.setPosition(connectionPoint.add(direction));
						wire.setRotation(Quaternion.angleAxis(Math.toDegrees(Math.asin(direction.getX() / direction.length())), Vector3.yp));
						componentsToRender.add(wire);
						//Do here. Since no reference afterwards.
						wire.getConnectorA().addWire(wire);
						wire.getConnectorB().addWire(wire);
					}
				}
				
				collector.clear();
			}
		}
	}
	
	private void importComponent(Component component)
	{
		if(component instanceof CompWireRaw)
		{
			wiresToRender.add((CompWireRaw) component);
		}
		else if(!(component instanceof CompBoard))
		{
			componentsToRender.add(component);
		}
		
		if(component instanceof CompLabel)
		{
			labelsToRender.add((CompLabel) component);
			return;
		}
		
		if(component instanceof CompContainer)
		{
			for(Component child : ((CompContainer) component).getChildren())
			{
				importComponent(child);
			}
		}
	}
	
	public CompBoard getRootBoard()
	{
		return rootBoard;
	}
	
	public List<CompWireRaw> getWiresToRender()
	{
		return wiresToRender;
	}
	
	public List<CompLabel> getLabelsToRender()
	{
		return labelsToRender;
	}
	
	public List<CompWireRaw> getBrokenWires()
	{
		return brokenWires;
	}
}

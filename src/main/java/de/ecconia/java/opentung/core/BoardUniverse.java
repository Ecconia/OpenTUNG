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
import de.ecconia.java.opentung.components.meta.ConnectedComponent;
import de.ecconia.java.opentung.components.meta.PlaceboParent;
import de.ecconia.java.opentung.core.structs.GPUTask;
import de.ecconia.java.opentung.core.systems.CPURaycast;
import de.ecconia.java.opentung.raycast.RayCastResult;
import de.ecconia.java.opentung.raycast.WireRayCaster;
import de.ecconia.java.opentung.savefile.BoardAndWires;
import de.ecconia.java.opentung.settings.Settings;
import de.ecconia.java.opentung.simulation.InheritingCluster;
import de.ecconia.java.opentung.simulation.InitClusterHelper;
import de.ecconia.java.opentung.simulation.Powerable;
import de.ecconia.java.opentung.simulation.SimulationManager;
import de.ecconia.java.opentung.simulation.Updateable;
import de.ecconia.java.opentung.simulation.Wire;
import de.ecconia.java.opentung.util.Ansi;
import de.ecconia.java.opentung.util.math.MathHelper;
import de.ecconia.java.opentung.util.math.Quaternion;
import de.ecconia.java.opentung.util.math.Vector3;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class BoardUniverse
{
	private final CompBoard rootBoard;
	private final Component placeboWireParent = new PlaceboParent(); //Will be set as parent for wires, but has no other purpose other than removing the null reference.
	
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
		for(CompWireRaw wire : bnw.getWires())
		{
			wire.setParent(placeboWireParent);
			wiresToRender.add(wire);
		}
	}
	
	public BoardUniverse(CompBoard board)
	{
		this.rootBoard = board;
		
		System.out.println("[BoardImport] Sorting components.");
		//Sort Elements into Lists, for the 3D section to use:
		importComponent(board);
		
		for(CompWireRaw wire : wiresToRender)
		{
			//Fix Parent of wires.
			((CompContainer) wire.getParent()).remove(wire);
			wire.setParent(placeboWireParent);
		}
		
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
				e.printStackTrace(System.out);
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
				if(comp instanceof ConnectedComponent)
				{
					for(Blot blot : ((ConnectedComponent) comp).getBlots())
					{
						InitClusterHelper.createBlottyCluster(blot);
					}
				}
			}
			for(Component comp : componentsToRender)
			{
				if(comp instanceof ConnectedComponent)
				{
					for(Peg peg : ((ConnectedComponent) comp).getPegs())
					{
						if(!peg.hasCluster())
						{
							InitClusterHelper.createPeggyCluster(peg);
						}
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
				e.printStackTrace(System.out);
			}
		}, "BoardProcessingThread");
		finalizeThread.setDaemon(true);
		finalizeThread.start();
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
		
		for(CompWireRaw wire : wiresToRender)
		{
			if(wire.getConnectorA() != null)
			{
				continue; //Already processed -> Skip.
			}
			Connector connectorA = scannable.getConnectorAt(wire.getEnd1());
			Connector connectorB = scannable.getConnectorAt(wire.getEnd2());
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
		
		if(snappingPegs.isEmpty())
		{
			return;
		}
		
		CPURaycast raycaster = new CPURaycast();
		for(CompSnappingPeg snappingPegA : snappingPegs)
		{
			if(snappingPegA.hasPartner())
			{
				continue;
			}
			
			Vector3 snappingPegAConnectionPoint = snappingPegA.getConnectionPoint();
			Vector3 rayA = snappingPegA.getAlignmentGlobal().inverse().multiply(Vector3.zn);
			RayCastResult result = raycaster.cpuRaycast(snappingPegAConnectionPoint, rayA, rootBoard);
			
			//Check if the result is not null, and a SnappingPeg within 0.2 distance.
			if(result.getMatch() != null && result.getMatch() instanceof Connector && result.getMatch().getParent() instanceof CompSnappingPeg && result.getDistance() <= 0.2)
			{
				CompSnappingPeg snappingPegB = (CompSnappingPeg) result.getMatch().getParent();
				if(!snappingPegB.hasPartner()) //Do not process it further, if it is already connected to somewhere.
				{
					//Calculate their angles to each other:
					Vector3 rayB = snappingPegB.getAlignmentGlobal().inverse().multiply(Vector3.zn);
					double angle = MathHelper.angleFromVectors(rayA, rayB);
					if(angle > 178 && angle < 182)
					{
						//Angles and ray-cast match, now perform the actual linking:
						Vector3 snappingPegBConnectionPoint = snappingPegB.getConnectionPoint();
						Vector3 direction = snappingPegBConnectionPoint.subtract(snappingPegAConnectionPoint);
						double distance = direction.length();
						Quaternion alignment = MathHelper.rotationFromVectors(Vector3.zp, direction.normalize());
						if(Double.isNaN(alignment.getA()))
						{
							System.out.println("[ERROR] Cannot place snapping peg wire, cause start- and end-point are probably the same... Please try to not abuse OpenTUNG. If you did not intentionally cause this, send your save to a developer.");
							continue; //Do not connect these, there is something horribly wrong here.
						}
						
						snappingPegB.setPartner(snappingPegA);
						snappingPegA.setPartner(snappingPegB);
						CompSnappingWire wire = new CompSnappingWire(snappingPegA.getParent());
						wire.setLength((float) distance);
						wire.setPositionGlobal(snappingPegAConnectionPoint.add(direction.divide(2)));
						wire.setAlignmentGlobal(alignment);
						
						componentsToRender.add(wire);
						
						//Currently no cluster has been created, thus link the wires manually, for the cluster creation to use it.
						wire.setConnectorA(snappingPegA.getPegs().get(0));
						wire.setConnectorB(snappingPegB.getPegs().get(0));
						wire.getConnectorA().addWire(wire);
						wire.getConnectorB().addWire(wire);
					}
				}
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
	
	public Component getPlaceboWireParent()
	{
		return placeboWireParent;
	}
}

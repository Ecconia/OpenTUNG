package de.ecconia.java.opentung.core.tools;

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
import de.ecconia.java.opentung.components.meta.Part;
import de.ecconia.java.opentung.core.BoardUniverse;
import de.ecconia.java.opentung.core.data.Hitpoint;
import de.ecconia.java.opentung.core.data.SharedData;
import de.ecconia.java.opentung.core.structs.GPUTask;
import de.ecconia.java.opentung.meshing.ConductorMeshBag;
import de.ecconia.java.opentung.meshing.MeshBagContainer;
import de.ecconia.java.opentung.raycast.WireRayCaster;
import de.ecconia.java.opentung.settings.keybinds.Keybindings;
import de.ecconia.java.opentung.simulation.Cluster;
import de.ecconia.java.opentung.simulation.ClusterHelper;
import de.ecconia.java.opentung.simulation.HiddenWire;
import de.ecconia.java.opentung.simulation.SimulationManager;
import de.ecconia.java.opentung.simulation.Wire;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

public class Delete implements Tool
{
	private final SharedData sharedData;
	private final SimulationManager simulation;
	private final MeshBagContainer worldMesh;
	private final WireRayCaster wireRayCaster;
	private final BlockingQueue<GPUTask> gpuTasks;
	private final BoardUniverse board;
	
	public Delete(SharedData sharedData)
	{
		this.sharedData = sharedData;
		
		simulation = sharedData.getBoardUniverse().getSimulation();
		worldMesh = sharedData.getRenderPlane3D().getWorldMesh();
		wireRayCaster = sharedData.getRenderPlane3D().getWireRayCaster();
		gpuTasks = sharedData.getGpuTasks();
		board = sharedData.getBoardUniverse();
	}
	
	@Override
	public Boolean activateKeyUp(Hitpoint hitpoint, int scancode, boolean control)
	{
		if(scancode == Keybindings.KeyDelete)
		{
			Part toBeDeleted = hitpoint.getHitPart();
			if(toBeDeleted != null)
			{
				if(toBeDeleted instanceof Connector)
				{
					toBeDeleted = toBeDeleted.getParent();
				}
				if(toBeDeleted instanceof CompContainer && !((CompContainer) toBeDeleted).isEmpty())
				{
					System.out.println("Cannot delete containers with components yet.");
					return false;
				}
				if(toBeDeleted.getParent() == null)
				{
					//Either this is a root-board, or its already about to be deleted.
					System.out.println("Cannot delete the root-board, or this component is already about to be deleted.");
					return false;
				}
				//Now deleting:
				
				//Delete the parent to prevent this component to be deleted another time. And some other reasons.
				final Component parent = toBeDeleted.getParent();
				toBeDeleted.setParent(null);
				
				if(toBeDeleted instanceof CompContainer)
				{
					CompContainer container = (CompContainer) toBeDeleted;
					gpuTasks.add((worldRenderer) -> {
						worldMesh.removeComponent(container, simulation);
						CompContainer parentContainer = (CompContainer) parent;
						parentContainer.remove(container);
						parentContainer.updateBounds();
						
						worldRenderer.toolDisable();
					});
				}
				else if(toBeDeleted instanceof CompWireRaw)
				{
					final CompWireRaw wireToDelete = (CompWireRaw) toBeDeleted;
					simulation.updateJobNextTickThreadSafe((simulation) -> {
						List<Cluster> modifiedClusters = new ArrayList<>(3);
						modifiedClusters.add(wireToDelete.getCluster()); //If this wire is somehow affiliated with the highlighting, the directly connected clusters are listed, thus only add the wire-cluster. Same as blot. And if pegs, also same or known.
						
						Map<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> updates = new HashMap<>();
						ClusterHelper.removeWire(simulation, wireToDelete, updates);
						
						//However after removal, the two clusters of the blot might have changed, in a way that original wire-cluster fully vanished. Safety first, lets add them.
						modifiedClusters.add(wireToDelete.getConnectorA().getCluster());
						modifiedClusters.add(wireToDelete.getConnectorB().getCluster());
						sharedData.getRenderPlane3D().clustersChanged(modifiedClusters);
						
						gpuTasks.add((worldRenderer) -> {
							System.out.println("[ClusterUpdateDebug] Updating " + updates.size() + " conductor mesh bags.");
							for(Map.Entry<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> entry : updates.entrySet())
							{
								entry.getKey().handleUpdates(entry.getValue(), simulation);
							}
							board.getWiresToRender().remove(wireToDelete);
							wireRayCaster.removeWire(wireToDelete);
							worldMesh.removeComponent(wireToDelete, simulation);
							
							worldRenderer.toolDisable();
						});
					});
				}
				else if(toBeDeleted instanceof Component)
				{
					final Component component = (Component) toBeDeleted;
					//TODO: Get rid of this bad boi code: Do not loop over wires on the input thread, use the simulation thread instead.
					if(toBeDeleted instanceof CompSnappingPeg)
					{
						for(Wire wire : ((ConnectedComponent) component).getPegs().get(0).getWires())
						{
							if(wire instanceof CompSnappingWire)
							{
								CompSnappingPeg sPeg = (CompSnappingPeg) toBeDeleted;
								simulation.updateJobNextTickThreadSafe((simulation) -> {
									Map<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> updates = new HashMap<>();
									ClusterHelper.removeWire(simulation, wire, updates);
									sPeg.getPartner().setPartner(null);
									sPeg.setPartner(null);
									gpuTasks.add((unused) -> {
										System.out.println("[ClusterUpdateDebug] Updating " + updates.size() + " conductor mesh bags.");
										for(Map.Entry<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> entry : updates.entrySet())
										{
											entry.getKey().handleUpdates(entry.getValue(), simulation);
										}
										worldMesh.removeComponent((CompSnappingWire) wire, simulation);
									});
								});
								break;
							}
						}
					}
					
					simulation.updateJobNextTickThreadSafe((simulation) -> {
						List<Wire> wiresToRemove = new ArrayList<>();
						Map<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> updates = new HashMap<>();
						if(component instanceof ConnectedComponent)
						{
							ConnectedComponent con = (ConnectedComponent) component;
							List<Cluster> modifiedClusters = new ArrayList<>(con.getConnectors().size());
							for(Blot blot : con.getBlots())
							{
								modifiedClusters.add(blot.getCluster());
								ClusterHelper.removeBlot(simulation, blot, updates);
								wiresToRemove.addAll(blot.getWires());
							}
							for(Peg peg : con.getPegs())
							{
								modifiedClusters.add(peg.getCluster());
								ClusterHelper.removePeg(simulation, peg, updates);
								wiresToRemove.addAll(peg.getWires());
							}
							for(Wire wire : wiresToRemove)
							{
								if(wire instanceof HiddenWire)
								{
									continue;
								}
								((Part) wire).setParent(null); //Mark as deleted.
							}
							sharedData.getRenderPlane3D().clustersChanged(modifiedClusters);
						}
						
						gpuTasks.add((worldRenderer) -> {
							System.out.println("[ClusterUpdateDebug] Updating " + updates.size() + " conductor mesh bags.");
							for(Map.Entry<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> entry : updates.entrySet())
							{
								entry.getKey().handleUpdates(entry.getValue(), simulation);
							}
							for(Wire wire : wiresToRemove)
							{
								//At this point snapping wires are already removed, since at this point the simulation task already ran.
								if(wire.getClass() == HiddenWire.class)
								{
									//Ignore hidden wires, they won't be saved/loaded/displayed.
									continue;
								}
								board.getWiresToRender().remove(wire); //Non-Visible wires got filtered before.
								wireRayCaster.removeWire((CompWireRaw) wire);
								worldMesh.removeComponent((CompWireRaw) wire, simulation);
							}
							if(component instanceof CompLabel)
							{
								((CompLabel) component).unload();
								board.getLabelsToRender().remove(component);
							}
							
							if(parent != null)
							{
								CompContainer parentContainer = (CompContainer) parent;
								parentContainer.remove(component);
								parentContainer.updateBounds();
							}
							
							worldMesh.removeComponent(component, simulation);
							
							worldRenderer.toolDisable();
						});
					});
				}
				else
				{
					System.out.println("Unknown part to delete: " + toBeDeleted.getClass().getSimpleName());
					toBeDeleted.setParent(parent);
					return false;
				}
				return true;
			}
			return null;
		}
		return null;
	}
}

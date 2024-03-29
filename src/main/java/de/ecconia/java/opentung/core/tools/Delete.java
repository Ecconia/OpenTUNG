package de.ecconia.java.opentung.core.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

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
						
						//If a currently non-selected source-cluster wire gets deleted, the connected cluster might be semi-highlighted and thus needs to be expanded.
						// Thus also register the new clusters:
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
					simulation.updateJobNextTickThreadSafe((simulation) -> {
						List<Wire> wiresToRemove = new ArrayList<>();
						Map<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> updates = new HashMap<>();
						CompSnappingWire snappingWire = null;
						if(component instanceof ConnectedComponent)
						{
							ConnectedComponent con = (ConnectedComponent) component;
							List<Cluster> modifiedClusters = new ArrayList<>(con.getConnectors().size());
							if(component instanceof CompSnappingPeg)
							{
								CompSnappingPeg snappingPeg = (CompSnappingPeg) component;
								if(snappingPeg.hasPartner())
								{
									for(Wire wire : con.getPegs().get(0).getWires())
									{
										if(wire instanceof CompSnappingWire)
										{
											//Detect this wire first, before it gets deleted by normal wire handling.
											snappingWire = (CompSnappingWire) wire;
											snappingPeg.setParent(null);
											modifiedClusters.add(wire.getCluster()); //Add this cluster first, since once it got removed. The original cluster could be gone.
											ClusterHelper.removeWire(simulation, wire, updates);
											//The snapping wire cannot be connected to a blot. Means both sides and the wire itself are only one cluster.
											// If it is highlighted, it already is remembered. If not highlighted, nothing changes, since both sides still would be a not highlighted cluster.
											CompSnappingPeg partner = snappingPeg.getPartner();
											partner.setPartner(null);
											snappingPeg.setPartner(null);
											break;
										}
									}
								}
							}
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
						
						CompSnappingWire finalSnappingWire = snappingWire;
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
							if(finalSnappingWire != null)
							{
								worldMesh.removeComponent(finalSnappingWire, simulation);
							}
							if(component instanceof CompLabel)
							{
								//Assume there is a texture, code can handle that.
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
				return false;
			}
			return null;
		}
		return null;
	}
}

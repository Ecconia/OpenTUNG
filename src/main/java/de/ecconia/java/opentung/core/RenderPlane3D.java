package de.ecconia.java.opentung.core;

import de.ecconia.java.opentung.OpenTUNG;
import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.CompLabel;
import de.ecconia.java.opentung.components.CompPanelLabel;
import de.ecconia.java.opentung.components.CompPeg;
import de.ecconia.java.opentung.components.CompSnappingPeg;
import de.ecconia.java.opentung.components.CompSnappingWire;
import de.ecconia.java.opentung.components.CompThroughPeg;
import de.ecconia.java.opentung.components.conductor.Blot;
import de.ecconia.java.opentung.components.conductor.CompWireRaw;
import de.ecconia.java.opentung.components.conductor.Connector;
import de.ecconia.java.opentung.components.conductor.Peg;
import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.CubeOpenRotated;
import de.ecconia.java.opentung.components.fragments.Meshable;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.Holdable;
import de.ecconia.java.opentung.components.meta.Part;
import de.ecconia.java.opentung.components.meta.PlaceableInfo;
import de.ecconia.java.opentung.inputs.Controller3D;
import de.ecconia.java.opentung.inputs.InputProcessor;
import de.ecconia.java.opentung.libwrap.Matrix;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;
import de.ecconia.java.opentung.meshing.ConductorMeshBag;
import de.ecconia.java.opentung.meshing.MeshBagContainer;
import de.ecconia.java.opentung.raycast.RayCastResult;
import de.ecconia.java.opentung.raycast.WireRayCaster;
import de.ecconia.java.opentung.settings.Settings;
import de.ecconia.java.opentung.simulation.Cluster;
import de.ecconia.java.opentung.simulation.ClusterHelper;
import de.ecconia.java.opentung.simulation.HiddenWire;
import de.ecconia.java.opentung.simulation.InheritingCluster;
import de.ecconia.java.opentung.simulation.SourceCluster;
import de.ecconia.java.opentung.simulation.Updateable;
import de.ecconia.java.opentung.simulation.Wire;
import de.ecconia.java.opentung.units.IconGeneration;
import de.ecconia.java.opentung.units.LabelToolkit;
import de.ecconia.java.opentung.util.Ansi;
import de.ecconia.java.opentung.util.FourDirections;
import de.ecconia.java.opentung.util.MinMaxBox;
import de.ecconia.java.opentung.util.math.MathHelper;
import de.ecconia.java.opentung.util.math.Quaternion;
import de.ecconia.java.opentung.util.math.Vector3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.lwjgl.opengl.GL30;

public class RenderPlane3D implements RenderPlane
{
	private Camera camera;
	
	private final InputProcessor inputHandler;
	
	private final MeshBagContainer worldMesh;
	
	private final List<Vector3> wireEndsToRender = new ArrayList<>();
	private final LabelToolkit labelToolkit = new LabelToolkit();
	private final BlockingQueue<GPUTask> gpuTasks = new LinkedBlockingQueue<>();
	private final SharedData sharedData;
	private final ShaderStorage shaderStorage;
	
	private final WireRayCaster wireRayCaster;
	
	//TODO: Remove this thing again from here. But later when there is more management.
	private final BoardUniverse board;
	
	private Part currentlySelected; //What the camera is currently looking at.
	private Cluster clusterToHighlight;
	private List<Connector> connectorsToHighlight = new ArrayList<>();
	
	public RenderPlane3D(InputProcessor inputHandler, BoardUniverse board, SharedData sharedData)
	{
		this.board = board;
		this.wireRayCaster = new WireRayCaster();
		board.startFinalizeImport(gpuTasks, wireRayCaster);
		this.inputHandler = inputHandler;
		this.sharedData = sharedData;
		this.shaderStorage = sharedData.getShaderStorage();
		sharedData.setGPUTasks(gpuTasks);
		sharedData.setRenderPlane3D(this);
		this.worldMesh = new MeshBagContainer(sharedData.getShaderStorage());
		this.secondaryMesh = new MeshBagContainer(sharedData.getShaderStorage());
	}
	
	public void prepareSaving(AtomicInteger pauseArrived)
	{
		board.getSimulation().pauseSimulation(pauseArrived);
		gpuTasks.add((unused) -> {
			currentlySelected = null;
			placementData = null;
			boardIsBeingDragged = false;
			wireStartPoint = null;
			pauseArrived.incrementAndGet();
		});
	}
	
	public void postSave()
	{
		board.getSimulation().resumeSimulation();
	}
	
	//Other:
	
	private PlacementData placementData; //Scope purely render, read by copy.
	private boolean fullyLoaded;
	
	//Rotation fix variables:
	private Vector3 fixXAxis = Vector3.xp; //Never NPE, use +X as default.
	private Vector3 lastUpNormal = Vector3.yp; //Used whenever an item is shadow drawn, to use more natural rotations.
	
	//Board specific values:
	private boolean placeableBoardIsLaying = true;
	private boolean boardIsBeingDragged = false; //Scope input/(render), read on many places.
	
	//Grabbing stuff:
	private CompContainer grabbedParent;
	private Component grabbedComponent;
	private List<Wire> grabbedWires;
	private double grabRotation;
	private final MeshBagContainer secondaryMesh;
	
	//Input handling:
	
	private Controller3D controller;
	private Connector wireStartPoint; //Selected by dragging from a connector.
	private double placementRotation;
	
	public Part getCursorObject()
	{
		return currentlySelected;
	}
	
	public boolean isGrabbing()
	{
		return grabbedComponent != null;
	}
	
	//Click events:
	
	public void componentLeftClicked(Part part)
	{
		part.leftClicked(board.getSimulation());
	}
	
	public void componentLeftHold(Holdable holdable)
	{
		holdable.setHold(true, board.getSimulation());
	}
	
	public void componentLeftUnHold(Holdable holdable)
	{
		holdable.setHold(false, board.getSimulation());
	}
	
	public void componentRightClicked(Part part)
	{
		//TODO: Move this somewhere more generic.
		Cluster cluster = null;
		if(part instanceof CompBoard && sharedData.getCurrentPlaceable() == CompBoard.info)
		{
			//Rightclicked while placing a board -> change layout:
			placeableBoardIsLaying = !placeableBoardIsLaying;
			return;
		}
		if(part instanceof CompWireRaw)
		{
			cluster = ((CompWireRaw) part).getCluster();
		}
		else if(part instanceof CompThroughPeg || part instanceof CompPeg || part instanceof CompSnappingPeg)
		{
			cluster = ((Component) part).getPegs().get(0).getCluster();
		}
		else if(part instanceof Connector)
		{
			cluster = ((Connector) part).getCluster();
		}
		
		if(cluster != null)
		{
			Cluster fCluster = cluster;
			gpuTasks.add(new GPUTask()
			{
				@Override
				public void execute(RenderPlane3D world3D)
				{
					if(clusterToHighlight == fCluster)
					{
						clusterToHighlight = null;
						connectorsToHighlight = new ArrayList<>();
					}
					else
					{
						clusterToHighlight = fCluster;
						connectorsToHighlight = fCluster.getConnectors();
					}
				}
			});
		}
	}
	
	public void rightDragOnConnector(Connector connector)
	{
		wireStartPoint = connector;
	}
	
	public void rightDragOnConnectorStop(Connector connector)
	{
		Connector from = wireStartPoint;
		wireStartPoint = null;
		
		if(!fullyLoaded)
		{
			return;
		}
		
		if(connector != null)
		{
			Connector to = connector;
			
			if(from instanceof Blot && to instanceof Blot)
			{
				System.out.println("Blot-Blot connections are not allowed, cause pointless.");
				return;
			}
			
			for(Wire wire : from.getWires())
			{
				if(wire.getOtherSide(from) == to)
				{
					System.out.println("Already connected.");
					return;
				}
			}
			
			if(from.getCluster() == clusterToHighlight || to.getCluster() == clusterToHighlight)
			{
				gpuTasks.add((unused) -> {
					clusterToHighlight = null;
					connectorsToHighlight = new ArrayList<>();
				});
			}
			
			//Add wire:
			CompWireRaw newWire;
			{
				newWire = new CompWireRaw(board.getPlaceboWireParent());
				
				Vector3 fromPos = from.getConnectionPoint();
				Vector3 toPos = to.getConnectionPoint();
				
				//Pos + Rot
				Vector3 direction = fromPos.subtract(toPos).divide(2);
				double distance = direction.length();
				Quaternion rotation = MathHelper.rotationFromVectors(Vector3.zp, direction.normalize());
				Vector3 position = toPos.add(direction);
				newWire.setRotation(rotation);
				newWire.setPosition(position);
				newWire.setLength((float) distance * 2f);
			}
			
			Cluster wireCluster;
			
			board.getSimulation().updateJobNextTickThreadSafe((simulation) -> {
				Map<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> updates = new HashMap<>();
				//Places the wires and updates clusters as needed. Also finishes the wire linking.
				ClusterHelper.placeWire(simulation, board, from, to, newWire, updates);
				
				//Once it is fully prepared by simulation thread, cause the graphic thread to draw it.
				try
				{
					gpuTasks.put((ignored) -> {
						System.out.println("[ClusterUpdateDebug] Updating " + updates.size() + " conductor mesh bags.");
						for(Map.Entry<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> entry : updates.entrySet())
						{
							entry.getKey().handleUpdates(entry.getValue(), board.getSimulation());
						}
						//Add the wire to the mesh sources
						board.getWiresToRender().add(newWire);
						wireRayCaster.addWire(newWire);
						worldMesh.addComponent(newWire, board.getSimulation());
					});
				}
				catch(InterruptedException e)
				{
					e.printStackTrace();
				}
			});
		}
	}
	
	public void rotatePlacement(double degrees)
	{
		if(isGrabbing())
		{
			grabRotation += degrees;
			if(grabRotation >= 360)
			{
				grabRotation -= 360;
			}
			if(grabRotation <= 0)
			{
				grabRotation += 360;
			}
		}
		else
		{
			placementRotation += degrees;
			if(placementRotation >= 360)
			{
				placementRotation -= 360;
			}
			if(placementRotation <= 0)
			{
				placementRotation += 360;
			}
		}
	}
	
	public void placementStart()
	{
		if(placementData != null && sharedData.getCurrentPlaceable() == CompBoard.info)
		{
			//Start dragging until end.
			boardIsBeingDragged = true; //It is unsure, if the last or new frames placement position will be used...
		}
	}
	
	public boolean attemptPlacement()
	{
		PlacementData placement = placementData;
		boolean boardIsBeingDraggedCopy = boardIsBeingDragged;
		boardIsBeingDragged = false; //Resets this boolean, if for a reason its not resetted - ugly.
		
		if(wireStartPoint != null)
		{
			return false; //We are dragging a wire, don't place something!
		}
		
		if(!fullyLoaded)
		{
			return false;
		}
		
		if(placement == null)
		{
			return false;
		}
		if(placement.getParentBoard() != board.getRootBoard() && placement.getParentBoard().getParent() == null)
		{
			System.out.println("Board attempted to place on is deleted/gone.");
			return false;
		}
		
		PlaceableInfo currentPlaceable = sharedData.getCurrentPlaceable();
		if(isGrabbing())
		{
			//Calculate the new alignment:
			Quaternion newAlignment = MathHelper.rotationFromVectors(Vector3.yp, placement.getNormal());
			double normalAxisRotationAngle = -grabRotation + calculateFixRotationOffset(newAlignment);
			Quaternion normalAxisRotation = Quaternion.angleAxis(normalAxisRotationAngle, placementData.getNormal());
			newAlignment = newAlignment.multiply(normalAxisRotation);
			
			//Apply new position and alignment:
			grabbedComponent.setPosition(placement.getPosition()); //New position
			grabbedComponent.setRotation(newAlignment);
			
			//Update positions and alignment of wires, they inherit the position from the grabbed component.
			for(Wire wire : grabbedWires)
			{
				if(wire instanceof HiddenWire)
				{
					continue;
				}
				Vector3 thisPos = wire.getConnectorA().getConnectionPoint();
				Vector3 thatPos = wire.getConnectorB().getConnectionPoint();
				
				Vector3 direction = thisPos.subtract(thatPos).divide(2);
				double distance = direction.length();
				Quaternion rotation = MathHelper.rotationFromVectors(Vector3.zp, direction.normalize());
				Vector3 position = thatPos.add(direction);
				
				CompWireRaw cWire = (CompWireRaw) wire;
				cWire.setPosition(position);
				cWire.setRotation(rotation);
				cWire.setLength((float) distance * 2f);
			}
			
			gpuTasks.add((unused) -> {
				worldMesh.addComponent(grabbedComponent, board.getSimulation());
				grabbedComponent.setParent(placement.getParentBoard());
				placement.getParentBoard().addChild(grabbedComponent);
				grabbedComponent.updateBoundsDeep();
				placement.getParentBoard().updateBounds();
				for(Wire wire : grabbedWires)
				{
					CompWireRaw cWire = (CompWireRaw) wire;
					board.getWiresToRender().add(cWire);
					worldMesh.addComponent(cWire, board.getSimulation());
					wireRayCaster.addWire(cWire);
				}
				if(grabbedComponent instanceof CompLabel && ((CompLabel) grabbedComponent).hasTexture())
				{
					board.getLabelsToRender().add((CompLabel) grabbedComponent);
				}
				
				grabbedComponent = null;
				grabbedWires = null;
			});
			
			return true;
		}
		//TODO: Ugly, not thread-safe enough for my taste. Might even cause bugs. So eventually it has to be changed.
		else if(currentPlaceable != null)
		{
			boolean isPlacingBoard = currentPlaceable == CompBoard.info;
			
			Quaternion newAlignment = MathHelper.rotationFromVectors(Vector3.yp, placement.getNormal());
			double normalAxisRotationAngle = -placementRotation + calculateFixRotationOffset(newAlignment);
			Quaternion normalAxisRotation = Quaternion.angleAxis(normalAxisRotationAngle, placementData.getNormal());
			Quaternion finalAlignment = newAlignment.multiply(normalAxisRotation);
			if(isPlacingBoard)
			{
				Quaternion boardAlignment = Quaternion.angleAxis(placeableBoardIsLaying ? 0 : 90, Vector3.xn);
				finalAlignment = boardAlignment.multiply(finalAlignment);
			}
			
			Vector3 position = placement.getPosition();
			Component newComponent;
			if(isPlacingBoard)
			{
				int x = 1;
				int z = 1;
				
				//TODO: Using camera position on the non-render thread is not okay.
				
				//Get camera position and ray and convert them into board space:
				Vector3 cameraPosition = camera.getPosition();
				Vector3 cameraRay = Vector3.zp;
				cameraRay = Quaternion.angleAxis(camera.getNeck(), Vector3.xn).multiply(cameraRay);
				cameraRay = Quaternion.angleAxis(camera.getRotation(), Vector3.yn).multiply(cameraRay);
				Vector3 cameraRayBoardSpace = finalAlignment.multiply(cameraRay);
				Vector3 cameraPositionBoardSpace = finalAlignment.multiply(cameraPosition.subtract(position));
				
				//Get collision point with area Y=0:
				double distance = -cameraPositionBoardSpace.getY() / cameraRayBoardSpace.getY();
				double cameraDistance = cameraRayBoardSpace.length();
				Vector3 distanceVector = cameraRayBoardSpace.multiply(distance);
				double dragDistance = distanceVector.length();
				if(dragDistance - cameraDistance > 20)
				{
					//TBI: Is this okay?
					distanceVector = distanceVector.multiply(1.0 / distanceVector.length() * 20);
				}
				Vector3 collisionPoint = cameraPositionBoardSpace.add(distanceVector);
				if(distance >= 0)
				{
					//Y should be at 0 or very close to it - x and z can be used as are.
					x = (int) ((Math.abs(collisionPoint.getX()) + 0.15f) / 0.3f) + 1;
					z = (int) ((Math.abs(collisionPoint.getZ()) + 0.15f) / 0.3f) + 1;
					Vector3 roundedCollisionPoint = new Vector3((x - 1) * 0.15 * (collisionPoint.getX() >= 0 ? 1f : -1f), 0, (z - 1) * 0.15 * (collisionPoint.getZ() >= 0 ? 1f : -1f));
					position = position.add(finalAlignment.inverse().multiply(roundedCollisionPoint));
				}
				newComponent = new CompBoard(placement.getParentBoard(), x, z);
			}
			else
			{
				newComponent = currentPlaceable.instance(placement.getParentBoard());
			}
			newComponent.setRotation(finalAlignment);
			newComponent.setPosition(position);
			
			if(isPlacingBoard)
			{
				try
				{
					gpuTasks.put((ignored) -> {
						placement.getParentBoard().addChild(newComponent);
						placement.getParentBoard().updateBounds();
						worldMesh.addComponent(newComponent, board.getSimulation());
					});
				}
				catch(InterruptedException e)
				{
					e.printStackTrace();
				}
				return true; //Don't do all the other checks, obsolete.
			}
			
			newComponent.init(); //Inits components such as the ThroughPeg (needs to be called after position is set).
			
			//TODO: Make generic
			if(currentPlaceable == CompThroughPeg.info)
			{
				//TODO: Especially with modded components, this init here has to function generically for all components. (Perform cluster exploration).
				Cluster cluster = new InheritingCluster();
				Peg first = newComponent.getPegs().get(0);
				Peg second = newComponent.getPegs().get(1);
				cluster.addConnector(first);
				first.setCluster(cluster);
				cluster.addConnector(second);
				second.setCluster(cluster);
				cluster.addWire(first.getWires().get(0));
			}
			else
			{
				for(Peg peg : newComponent.getPegs())
				{
					Cluster cluster = new InheritingCluster();
					cluster.addConnector(peg);
					peg.setCluster(cluster);
				}
				for(Blot blot : newComponent.getBlots())
				{
					Cluster cluster = new SourceCluster(blot);
					cluster.addConnector(blot);
					blot.setCluster(cluster);
				}
			}
			
			if(newComponent instanceof Updateable)
			{
				board.getSimulation().updateNextTickThreadSafe((Updateable) newComponent);
			}
			
			try
			{
				gpuTasks.put((ignored) -> {
					placement.getParentBoard().addChild(newComponent);
					placement.getParentBoard().updateBounds();
					worldMesh.addComponent(newComponent, board.getSimulation());
				});
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
			return true;
		}
		
		return false;
	}
	
	public void delete(Part toBeDeleted)
	{
		if(isGrabbing())
		{
			return;
		}
		if(boardIsBeingDragged)
		{
			return;
		}
		if(toBeDeleted instanceof Connector)
		{
			toBeDeleted = toBeDeleted.getParent();
		}
		if(toBeDeleted instanceof CompContainer && !((CompContainer) toBeDeleted).isEmpty())
		{
			System.out.println("Cannot delete containers with components yet.");
			return;
		}
		if(toBeDeleted.getParent() == null)
		{
			//Either this is a root-board, or its already about to be deleted.
			System.out.println("Cannot delete the root-board, or this component is already about to be deleted.");
			return;
		}
		//Delete the parent to prevent this component to be deleted another time. And some other reasons.
		final Component parent = toBeDeleted.getParent();
		toBeDeleted.setParent(null);
		
		if(toBeDeleted instanceof CompContainer)
		{
			CompContainer container = (CompContainer) toBeDeleted;
			gpuTasks.add((unused) -> {
				worldMesh.removeComponent(container, board.getSimulation());
				CompContainer parentConainer = (CompContainer) parent;
				parentConainer.remove(container);
				parentConainer.updateBounds();
			});
		}
		else if(toBeDeleted instanceof CompWireRaw)
		{
			final CompWireRaw wireToDelete = (CompWireRaw) toBeDeleted;
			
			board.getSimulation().updateJobNextTickThreadSafe((simulation) -> {
				Map<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> updates = new HashMap<>();
				ClusterHelper.removeWire(simulation, wireToDelete, updates);
				
				gpuTasks.add((unused) -> {
					System.out.println("[ClusterUpdateDebug] Updating " + updates.size() + " conductor mesh bags.");
					for(Map.Entry<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> entry : updates.entrySet())
					{
						entry.getKey().handleUpdates(entry.getValue(), board.getSimulation());
					}
					if(clusterToHighlight == wireToDelete.getCluster())
					{
						clusterToHighlight = null;
						connectorsToHighlight = new ArrayList<>();
					}
					board.getWiresToRender().remove(wireToDelete);
					wireRayCaster.removeWire(wireToDelete);
					worldMesh.removeComponent(wireToDelete, board.getSimulation());
				});
			});
		}
		else if(toBeDeleted instanceof Component)
		{
			final Component component = (Component) toBeDeleted;
			if(toBeDeleted instanceof CompSnappingPeg)
			{
				for(Wire wire : component.getPegs().get(0).getWires())
				{
					if(wire instanceof CompSnappingWire)
					{
						CompSnappingPeg sPeg = (CompSnappingPeg) toBeDeleted;
						board.getSimulation().updateJobNextTickThreadSafe((simulation) -> {
							Map<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> updates = new HashMap<>();
							ClusterHelper.removeWire(simulation, wire, updates);
							sPeg.getPartner().setPartner(null);
							sPeg.setPartner(null);
							gpuTasks.add((unused) -> {
								System.out.println("[ClusterUpdateDebug] Updating " + updates.size() + " conductor mesh bags.");
								for(Map.Entry<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> entry : updates.entrySet())
								{
									entry.getKey().handleUpdates(entry.getValue(), board.getSimulation());
								}
								worldMesh.removeComponent((CompSnappingWire) wire, board.getSimulation());
							});
						});
						break;
					}
				}
			}
			
			board.getSimulation().updateJobNextTickThreadSafe((simulation) -> {
				List<Wire> wiresToRemove = new ArrayList<>();
				Map<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> updates = new HashMap<>();
				for(Blot blot : component.getBlots())
				{
					ClusterHelper.removeBlot(simulation, blot, updates);
					wiresToRemove.addAll(blot.getWires());
				}
				for(Peg peg : component.getPegs())
				{
					ClusterHelper.removePeg(simulation, peg, updates);
					wiresToRemove.addAll(peg.getWires());
				}
				
				gpuTasks.add((unused) -> {
					System.out.println("[ClusterUpdateDebug] Updating " + updates.size() + " conductor mesh bags.");
					for(Map.Entry<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> entry : updates.entrySet())
					{
						entry.getKey().handleUpdates(entry.getValue(), board.getSimulation());
					}
					for(Connector connector : component.getConnectors())
					{
						if(clusterToHighlight == connector.getCluster())
						{
							clusterToHighlight = null;
							connectorsToHighlight = new ArrayList<>();
						}
					}
					for(Wire wire : wiresToRemove)
					{
						if(wire.getClass() == HiddenWire.class)
						{
							continue;
						}
						board.getWiresToRender().remove(wire);
						wireRayCaster.removeWire((CompWireRaw) wire);
						worldMesh.removeComponent((CompWireRaw) wire, board.getSimulation());
					}
					if(component instanceof CompLabel)
					{
						((CompLabel) component).unload();
						board.getLabelsToRender().remove(component);
					}
					
					if(parent != null)
					{
						CompContainer parentConainer = (CompContainer) parent;
						parentConainer.remove(component);
						parentConainer.updateBounds();
					}
					
					worldMesh.removeComponent(component, board.getSimulation());
				});
			});
		}
		else
		{
			System.out.println("Unknown part to delete: " + toBeDeleted.getClass().getSimpleName());
		}
	}
	
	public void grab(Component toBeGrabbed)
	{
		if(wireStartPoint != null)
		{
			return; //We are dragging a wire, don't grab something!
		}
		if(grabbedComponent != null || grabbedWires != null)
		{
			return;
		}
		if(toBeGrabbed instanceof Wire)
		{
			//We don't grab wires.
			return;
		}
		
		CompContainer parent = (CompContainer) toBeGrabbed.getParent();
		if(parent == null)
		{
			System.out.println("Can't grab component, since its either the root board or soon deleted.");
			return;
		}
		toBeGrabbed.setParent(null);
		
		if(toBeGrabbed instanceof CompContainer)
		{
			System.out.println("Cannot grab containers - yet.");
			return;
		}
		//Remove the snapping wire fully. TODO: Restore snapping peg wire when aborting grabbing.
		else if(toBeGrabbed instanceof CompSnappingPeg)
		{
			for(Wire wire : toBeGrabbed.getPegs().get(0).getWires())
			{
				if(wire instanceof CompSnappingWire)
				{
					CompSnappingPeg sPeg = (CompSnappingPeg) toBeGrabbed;
					board.getSimulation().updateJobNextTickThreadSafe((simulation) -> {
						Map<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> updates = new HashMap<>();
						ClusterHelper.removeWire(simulation, wire, updates);
						sPeg.getPartner().setPartner(null);
						sPeg.setPartner(null);
						gpuTasks.add((unused) -> {
							System.out.println("[ClusterUpdateDebug] Updating " + updates.size() + " conductor mesh bags.");
							for(Map.Entry<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> entry : updates.entrySet())
							{
								entry.getKey().handleUpdates(entry.getValue(), board.getSimulation());
							}
							worldMesh.removeComponent((CompSnappingWire) wire, board.getSimulation());
						});
					});
					break;
				}
			}
		}
		
		board.getSimulation().updateJobNextTickThreadSafe((unused) -> {
			//Collect wires:
			List<Wire> wires = new ArrayList<>();
			for(Connector connector : toBeGrabbed.getConnectors())
			{
				for(Wire wire : connector.getWires())
				{
					if(wire instanceof CompWireRaw)
					{
						wires.add(wire);
					}
				}
			}
			
			gpuTasks.add((unused2) -> {
				for(Connector connector : toBeGrabbed.getConnectors())
				{
					if(clusterToHighlight == connector.getCluster())
					{
						clusterToHighlight = null;
						connectorsToHighlight = new ArrayList<>();
					}
				}
				if(toBeGrabbed instanceof CompLabel)
				{
					//Remove regardless of it having texture or not.
					board.getLabelsToRender().remove(toBeGrabbed);
				}
				//Remove from meshes on render thread
				worldMesh.removeComponent(toBeGrabbed, board.getSimulation());
				for(Wire wire : wires)
				{
					board.getWiresToRender().remove(wire);
					worldMesh.removeComponent((CompWireRaw) wire, board.getSimulation());
					wireRayCaster.removeWire((CompWireRaw) wire);
				}
				//Parent is never null at this point.
				parent.remove(toBeGrabbed);
				parent.updateBounds();
				
				//Create construct to store the grabbed content (to be drawn).
				
				fixXAxis = toBeGrabbed.getRotation().inverse().multiply(Vector3.xp);
				lastUpNormal = toBeGrabbed.getRotation().inverse().multiply(Vector3.yp);
				secondaryMesh.addComponent(toBeGrabbed, board.getSimulation());
				grabRotation = 0;
				grabbedComponent = toBeGrabbed;
				grabbedParent = parent;
				grabbedWires = wires;
			});
		});
	}
	
	public void deleteGrabbed()
	{
		board.getSimulation().updateJobNextTickThreadSafe((unused) -> {
			List<Wire> wireCopy = grabbedWires;
			Component compCopy = grabbedComponent;
			if(wireCopy == null || compCopy == null)
			{
				//Was aborted. But data is no longer valid.
				return;
			}
			Map<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> updates = new HashMap<>();
			for(Wire wire : wireCopy)
			{
				ClusterHelper.removeWire(board.getSimulation(), wire, updates);
			}
			for(Peg peg : compCopy.getPegs())
			{
				ClusterHelper.removePeg(board.getSimulation(), peg, updates);
			}
			for(Blot blot : compCopy.getBlots())
			{
				ClusterHelper.removeBlot(board.getSimulation(), blot, updates);
			}
			
			gpuTasks.add((unused2) -> {
				System.out.println("[ClusterUpdateDebug] Updating " + updates.size() + " conductor mesh bags.");
				for(Map.Entry<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> entry : updates.entrySet())
				{
					entry.getKey().handleUpdates(entry.getValue(), board.getSimulation());
				}
				secondaryMesh.removeComponent(compCopy, board.getSimulation());
				
				//Thats pretty much it. Just make the clipboard invisible:
				grabbedWires = null;
				grabbedComponent = null;
				grabbedParent = null;
				
				if(compCopy instanceof CompLabel)
				{
					((CompLabel) compCopy).unload();
				}
			});
		});
	}
	
	public void abortGrabbing()
	{
		gpuTasks.add((unused) -> {
			//TODO: board.getComponentsToRender().add(grabbedComponent);
			for(Wire wire : grabbedWires)
			{
				CompWireRaw cwire = (CompWireRaw) wire;
				board.getWiresToRender().add(cwire);
				worldMesh.addComponent(cwire, board.getSimulation());
				wireRayCaster.addWire(cwire);
			}
			if(grabbedComponent instanceof CompLabel && ((CompLabel) grabbedComponent).hasTexture())
			{
				board.getLabelsToRender().add((CompLabel) grabbedComponent);
			}
			
			grabbedParent.addChild(grabbedComponent);
			grabbedParent.updateBounds();
			grabbedComponent.setParent(grabbedParent);
			secondaryMesh.removeComponent(grabbedComponent, board.getSimulation());
			worldMesh.addComponent(grabbedComponent, board.getSimulation());
			
			grabbedComponent = null;
			grabbedWires = null;
			grabbedParent = null;
		});
	}
	
	//Setup and stuff:
	
	@Override
	public void setup()
	{
		//TODO: Currently manually triggered, but to be optimized away.
		CompLabel.initGL();
		CompPanelLabel.initGL();
		System.out.println("Starting label generation.");
		labelToolkit.startProcessing(gpuTasks, board.getLabelsToRender());
		
		System.out.println("Broken wires rendered: " + board.getBrokenWires().size());
		if(!board.getBrokenWires().isEmpty())
		{
			board.getWiresToRender().clear();
			board.getWiresToRender().addAll(board.getBrokenWires()); //Debuggy
			for(CompWireRaw wire : board.getBrokenWires())
			{
				//TODO: Highlight which exactly failed (Or just remove this whole section, rip)
				wireEndsToRender.add(wire.getEnd1());
				wireEndsToRender.add(wire.getEnd2());
			}
		}
		
		camera = new Camera();
		
		worldMesh.setup(board, board.getWiresToRender(), board.getSimulation());
		
		gpuTasks.add(new GPUTask()
		{
			@Override
			public void execute(RenderPlane3D world3D)
			{
				IconGeneration.render(shaderStorage);
				//Restore the projection matrix and viewport of this shader, since they got abused.
				shaderStorage.resetViewportAndVisibleCubeShader();
			}
		});
		
		//Do not start receiving events before here. Be sure the whole thing is properly setted up.
		controller = new Controller3D(this);
		inputHandler.setController(controller);
		
		System.out.println("[Debug] Label amount: " + board.getLabelsToRender().size());
		System.out.println("[Debug] Wire amount: " + board.getWiresToRender().size());
	}
	
	public void refreshPostWorldLoad()
	{
		System.out.println("[MeshDebug] Post-World-Load:");
		worldMesh.rebuildConductorMeshes(board.getSimulation());
		board.getSimulation().start();
		fullyLoaded = true;
		sharedData.setSimulationLoaded(true);
		inputHandler.updatePauseMenu();
		System.out.println("[MeshDebug] P-W-L Done.");
	}
	
	public void calculatePlacementPosition()
	{
		if(boardIsBeingDragged)
		{
			return; //Don't change anything, the camera may look somewhere else in the meantime.
		}
		
		if(currentlySelected == null)
		{
			placementData = null; //Nothing to place on.
			return;
		}
		
		//TODO: Also allow the tip of Mounts :)
		
		//If looking at a board
		Part part = currentlySelected;
		if(!(part instanceof CompBoard))
		{
			placementData = null; //Only place on boards.
			return;
		}
		
		CompBoard board = (CompBoard) part;
		
		//TODO: Another ungeneric access
		CubeFull shape = (CubeFull) board.getModelHolder().getSolid().get(0);
		Vector3 position = board.getPosition();
		Quaternion rotation = board.getRotation();
		Vector3 size = shape.getSize();
		if(shape.getMapper() != null)
		{
			size = shape.getMapper().getMappedSize(size, board);
		}
		
		Vector3 cameraPosition = camera.getPosition();
		
		Vector3 cameraRay = Vector3.zp;
		cameraRay = Quaternion.angleAxis(camera.getNeck(), Vector3.xn).multiply(cameraRay);
		cameraRay = Quaternion.angleAxis(camera.getRotation(), Vector3.yn).multiply(cameraRay);
		Vector3 cameraRayBoardSpace = rotation.multiply(cameraRay);
		
		Vector3 cameraPositionBoardSpace = rotation.multiply(cameraPosition.subtract(position)); //Convert the camera position, in the board space.
		
		double distanceLocalMin = (size.getX() - cameraPositionBoardSpace.getX()) / cameraRayBoardSpace.getX();
		double distanceLocalMax = ((-size.getX()) - cameraPositionBoardSpace.getX()) / cameraRayBoardSpace.getX();
		double distanceGlobal;
		Vector3 normalGlobal;
		if(distanceLocalMin < distanceLocalMax)
		{
			distanceGlobal = distanceLocalMin;
			normalGlobal = Vector3.xp;
		}
		else
		{
			distanceGlobal = distanceLocalMax;
			normalGlobal = Vector3.xn;
		}
		
		distanceLocalMin = (size.getY() - cameraPositionBoardSpace.getY()) / cameraRayBoardSpace.getY();
		distanceLocalMax = ((-size.getY()) - cameraPositionBoardSpace.getY()) / cameraRayBoardSpace.getY();
		double distanceLocal;
		Vector3 normalLocal;
		if(distanceLocalMin < distanceLocalMax)
		{
			distanceLocal = distanceLocalMin;
			normalLocal = Vector3.yp;
		}
		else
		{
			distanceLocal = distanceLocalMax;
			normalLocal = Vector3.yn;
		}
		if(distanceGlobal < distanceLocal)
		{
			distanceGlobal = distanceLocal;
			normalGlobal = normalLocal;
		}
		
		distanceLocalMin = (size.getZ() - cameraPositionBoardSpace.getZ()) / cameraRayBoardSpace.getZ();
		distanceLocalMax = ((-size.getZ()) - cameraPositionBoardSpace.getZ()) / cameraRayBoardSpace.getZ();
		if(distanceLocalMin < distanceLocalMax)
		{
			distanceLocal = distanceLocalMin;
			normalLocal = Vector3.zp;
		}
		else
		{
			distanceLocal = distanceLocalMax;
			normalLocal = Vector3.zn;
		}
		if(distanceGlobal < distanceLocal)
		{
			distanceGlobal = distanceLocal;
			normalGlobal = normalLocal;
		}
		
		boolean isSide = normalGlobal.getY() == 0;
		int sign = normalGlobal.oneNegative() ? -1 : 1;
		Vector3 collisionPointBoardSpace = cameraPositionBoardSpace.add(cameraRayBoardSpace.multiply(distanceGlobal));
		if(isSide)
		{
			double x = collisionPointBoardSpace.getX();
			double z = collisionPointBoardSpace.getZ();
			if(normalGlobal.getX() == 0)
			{
				double xHalf = board.getX() * 0.15;
				double xcp = x + xHalf;
				int xSteps = (int) (xcp / 0.3);
				x = (xSteps) * 0.3 - xHalf + 0.15;
				z -= sign * 0.075;
			}
			else
			{
				double zHalf = board.getZ() * 0.15;
				double zcp = z + zHalf;
				int zSteps = (int) (zcp / 0.3);
				z = zSteps * 0.3 - zHalf + 0.15;
				x -= sign * 0.075;
			}
			
			collisionPointBoardSpace = new Vector3(x, 0, z);
		}
		else
		{
			double xHalf = board.getX() * 0.15;
			double zHalf = board.getZ() * 0.15;
			
			double xcp = collisionPointBoardSpace.getX() + xHalf;
			double zcp = collisionPointBoardSpace.getZ() + zHalf;
			
			int xSteps = (int) (xcp / 0.3);
			int zSteps = (int) (zcp / 0.3);
			
			collisionPointBoardSpace = new Vector3(xSteps * 0.3 + 0.15 - xHalf, 0, zSteps * 0.3 + 0.15 - zHalf);
		}
		
		Vector3 placementPosition = board.getRotation().inverse().multiply(collisionPointBoardSpace).add(board.getPosition());
		Vector3 placementNormal = board.getRotation().inverse().multiply(normalGlobal).normalize(); //Safety normalization.
		CompBoard placementBoard = board;
		
		if(sharedData.getCurrentPlaceable() == CompBoard.info && grabbedComponent == null)
		{
			//Boards have their center within, thus the offset needs to be adjusted:
			placementPosition = placementPosition.add(placementNormal.multiply(placeableBoardIsLaying ? 0.15 : (0.15 + 0.075)));
		}
		
		placementData = new PlacementData(placementPosition, placementNormal, placementBoard, normalGlobal);
	}
	
	@Override
	public void render()
	{
		while(!gpuTasks.isEmpty())
		{
			gpuTasks.poll().execute(this);
		}
		
		worldMesh.rebuildDirty(board.getSimulation());
		secondaryMesh.rebuildDirty(board.getSimulation());
		
		camera.lockLocation();
		controller.doFrameCycle();
		
		float[] view = camera.getMatrix();
		if(Settings.doRaycasting && !sharedData.isSaving() && fullyLoaded)
		{
//			long start = System.currentTimeMillis();
			cpuRaycast();
//			long duration = System.currentTimeMillis() - start;
//			System.out.println("Raycast time: " + duration + "ms");
		}
		calculatePlacementPosition();
		if(Settings.drawWorld)
		{
			OpenTUNG.setBackgroundColor();
			OpenTUNG.clear();
			
			drawDynamic(view);
			drawPlacementPosition(view); //Must be called before drawWireToBePlaced, currently!!!
			highlightCluster(view);
			drawWireToBePlaced(view);
			drawHighlight(view);
			
			ShaderProgram lineShader = shaderStorage.getLineShader();
			lineShader.use();
			lineShader.setUniformM4(1, view);
			Matrix model = new Matrix();
			if(Settings.drawWorldAxisIndicator)
			{
				GenericVAO axisIndicator = shaderStorage.getAxisIndicator();
				model.identity();
				Vector3 position = new Vector3(0, 10, 0);
				model.translate((float) position.getX(), (float) position.getY(), (float) position.getZ());
				lineShader.setUniformM4(2, model.getMat());
				axisIndicator.use();
				axisIndicator.draw();
			}
		}
	}
	
	private void drawWireToBePlaced(float[] view)
	{
		if(wireStartPoint == null)
		{
			return;
		}
		
		Vector3 startingPos = wireStartPoint.getConnectionPoint();
		
		Vector3 toPos;
		if(placementData == null)
		{
			toPos = null;
			Part currentlyLookingAt = getCursorObject();
			if(currentlyLookingAt instanceof Connector)
			{
				toPos = ((Connector) currentlyLookingAt).getConnectionPoint();
			}
		}
		else
		{
			toPos = placementData.getPosition();
			//Fix offset.
			toPos = toPos.add(placementData.getNormal().multiply(0.075));
		}
		
		if(toPos != null)
		{
			//Draw wire between placementPosition and startingPos:
			Vector3 direction = toPos.subtract(startingPos).divide(2);
			double distance = direction.length();
			Quaternion rotation = MathHelper.rotationFromVectors(Vector3.zp, direction.normalize());
			
			Matrix model = new Matrix();
			Vector3 position = startingPos.add(direction);
			model.translate((float) position.getX(), (float) position.getY(), (float) position.getZ());
			model.multiply(new Matrix(rotation.createMatrix()));
			Vector3 size = new Vector3(0.025, 0.01, distance);
			model.scale((float) size.getX(), (float) size.getY(), (float) size.getZ());
			
			ShaderProgram invisibleCubeShader = shaderStorage.getInvisibleCubeShader();
			invisibleCubeShader.use();
			invisibleCubeShader.setUniformM4(1, view);
			invisibleCubeShader.setUniformM4(2, model.getMat());
			invisibleCubeShader.setUniformV4(3, new float[]{1.0f, 0.0f, 1.0f, 1.0f});
			GenericVAO invisibleCube = shaderStorage.getInvisibleCube();
			invisibleCube.use();
			invisibleCube.draw();
		}
	}
	
	private void drawPlacementPosition(float[] view)
	{
		if(wireStartPoint != null)
		{
			return; //Don't draw the placement, while dragging a wire - its annoying.
		}
		if(placementData == null)
		{
			return;
		}
		
		if(isGrabbing())
		{
			//Calculate the new alignment:
			Quaternion newAlignment = MathHelper.rotationFromVectors(Vector3.yp, placementData.getNormal()); //Get the direction of the new placement position (with invalid rotation).
			double normalAxisRotationAngle = -grabRotation + calculateFixRotationOffset(newAlignment);
			Quaternion normalAxisRotation = Quaternion.angleAxis(normalAxisRotationAngle, placementData.getNormal()); //Create rotation Quaternion.
			newAlignment = newAlignment.multiply(normalAxisRotation); //Apply rotation onto new direction to get alignment.
			
			//Since the Rotation cannot be changed, it must be modified. So we undo the old rotation and apply the new one.
			Quaternion newDeltaAlignment = grabbedComponent.getRotation().inverse().multiply(newAlignment);
			
			Matrix modelMatrix = new Matrix();
			
			//Move the component to the new placement position;
			Vector3 newPosition = placementData.getPosition();
			Matrix tmpMat = new Matrix();
			tmpMat.translate(
					(float) newPosition.getX(),
					(float) newPosition.getY(),
					(float) newPosition.getZ());
			modelMatrix.multiply(tmpMat);
			
			//Rotate the mesh to new direction/rotation:
			modelMatrix.multiply(new Matrix(newDeltaAlignment.createMatrix()));
			
			//Move the component back to the world-origin:
			Vector3 oldPosition = grabbedComponent.getPosition();
			modelMatrix.translate(
					(float) -oldPosition.getX(),
					(float) -oldPosition.getY(),
					(float) -oldPosition.getZ()
			);
			
			//Set delta-model matrix (moves the component from prev to new pos, including all children.
			secondaryMesh.setModelMatrix(modelMatrix);
			secondaryMesh.draw(view);
			
			ShaderProgram visibleCubeShader = shaderStorage.getVisibleCubeShader();
			visibleCubeShader.use();
			visibleCubeShader.setUniformM4(1, view);
			GenericVAO visibleCube = shaderStorage.getVisibleOpTexCube();
			visibleCube.use();
			
			Matrix m = new Matrix();
			for(Wire wire : grabbedWires)
			{
				//TODO: When collecting outgoing wires from the board, they need to be separated into A and B side attached to grabbed.
				Vector3 thisPos = wire.getConnectorA().getConnectionPoint();
				Vector3 thatPos = wire.getConnectorB().getConnectionPoint();
				if(wire.getConnectorA().getParent() == grabbedComponent)
				{
					thisPos = thisPos.subtract(oldPosition);
					thisPos = newDeltaAlignment.inverse().multiply(thisPos);
					thisPos = thisPos.add(newPosition);
				}
				if(wire.getConnectorB().getParent() == grabbedComponent)
				{
					thatPos = thatPos.subtract(oldPosition);
					thatPos = newDeltaAlignment.inverse().multiply(thatPos);
					thatPos = thatPos.add(newPosition);
				}
				
				Vector3 direction = thisPos.subtract(thatPos).divide(2);
				double distance = direction.length();
				Quaternion wireAlignment = MathHelper.rotationFromVectors(Vector3.zp, direction.normalize());
				Vector3 position = thatPos.add(direction);
				
				m.identity();
				m.translate((float) position.getX(), (float) position.getY(), (float) position.getZ());
				m.multiply(new Matrix(wireAlignment.createMatrix()));
				m.scale(0.025f, 0.01f, (float) distance);
				//TODO: The color appears raw using this shader, as in unshaded.
				visibleCubeShader.setUniformV4(3, (wire.getCluster().isActive() ? Color.circuitON : Color.circuitOFF).asArray());
				visibleCubeShader.setUniformM4(2, m.getMat());
				visibleCube.draw();
			}
			
			//TODO: Iterate over all labels in the secondary mesh. (Currently only one).
			//TODO: Per Label on grabbed board, undo old board location, apply new board location. (If board that is).
			if(grabbedComponent instanceof CompLabel && ((CompLabel) grabbedComponent).hasTexture())
			{
				ShaderProgram sdfShader = shaderStorage.getSdfShader();
				CompLabel label = (CompLabel) grabbedComponent;
				sdfShader.use();
				sdfShader.setUniformM4(1, view);
				label.activate();
				m.identity();
				m.translate((float) newPosition.getX(), (float) newPosition.getY(), (float) newPosition.getZ());
				m.multiply(new Matrix(newAlignment.createMatrix()));
				sdfShader.setUniformM4(2, m.getMat());
				label.getModelHolder().drawTextures();
			}
			
			return;
		}
		
		PlaceableInfo currentPlaceable = sharedData.getCurrentPlaceable();
		if(currentPlaceable == null)
		{
			ShaderProgram lineShader = shaderStorage.getLineShader();
			//TODO: Switch to line shader with uniform color.
			lineShader.use();
			lineShader.setUniformM4(1, view);
			GL30.glLineWidth(5f);
			Matrix model = new Matrix();
			model.identity();
			Vector3 datPos = placementData.getPosition().add(placementData.getNormal().multiply(0.075));
			model.translate((float) datPos.getX(), (float) datPos.getY(), (float) datPos.getZ());
			lineShader.setUniformM4(2, model.getMat());
			GenericVAO crossyIndicator = shaderStorage.getCrossyIndicator();
			crossyIndicator.use();
			crossyIndicator.draw();
		}
		else if(currentPlaceable == CompBoard.info)
		{
			Quaternion newAlignment = MathHelper.rotationFromVectors(Vector3.yp, placementData.getNormal());
			double normalAxisRotationAngle = -placementRotation + calculateFixRotationOffset(newAlignment);
			Quaternion normalAxisRotation = Quaternion.angleAxis(normalAxisRotationAngle, placementData.getNormal());
			newAlignment = newAlignment.multiply(normalAxisRotation);
			//Specific board rotation:
			Quaternion boardAlignment = Quaternion.angleAxis(placeableBoardIsLaying ? 0 : 90, Vector3.xn);
			Quaternion finalRotation = boardAlignment.multiply(newAlignment);
			
			int x = 1;
			int z = 1;
			Vector3 position = placementData.getPosition();
			if(boardIsBeingDragged)
			{
				//Adjust position and size according to camera.
				
				//Get camera position and ray and convert them into board space:
				Vector3 cameraPosition = camera.getPosition();
				Vector3 cameraRay = Vector3.zp;
				cameraRay = Quaternion.angleAxis(camera.getNeck(), Vector3.xn).multiply(cameraRay);
				cameraRay = Quaternion.angleAxis(camera.getRotation(), Vector3.yn).multiply(cameraRay);
				Vector3 cameraRayBoardSpace = finalRotation.multiply(cameraRay);
				Vector3 cameraPositionBoardSpace = finalRotation.multiply(cameraPosition.subtract(position));
				
				//Get collision point with area Y=0:
				double distance = -cameraPositionBoardSpace.getY() / cameraRayBoardSpace.getY();
				double cameraDistance = cameraRayBoardSpace.length();
				Vector3 distanceVector = cameraRayBoardSpace.multiply(distance);
				double dragDistance = distanceVector.length();
				if(dragDistance - cameraDistance > 20)
				{
					//TBI: Is this okay?
					distanceVector = distanceVector.multiply(1.0 / distanceVector.length() * 20);
				}
				Vector3 collisionPoint = cameraPositionBoardSpace.add(distanceVector);
				if(distance >= 0)
				{
					//Y should be at 0 or very close to it - x and z can be used as are.
					x = (int) ((Math.abs(collisionPoint.getX()) + 0.15f) / 0.3f) + 1;
					z = (int) ((Math.abs(collisionPoint.getZ()) + 0.15f) / 0.3f) + 1;
					Vector3 roundedCollisionPoint = new Vector3((x - 1) * 0.15 * (collisionPoint.getX() >= 0 ? 1f : -1f), 0, (z - 1) * 0.15 * (collisionPoint.getZ() >= 0 ? 1f : -1f));
					position = position.add(finalRotation.inverse().multiply(roundedCollisionPoint));
				}
			}
			
			//TBI: Ehh skip the model? (For now yes, the component is very defined in TUNG and LW.
			Matrix matrix = new Matrix();
			//Apply global position:
			matrix.translate((float) position.getX(), (float) position.getY(), (float) position.getZ());
			matrix.multiply(new Matrix(finalRotation.createMatrix())); //Apply global rotation.
			//The cube is centered, no translation.
			matrix.scale((float) x * 0.15f, 0.075f, (float) z * 0.15f); //Just use the right size from the start... At this point in code it always has that size.
			
			//Draw the board:
			shaderStorage.getBoardTexture().activate();
			ShaderProgram textureCubeShader = shaderStorage.getTextureCubeShader();
			textureCubeShader.use();
			textureCubeShader.setUniformM4(1, view);
			textureCubeShader.setUniformM4(2, matrix.getMat());
			textureCubeShader.setUniformV2(3, new float[]{x, z});
			textureCubeShader.setUniformV4(4, Color.boardDefault.asArray());
			GenericVAO textureCube = shaderStorage.getVisibleOpTexCube();
			textureCube.use();
			textureCube.draw();
		}
		else
		{
			Quaternion newAlignment = MathHelper.rotationFromVectors(Vector3.yp, placementData.getNormal());
			double normalAxisRotationAngle = -placementRotation + calculateFixRotationOffset(newAlignment);
			Quaternion normalAxisRotation = Quaternion.angleAxis(normalAxisRotationAngle, placementData.getNormal());
			newAlignment = newAlignment.multiply(normalAxisRotation);
			ShaderProgram visibleCubeShader = shaderStorage.getVisibleCubeShader();
			GenericVAO visibleCube = shaderStorage.getVisibleOpTexCube();
			World3DHelper.drawModel(visibleCubeShader, visibleCube, currentPlaceable.getModel(), placementData.getPosition(), newAlignment, view);
		}
	}
	
	private double calculateFixRotationOffset(Quaternion newGlobalAlignment)
	{
		FourDirections axes = new FourDirections(placementData.getLocalNormal(), placementData.getParentBoard().getRotation());
		
		//Get the angle, from the new X axis, to an "optimal" X axis.
		Vector3 newVirtualXAxis = newGlobalAlignment.inverse().multiply(Vector3.xp);
		Vector3 newRandomXAxis = axes.getFitting(fixXAxis);
		boolean diff = newRandomXAxis == null;
		if(diff)
		{
			//All angles are 90° case. Rotate old axis.
			//TBI: Target * inverse(Source) = diff //Will that work, cause rotation?
			Quaternion normalRotation = MathHelper.rotationFromVectors(lastUpNormal, placementData.getNormal()).inverse();
			//TBI: Rotation may be 180° in that case its pretty much random, but reliably in most cases.
			newRandomXAxis = normalRotation.multiply(fixXAxis);
			Vector3 fallbackAxis = newRandomXAxis;
			newRandomXAxis = axes.getFitting(newRandomXAxis); //Replace with one of the 4, axes, although only minor change.
			if(newRandomXAxis == null)
			{
				System.out.println(Ansi.red + "[ERROR] ROTATION CODE FAILED!" + Ansi.r
						+ "\n Placement-Vector: " + placementData.getNormal()
						+ "\n LastNormal: " + lastUpNormal
						+ "\n RotationResult: " + fallbackAxis);
				//ChooseAnyAlternative:
				newRandomXAxis = axes.getA();
			}
		}
		fixXAxis = newRandomXAxis;
		
		double newRotation = MathHelper.angleFromVectors(newVirtualXAxis, newRandomXAxis);
		if(newRotation != 0 && newRotation != 180) //Edge cases.
		{
			//We now have the angle, but not the sign of the fix-angle.
			//For that span a plane between the new X axis and the normal (just use the Y-X plane).
			// and check if the optimal X-Axis is above or below (just check the Z value).
			Vector3 undoneNewAxis = newGlobalAlignment.multiply(newRandomXAxis); //Rotate axis back to default space.
			if(undoneNewAxis.getZ() < 0)
			{
				newRotation = -newRotation;
			}
		}
		lastUpNormal = newGlobalAlignment.inverse().multiply(Vector3.yp);
		return newRotation;
	}
	
	private void drawDynamic(float[] view)
	{
		worldMesh.draw(view);
		
		Matrix model = new Matrix();
		ShaderProgram sdfShader = shaderStorage.getSdfShader();
		sdfShader.use();
		sdfShader.setUniformM4(1, view);
		for(CompLabel label : board.getLabelsToRender())
		{
			label.activate();
			model.identity();
			model.translate((float) label.getPosition().getX(), (float) label.getPosition().getY(), (float) label.getPosition().getZ());
			Matrix rotMat = new Matrix(label.getRotation().createMatrix());
			model.multiply(rotMat);
			sdfShader.setUniformM4(2, model.getMat());
			label.getModelHolder().drawTextures();
		}
		
		if(!wireEndsToRender.isEmpty())
		{
			ShaderProgram lineShader = shaderStorage.getLineShader();
			GenericVAO crossyIndicator = shaderStorage.getCrossyIndicator();
			lineShader.use();
			lineShader.setUniformM4(1, view);
			
			for(Vector3 position : wireEndsToRender)
			{
				model.identity();
				model.translate((float) position.getX(), (float) position.getY(), (float) position.getZ());
				lineShader.setUniformM4(2, model.getMat());
				crossyIndicator.use();
				crossyIndicator.draw();
			}
		}
	}
	
	private void drawHighlight(float[] view)
	{
		if(isGrabbing())
		{
			return;
		}
		if(currentlySelected == null)
		{
			return;
		}
		
		Part part = currentlySelected;
		
		boolean isBoard = part instanceof CompBoard;
		boolean isWire = part instanceof CompWireRaw;
		if(
				isBoard && !Settings.highlightBoards
						|| isWire && !Settings.highlightWires
						|| !(isBoard || isWire) && !Settings.highlightComponents
		)
		{
			return;
		}
		
		//Enable drawing to stencil buffer
		GL30.glStencilMask(0xFF);
		
		ShaderProgram invisibleCubeShader = shaderStorage.getInvisibleCubeShader();
		GenericVAO invisibleCube = shaderStorage.getInvisibleCube();
		if(part instanceof Component)
		{
			World3DHelper.drawStencilComponent(invisibleCubeShader, invisibleCube, (Component) part, view);
		}
		else //Connector
		{
			invisibleCubeShader.use();
			invisibleCubeShader.setUniformM4(1, view);
			invisibleCubeShader.setUniformV4(3, new float[]{0, 0, 0, 0});
			Matrix matrix = new Matrix();
			World3DHelper.drawCubeFull(invisibleCubeShader, invisibleCube, ((Connector) part).getModel(), part, part.getParent().getModelHolder().getPlacementOffset(), new Matrix());
		}
		
		//Draw on top
		GL30.glDisable(GL30.GL_DEPTH_TEST);
		//Only draw if stencil bit is set.
		GL30.glStencilFunc(GL30.GL_EQUAL, 1, 0xFF);
		
		float[] color = new float[]{
				Settings.highlightColorR,
				Settings.highlightColorG,
				Settings.highlightColorB,
				Settings.highlightColorA
		};
		
		ShaderProgram planeShader = shaderStorage.getFlatPlaneShader();
		planeShader.use();
		planeShader.setUniformV4(0, color);
		GenericVAO fullCanvasPlane = shaderStorage.getFlatPlane();
		fullCanvasPlane.use();
		fullCanvasPlane.draw();
		
		//Restore settings:
		GL30.glStencilFunc(GL30.GL_NOTEQUAL, 1, 0xFF);
		GL30.glEnable(GL30.GL_DEPTH_TEST);
		//Clear stencil buffer:
		GL30.glClear(GL30.GL_STENCIL_BUFFER_BIT);
		//After clearing, disable usage/writing of/to stencil buffer again.
		GL30.glStencilMask(0x00);
	}
	
	private void highlightCluster(float[] view)
	{
		if(clusterToHighlight == null)
		{
			return;
		}
		
		//Enable drawing to stencil buffer
		GL30.glStencilMask(0xFF);
		
		ShaderProgram invisibleCubeShader = shaderStorage.getInvisibleCubeShader();
		GenericVAO invisibleCube = shaderStorage.getInvisibleCube();
		for(Wire wire : clusterToHighlight.getWires())
		{
			if(wire instanceof HiddenWire)
			{
				continue;
			}
			World3DHelper.drawStencilComponent(invisibleCubeShader, invisibleCube, (CompWireRaw) wire, view);
		}
		invisibleCubeShader.use();
		invisibleCubeShader.setUniformM4(1, view);
		invisibleCubeShader.setUniformV4(3, new float[]{0, 0, 0, 0});
		Matrix matrix = new Matrix();
		for(Connector connector : connectorsToHighlight)
		{
			World3DHelper.drawCubeFull(invisibleCubeShader, invisibleCube, connector.getModel(), connector.getParent(), connector.getParent().getModelHolder().getPlacementOffset(), matrix);
		}
		
		//Draw on top
		GL30.glDisable(GL30.GL_DEPTH_TEST);
		//Only draw if stencil bit is set.
		GL30.glStencilFunc(GL30.GL_EQUAL, 1, 0xFF);
		
		float[] color = new float[]{
				Settings.highlightClusterColorR,
				Settings.highlightClusterColorG,
				Settings.highlightClusterColorB,
				Settings.highlightClusterColorA
		};
		
		ShaderProgram planeShader = shaderStorage.getFlatPlaneShader();
		planeShader.use();
		planeShader.setUniformV4(0, color);
		GenericVAO fullCanvasPlane = shaderStorage.getFlatPlane();
		fullCanvasPlane.use();
		fullCanvasPlane.draw();
		
		//Restore settings:
		GL30.glStencilFunc(GL30.GL_NOTEQUAL, 1, 0xFF);
		GL30.glEnable(GL30.GL_DEPTH_TEST);
		//Clear stencil buffer:
		GL30.glClear(GL30.GL_STENCIL_BUFFER_BIT);
		//After clearing, disable usage/writing of/to stencil buffer again.
		GL30.glStencilMask(0x00);
	}
	
	private Part match;
	private double dist;
	
	private void cpuRaycast()
	{
		Vector3 cameraPosition = camera.getPosition();
		Vector3 cameraRay = Vector3.zp;
		cameraRay = Quaternion.angleAxis(camera.getNeck(), Vector3.xn).multiply(cameraRay);
		cameraRay = Quaternion.angleAxis(camera.getRotation(), Vector3.yn).multiply(cameraRay);
		
		match = null;
		dist = Double.MAX_VALUE;
		
		//Don't collide with wires, if one is about to be drawn.
		if(wireStartPoint == null)
		{
			RayCastResult result = wireRayCaster.castRay(cameraPosition, cameraRay);
			if(result != null && result.getDistance() < dist)
			{
				match = result.getMatch();
				dist = result.getDistance();
			}
		}
		
		focusProbe(board.getRootBoard(), cameraPosition, cameraRay);
		
		currentlySelected = match;
	}
	
	private void focusProbe(Component component, Vector3 camPos, Vector3 camRay)
	{
		if(component instanceof CompSnappingWire)
		{
			return;
		}
		
		if(!component.getBounds().contains(camPos))
		{
			double distance = distance(component.getBounds(), camPos, camRay);
			if(distance < 0 || distance >= dist)
			{
				return; //We already found something closer bye.
			}
		}
		
		//Normal or board:
		testComponent(component, camPos, camRay);
		if(component instanceof CompContainer)
		{
			//Test children:
			for(Component child : ((CompContainer) component).getChildren())
			{
				focusProbe(child, camPos, camRay);
			}
		}
	}
	
	private void testComponent(Component component, Vector3 camPos, Vector3 camRay)
	{
		Quaternion componentRotation = component.getRotation();
		Vector3 cameraPositionComponentSpace = componentRotation.multiply(camPos.subtract(component.getPosition())).subtract(component.getModelHolder().getPlacementOffset());
		Vector3 cameraRayComponentSpace = componentRotation.multiply(camRay);
		
		for(Connector connector : component.getConnectors())
		{
			CubeFull shape = connector.getModel();
			Vector3 size = shape.getSize();
			Vector3 cameraRayPegSpace = cameraRayComponentSpace;
			Vector3 cameraPositionPeg = cameraPositionComponentSpace;
			if(shape instanceof CubeOpenRotated)
			{
				Quaternion rotation = ((CubeOpenRotated) shape).getRotation().inverse();
				cameraRayPegSpace = rotation.multiply(cameraRayPegSpace);
				cameraPositionPeg = rotation.multiply(cameraPositionPeg);
			}
			cameraPositionPeg = cameraPositionPeg.subtract(connector.getModel().getPosition());
			
			double distance = distance(size, cameraPositionPeg, cameraRayPegSpace);
			if(distance < 0 || distance >= dist)
			{
				continue;
			}
			
			match = connector;
			dist = distance;
		}
		
		for(Meshable meshable : component.getModelHolder().getSolid())
		{
			CubeFull shape = (CubeFull) meshable;
			Vector3 cameraPositionSolidSpace = cameraPositionComponentSpace.subtract(shape.getPosition());
			Vector3 size = shape.getSize();
			if(shape.getMapper() != null)
			{
				size = shape.getMapper().getMappedSize(size, component);
			}
			
			double distance = distance(size, cameraPositionSolidSpace, cameraRayComponentSpace);
			if(distance < 0 || distance >= dist)
			{
				continue;
			}
			
			match = component;
			dist = distance;
		}
		
		for(Meshable meshable : component.getModelHolder().getColorables())
		{
			CubeFull shape = (CubeFull) meshable;
			Vector3 cameraPositionColorSpace = cameraPositionComponentSpace.subtract(shape.getPosition());
			Vector3 size = shape.getSize();
			
			double distance = distance(size, cameraPositionColorSpace, cameraRayComponentSpace);
			if(distance < 0 || distance >= dist)
			{
				continue;
			}
			
			match = component;
			dist = distance;
		}
	}
	
	private double distance(Vector3 size, Vector3 camPos, Vector3 camRay)
	{
		double xA = (size.getX() - camPos.getX()) / camRay.getX();
		double xB = ((-size.getX()) - camPos.getX()) / camRay.getX();
		double yA = (size.getY() - camPos.getY()) / camRay.getY();
		double yB = ((-size.getY()) - camPos.getY()) / camRay.getY();
		double zA = (size.getZ() - camPos.getZ()) / camRay.getZ();
		double zB = ((-size.getZ()) - camPos.getZ()) / camRay.getZ();
		
		double tMin;
		double tMax;
		{
			if(xA < xB)
			{
				tMin = xA;
				tMax = xB;
			}
			else
			{
				tMin = xB;
				tMax = xA;
			}
			
			double min = yA;
			double max = yB;
			if(min > max)
			{
				min = yB;
				max = yA;
			}
			
			if(min > tMin)
			{
				tMin = min;
			}
			if(max < tMax)
			{
				tMax = max;
			}
			
			min = zA;
			max = zB;
			if(min > max)
			{
				min = zB;
				max = zA;
			}
			
			if(min > tMin)
			{
				tMin = min;
			}
			if(max < tMax)
			{
				tMax = max;
			}
		}
		
		if(tMax < 0)
		{
			return -1; //Behind camera.
		}
		
		if(tMin > tMax)
		{
			return -1; //No collision.
		}
		
		return tMin;
	}
	
	private double distance(MinMaxBox aabb, Vector3 camPos, Vector3 camRay)
	{
		Vector3 minV = aabb.getMin();
		Vector3 maxV = aabb.getMax();
		
		double xA = (maxV.getX() - camPos.getX()) / camRay.getX();
		double xB = (minV.getX() - camPos.getX()) / camRay.getX();
		double yA = (maxV.getY() - camPos.getY()) / camRay.getY();
		double yB = (minV.getY() - camPos.getY()) / camRay.getY();
		double zA = (maxV.getZ() - camPos.getZ()) / camRay.getZ();
		double zB = (minV.getZ() - camPos.getZ()) / camRay.getZ();
		
		double tMin;
		double tMax;
		{
			if(xA < xB)
			{
				tMin = xA;
				tMax = xB;
			}
			else
			{
				tMin = xB;
				tMax = xA;
			}
			
			double min = yA;
			double max = yB;
			if(min > max)
			{
				min = yB;
				max = yA;
			}
			
			if(min > tMin)
			{
				tMin = min;
			}
			if(max < tMax)
			{
				tMax = max;
			}
			
			min = zA;
			max = zB;
			if(min > max)
			{
				min = zB;
				max = zA;
			}
			
			if(min > tMin)
			{
				tMin = min;
			}
			if(max < tMax)
			{
				tMax = max;
			}
		}
		
		if(tMax < 0)
		{
			return -1; //Behind camera.
		}
		
		if(tMin > tMax)
		{
			return -1; //No collision.
		}
		
		return tMin;
	}
	
	@Override
	public void newSize(int width, int height)
	{
	}
	
	public Camera getCamera()
	{
		return camera;
	}
	
	private static class PlacementData
	{
		private final Vector3 normal;
		private final Vector3 position;
		private final CompBoard parentBoard;
		private final Vector3 localNormal;
		
		public PlacementData(Vector3 position, Vector3 normal, CompBoard parentBoard, Vector3 localNormal)
		{
			this.normal = normal;
			this.position = position;
			this.parentBoard = parentBoard;
			this.localNormal = localNormal;
		}
		
		public Vector3 getNormal()
		{
			return normal;
		}
		
		public Vector3 getPosition()
		{
			return position;
		}
		
		public CompBoard getParentBoard()
		{
			return parentBoard;
		}
		
		public Vector3 getLocalNormal()
		{
			return localNormal;
		}
	}
}

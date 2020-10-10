package de.ecconia.java.opentung;

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
import de.ecconia.java.opentung.components.fragments.Meshable;
import de.ecconia.java.opentung.components.meta.Colorable;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.Holdable;
import de.ecconia.java.opentung.components.meta.Part;
import de.ecconia.java.opentung.inputs.Controller3D;
import de.ecconia.java.opentung.inputs.InputProcessor;
import de.ecconia.java.opentung.libwrap.Matrix;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.TextureWrapper;
import de.ecconia.java.opentung.libwrap.meshes.ColorMesh;
import de.ecconia.java.opentung.libwrap.meshes.ConductorMesh;
import de.ecconia.java.opentung.libwrap.meshes.SolidMesh;
import de.ecconia.java.opentung.libwrap.meshes.TextureMesh;
import de.ecconia.java.opentung.libwrap.vaos.InYaFaceVAO;
import de.ecconia.java.opentung.libwrap.vaos.LineVAO;
import de.ecconia.java.opentung.libwrap.vaos.SimpleCubeVAO;
import de.ecconia.java.opentung.libwrap.vaos.VisualShapeVAO;
import de.ecconia.java.opentung.math.MathHelper;
import de.ecconia.java.opentung.math.Quaternion;
import de.ecconia.java.opentung.math.Vector3;
import de.ecconia.java.opentung.settings.Settings;
import de.ecconia.java.opentung.simulation.Cluster;
import de.ecconia.java.opentung.simulation.ClusterHelper;
import de.ecconia.java.opentung.simulation.HiddenWire;
import de.ecconia.java.opentung.simulation.InheritingCluster;
import de.ecconia.java.opentung.simulation.SourceCluster;
import de.ecconia.java.opentung.simulation.Updateable;
import de.ecconia.java.opentung.simulation.Wire;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.lwjgl.opengl.GL30;

public class RenderPlane3D implements RenderPlane
{
	private Camera camera;
	private long lastCycle;
	
	private ShaderProgram lineShader;
	private LineVAO crossyIndicator;
	private LineVAO axisIndicator;
	private ShaderProgram justShape;
	private SimpleCubeVAO cubeVAO;
	private ShaderProgram visualShapeShader;
	private VisualShapeVAO visualShape;
	private ShaderProgram placeableBoardShader;
	private ShaderProgram inYaFace;
	private InYaFaceVAO inYaFaceVAO;
	private ShaderProgram sdfShader;
	
	private TextureWrapper boardTexture;
	
	private final InputProcessor inputHandler;
	
	private TextureMesh textureMesh;
	private SolidMesh solidMesh;
	private ConductorMesh conductorMesh;
	private ColorMesh colorMesh;
	
	private final List<Vector3> wireEndsToRender = new ArrayList<>();
	private final LabelToolkit labelToolkit = new LabelToolkit();
	private final BlockingQueue<GPUTask> gpuTasks = new LinkedBlockingQueue<>();
	private final SharedData sharedData;
	
	//TODO: Remove this thing again from here. But later when there is more management.
	private final BoardUniverse board;
	
	private Part currentlySelected; //What the camera is currently looking at.
	private Cluster clusterToHighlight;
	private List<Connector> connectorsToHighlight = new ArrayList<>();
	private int width = 0;
	private int height = 0;
	private float[] latestProjectionMat;
	
	public RenderPlane3D(InputProcessor inputHandler, BoardUniverse board, SharedData sharedData)
	{
		this.board = board;
		board.startFinalizeImport(gpuTasks);
		this.inputHandler = inputHandler;
		this.sharedData = sharedData;
		sharedData.setGPUTasks(gpuTasks);
		sharedData.setRenderPlane3D(this);
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
	
	//Board specific values:
	private boolean placeableBoardIslaying = true;
	private boolean boardIsBeingDragged = false; //Scope input/(render), read on many places.
	
	//Grabbing stuff:
	private Component grabbedComponent;
	private List<Wire> grabbedWires;
	private int grabRotation;
	
	//Input handling:
	
	private Controller3D controller;
	private Connector wireStartPoint; //Selected by dragging from a connector.
	private int placementRotation;
	
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
			placeableBoardIslaying = !placeableBoardIslaying;
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
			
			//Add wire:
			CompWireRaw newWire;
			{
				//TODO: Use both connectors to figure out the parent - for now not required but later on.
				newWire = new CompWireRaw(null);
				
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
				//Places the wires and updates clusters as needed. Also finishes the wire linking.
				ClusterHelper.placeWire(simulation, board, from, to, newWire);
				
				//Once it is fully prepared by simulation thread, cause the graphic thread to draw it.
				try
				{
					gpuTasks.put((ignored) -> {
						//Add the wire to the mesh sources
						board.getWiresToRender().add(newWire);
						
						refreshWireMeshes();
					});
				}
				catch(InterruptedException e)
				{
					e.printStackTrace();
				}
			});
		}
	}
	
	public void rotatePlacement(int degrees)
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
		
		PlaceableInfo currentPlaceable = sharedData.getCurrentPlaceable();
		if(isGrabbing())
		{
			Vector3 newPosition = placement.getPosition();
			Quaternion newRotation;
			{
				Quaternion originalGlobalRotation = grabbedComponent.getRotation();
				Vector3 upVectorGlobal = Vector3.yp;
				Vector3 upVectorLocal = originalGlobalRotation.multiply(upVectorGlobal);
				Quaternion upVectorLocalRotation = MathHelper.rotationFromVectors(Vector3.yp, upVectorLocal);
				Quaternion rotationLocal = upVectorLocalRotation.multiply(originalGlobalRotation);
				Quaternion modelRotation = Quaternion.angleAxis(grabRotation, Vector3.yn);
				Quaternion rotatedRotation = modelRotation.multiply(rotationLocal);
				Quaternion compRotation = MathHelper.rotationFromVectors(Vector3.yp, placement.getNormal());
				newRotation = rotatedRotation.multiply(compRotation);
			}
			
			grabbedComponent.setPosition(newPosition);
			grabbedComponent.setRotation(newRotation);
			
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
				
				CompWireRaw cwire = (CompWireRaw) wire;
				cwire.setPosition(position);
				cwire.setRotation(rotation);
				cwire.setLength((float) distance * 2f);
			}
			
			gpuTasks.add((unused) -> {
				board.getComponentsToRender().add(grabbedComponent);
				for(Wire wire : grabbedWires)
				{
					board.getWiresToRender().add((CompWireRaw) wire);
				}
				if(grabbedComponent instanceof CompLabel)
				{
					board.getLabelsToRender().add((CompLabel) grabbedComponent);
				}
				
				refreshComponentMeshes(grabbedComponent instanceof Colorable);
				
				grabbedComponent = null;
				grabbedWires = null;
			});
			
			return true;
		}
		//TODO: Ugly, not thread-safe enough for my taste. Might even cause bugs. So eventually it has to be changed.
		else if(currentPlaceable != null)
		{
			boolean isPlacingBoard = currentPlaceable == CompBoard.info;
			Quaternion rotation = Quaternion.angleAxis(placementRotation, Vector3.yn);
			Quaternion compRotation = MathHelper.rotationFromVectors(Vector3.yp, placement.getNormal());
			Quaternion finalRotation = rotation.multiply(compRotation);
			if(isPlacingBoard)
			{
				Quaternion boardAlignment = Quaternion.angleAxis(placeableBoardIslaying ? 0 : 90, Vector3.xn);
				finalRotation = boardAlignment.multiply(finalRotation);
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
				newComponent = new CompBoard(placement.getParentBoard(), x, z);
			}
			else
			{
				newComponent = currentPlaceable.instance(placement.getParentBoard());
			}
			newComponent.setRotation(finalRotation);
			newComponent.setPosition(position);
			
			//TODO: Update bounds and stuff
			
			if(currentPlaceable == CompBoard.info)
			{
				try
				{
					gpuTasks.put((ignored) -> {
						board.getBoardsToRender().add((CompBoard) newComponent);
						placement.getParentBoard().addChild(newComponent);
						refreshBoardMeshes();
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
				Cluster cluster = new InheritingCluster(board.getNewClusterID());
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
					Cluster cluster = new InheritingCluster(board.getNewClusterID());
					cluster.addConnector(peg);
					peg.setCluster(cluster);
				}
				for(Blot blot : newComponent.getBlots())
				{
					Cluster cluster = new SourceCluster(board.getNewClusterID(), blot);
					cluster.addConnector(blot);
					blot.setCluster(cluster);
				}
			}
			
			if(newComponent instanceof Colorable)
			{
				int colorablesCount = newComponent.getModelHolder().getColorables().size();
				for(int i = 0; i < colorablesCount; i++)
				{
					((Colorable) newComponent).setColorID(i, board.getColorableIDs().getNewID());
				}
			}
			
			if(newComponent instanceof Updateable)
			{
				board.getSimulation().updateNextTickThreadSafe((Updateable) newComponent);
			}
			
			try
			{
				gpuTasks.put((ignored) -> {
					board.getComponentsToRender().add(newComponent);
					placement.getParentBoard().addChild(newComponent);
					refreshComponentMeshes(newComponent instanceof Colorable);
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
		if(toBeDeleted instanceof Connector)
		{
			toBeDeleted = toBeDeleted.getParent();
		}
		
		if(toBeDeleted instanceof CompContainer)
		{
			CompContainer container = (CompContainer) toBeDeleted;
			if(container.isEmpty())
			{
				//Asume containers are not logic components.
				gpuTasks.add((unused) -> {
					if(container.getParent() != null)
					{
						((CompContainer) container.getParent()).remove(container);
					}
					
					if(container instanceof CompBoard)
					{
						board.getBoardsToRender().remove(container);
						refreshBoardMeshes();
					}
					else
					{
						board.getComponentsToRender().remove(container);
						refreshComponentMeshes(container instanceof Colorable);
					}
				});
			}
			else
			{
				System.out.println("Cannot delete containers with components yet.");
			}
		}
		else if(toBeDeleted instanceof CompWireRaw)
		{
			final CompWireRaw wireToDelete = (CompWireRaw) toBeDeleted;
			
			board.getSimulation().updateJobNextTickThreadSafe((simulation) -> {
				if(wireToDelete.getParent() != null)
				{
					((CompContainer) wireToDelete.getParent()).remove(wireToDelete);
				}
				
				ClusterHelper.removeWire(board, simulation, wireToDelete);
				
				gpuTasks.add((unused) -> {
					if(clusterToHighlight == wireToDelete.getCluster())
					{
						clusterToHighlight = null;
						connectorsToHighlight = new ArrayList<>();
					}
					board.getWiresToRender().remove(wireToDelete);
					refreshWireMeshes();
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
							ClusterHelper.removeWire(board, simulation, wire);
							sPeg.getPartner().setPartner(null);
							sPeg.setPartner(null);
							gpuTasks.add((unused) -> {
								board.getComponentsToRender().remove(wire);
							});
						});
						break;
					}
				}
			}
			else if(toBeDeleted instanceof Colorable)
			{
				Colorable colorable = (Colorable) toBeDeleted;
				gpuTasks.add((unused) -> {
					int colorablesCount = component.getModelHolder().getColorables().size();
					for(int i = 0; i < colorablesCount; i++)
					{
						board.getColorableIDs().freeID(colorable.getColorID(i));
					}
				});
			}
			
			board.getSimulation().updateJobNextTickThreadSafe((simulation) -> {
				if(component.getParent() != null)
				{
					((CompContainer) component.getParent()).remove(component);
				}
				
				List<Wire> wiresToRemove = new ArrayList<>();
				for(Blot blot : component.getBlots())
				{
					ClusterHelper.removeBlot(board, simulation, blot);
					wiresToRemove.addAll(blot.getWires());
				}
				for(Peg peg : component.getPegs())
				{
					ClusterHelper.removePeg(board, simulation, peg);
					wiresToRemove.addAll(peg.getWires());
				}
				
				gpuTasks.add((unused) -> {
					for(Blot blot : component.getBlots())
					{
						if(clusterToHighlight == blot.getCluster())
						{
							clusterToHighlight = null;
							connectorsToHighlight = new ArrayList<>();
						}
					}
					for(Peg peg : component.getPegs())
					{
						if(clusterToHighlight == peg.getCluster())
						{
							clusterToHighlight = null;
							connectorsToHighlight = new ArrayList<>();
						}
					}
					board.getComponentsToRender().remove(component);
					for(Wire wire : wiresToRemove)
					{
						board.getWiresToRender().remove(wire);
					}
					if(component instanceof CompLabel)
					{
						((CompLabel) component).unload();
						board.getLabelsToRender().remove(component);
					}
					refreshComponentMeshes(component instanceof Colorable);
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
		if(toBeGrabbed instanceof CompContainer)
		{
			System.out.println("Cannot grab container - yet.");
			return;
		}
		if(toBeGrabbed instanceof Wire)
		{
			//We don't grab wires.
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
						ClusterHelper.removeWire(board, simulation, wire);
						sPeg.getPartner().setPartner(null);
						sPeg.setPartner(null);
						gpuTasks.add((unused) -> {
							board.getComponentsToRender().remove(wire);
							//No mesh updates, since by order one will happen soon anyway.
						});
					});
					break;
				}
			}
		}
		else if(toBeGrabbed instanceof Colorable)
		{
			Colorable colorable = (Colorable) toBeGrabbed;
			gpuTasks.add((unused) -> {
				int colorablesCount = toBeGrabbed.getModelHolder().getColorables().size();
				for(int i = 0; i < colorablesCount; i++)
				{
					board.getColorableIDs().freeID(colorable.getColorID(i));
				}
			});
		}
		
		board.getSimulation().updateJobNextTickThreadSafe((unused) -> {
			//Collect wires:
			List<Wire> wires = new ArrayList<>();
			for(Peg peg : toBeGrabbed.getPegs())
			{
				for(Wire wire : peg.getWires())
				{
					if(wire instanceof CompWireRaw)
					{
						wires.add(wire);
					}
				}
			}
			for(Blot blot : toBeGrabbed.getBlots())
			{
				for(Wire wire : blot.getWires())
				{
					if(wire instanceof CompWireRaw)
					{
						wires.add(wire);
					}
				}
			}
			
			gpuTasks.add((unused2) -> {
				for(Blot blot : toBeGrabbed.getBlots())
				{
					if(clusterToHighlight == blot.getCluster())
					{
						clusterToHighlight = null;
						connectorsToHighlight = new ArrayList<>();
					}
				}
				for(Peg peg : toBeGrabbed.getPegs())
				{
					if(clusterToHighlight == peg.getCluster())
					{
						clusterToHighlight = null;
						connectorsToHighlight = new ArrayList<>();
					}
				}
				if(toBeGrabbed instanceof CompLabel)
				{
					board.getLabelsToRender().remove(toBeGrabbed);
				}
				//Remove from meshes on render thread
				board.getComponentsToRender().remove(toBeGrabbed);
				board.getWiresToRender().removeAll(wires);
				refreshComponentMeshes(toBeGrabbed instanceof Colorable);
				//Create construct to store the grabbed content (to be drawn).
				
				grabRotation = 0;
				grabbedComponent = toBeGrabbed;
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
			if(compCopy.getParent() != null)
			{
				((CompContainer) compCopy.getParent()).remove(compCopy);
			}
			for(Wire wire : wireCopy)
			{
				ClusterHelper.removeWire(board, board.getSimulation(), wire);
			}
			for(Peg peg : compCopy.getPegs())
			{
				ClusterHelper.removePeg(board, board.getSimulation(), peg);
			}
			for(Blot blot : compCopy.getBlots())
			{
				ClusterHelper.removeBlot(board, board.getSimulation(), blot);
			}
			
			gpuTasks.add((unused2) -> {
				grabbedWires = null;
				grabbedComponent = null;
				
				if(compCopy instanceof CompLabel)
				{
					((CompLabel) compCopy).unload();
				}
				System.out.println("[MeshDebug] Update:");
				conductorMesh.update(board.getComponentsToRender(), board.getWiresToRender());
				System.out.println("[MeshDebug] Done.");
			});
		});
	}
	
	public void abortGrabbing()
	{
		gpuTasks.add((unused) -> {
			board.getComponentsToRender().add(grabbedComponent);
			for(Wire wire : grabbedWires)
			{
				board.getWiresToRender().add((CompWireRaw) wire);
			}
			if(grabbedComponent instanceof CompLabel)
			{
				board.getLabelsToRender().add((CompLabel) grabbedComponent);
			}
			
			refreshComponentMeshes(grabbedComponent instanceof Colorable);
			
			grabbedComponent = null;
			grabbedWires = null;
		});
	}
	
	//Setup and stuff:
	
	@Override
	public void setup()
	{
		{
			int side = 16;
			BufferedImage image = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = image.createGraphics();
			g.setColor(java.awt.Color.white);
			g.fillRect(0, 0, side - 1, side - 1);
			g.setColor(new java.awt.Color(0x777777));
			g.drawRect(0, 0, side - 1, side - 1);
			g.dispose();
			boardTexture = TextureWrapper.createBoardTexture(image);
		}
		
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
		
		//Colorable IDs:
		{
			for(Component comp : board.getComponentsToRender())
			{
				if(!(comp instanceof Colorable))
				{
					continue;
				}
				
				int colorablesCount = comp.getModelHolder().getColorables().size();
				for(int i = 0; i < colorablesCount; i++)
				{
					CubeFull cube = (CubeFull) comp.getModelHolder().getColorables().get(i);
					
					int colorID = board.getColorableIDs().getNewID();
					((Colorable) comp).setColorID(i, colorID);
				}
			}
		}
		
		lineShader = new ShaderProgram("lineShader");
		crossyIndicator = LineVAO.generateCrossyIndicator();
		axisIndicator = LineVAO.generateAxisIndicator();
		justShape = new ShaderProgram("justShape");
		cubeVAO = SimpleCubeVAO.generateCube();
		visualShapeShader = new ShaderProgram("visualShape");
		visualShape = VisualShapeVAO.generateCube();
		placeableBoardShader = new ShaderProgram("placeableBoardShader");
		
		inYaFace = new ShaderProgram("outline/inYaFacePlane");
		inYaFaceVAO = InYaFaceVAO.generateInYaFacePlane();
		sdfShader = new ShaderProgram("sdfLabel");
		
		camera = new Camera();
		//Do not start receiving events before here. Be sure the whole thing is properly setted up.
		controller = new Controller3D(this);
		inputHandler.setController(controller);
		
		//Create meshes:
		{
			System.out.println("[MeshDebug] Starting mesh generation...");
			textureMesh = new TextureMesh(boardTexture, board.getBoardsToRender());
			solidMesh = new SolidMesh(board.getComponentsToRender());
			conductorMesh = new ConductorMesh(board.getComponentsToRender(), board.getWiresToRender(), board.getSimulation(), true);
			colorMesh = new ColorMesh(board.getComponentsToRender(), board.getSimulation());
			System.out.println("[MeshDebug] Done.");
		}
		
		gpuTasks.add(new GPUTask()
		{
			@Override
			public void execute(RenderPlane3D world3D)
			{
				IconGeneration.render(visualShapeShader, visualShape);
				
				//Restore the projection matrix of this shader, since it got abused.
				visualShapeShader.setUniform(0, latestProjectionMat);
				//Restore viewport:
				GL30.glViewport(0, 0, width, height);
			}
		});
		
		System.out.println("[Debug] Label amount: " + board.getLabelsToRender().size());
		System.out.println("[Debug] Wire amount: " + board.getWiresToRender().size());
		lastCycle = System.currentTimeMillis();
	}
	
	public void refreshPostWorldLoad()
	{
		System.out.println("[MeshDebug] Update:");
		conductorMesh.update(board.getComponentsToRender(), board.getWiresToRender());
		for(Cluster cluster : board.getClusters())
		{
			cluster.updateState(board.getSimulation());
		}
		board.getSimulation().start();
		fullyLoaded = true;
		sharedData.setSimulationLoaded(true);
		inputHandler.updatePauseMenu();
		System.out.println("[MeshDebug] Done.");
	}
	
	public void refreshComponentMeshes(boolean hasColorable)
	{
		System.out.println("[MeshDebug] Update:");
		conductorMesh.update(board.getComponentsToRender(), board.getWiresToRender());
		solidMesh.update(board.getComponentsToRender());
		if(hasColorable)
		{
			colorMesh.update(board.getComponentsToRender());
		}
		System.out.println("[MeshDebug] Done.");
	}
	
	public void refreshWireMeshes()
	{
		System.out.println("[MeshDebug] Update:");
		conductorMesh.update(board.getComponentsToRender(), board.getWiresToRender());
		System.out.println("[MeshDebug] Done.");
	}
	
	private void refreshBoardMeshes()
	{
		System.out.println("[MeshDebug] Update:");
		textureMesh.update(board.getBoardsToRender());
		System.out.println("[MeshDebug] Done.");
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
		
		if(sharedData.getCurrentPlaceable() == CompBoard.info)
		{
			//Boards have their center within, thus the offset needs to be adjusted:
			placementPosition = placementPosition.add(placementNormal.multiply(placeableBoardIslaying ? 0.15 : (0.15 + 0.075)));
		}
		
		placementData = new PlacementData(placementPosition, placementNormal, placementBoard);
	}
	
	@Override
	public void render()
	{
		while(!gpuTasks.isEmpty())
		{
			gpuTasks.poll().execute(this);
		}
		
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
			
			lineShader.use();
			lineShader.setUniform(1, view);
			Matrix model = new Matrix();
			if(Settings.drawComponentPositionIndicator)
			{
				for(Component comp : board.getComponentsToRender())
				{
					model.identity();
					model.translate((float) comp.getPosition().getX(), (float) comp.getPosition().getY(), (float) comp.getPosition().getZ());
					lineShader.setUniform(2, model.getMat());
					crossyIndicator.use();
					crossyIndicator.draw();
				}
			}
			if(Settings.drawWorldAxisIndicator)
			{
				model.identity();
				Vector3 position = new Vector3(0, 10, 0);
				model.translate((float) position.getX(), (float) position.getY(), (float) position.getZ());
				lineShader.setUniform(2, model.getMat());
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
			
			justShape.use();
			justShape.setUniform(1, view);
			justShape.setUniform(2, model.getMat());
			justShape.setUniformV4(3, new float[]{1.0f, 0.0f, 1.0f, 1.0f});
			
			cubeVAO.use();
			cubeVAO.draw();
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
			Matrix m = new Matrix();
			visualShapeShader.use();
			visualShapeShader.setUniform(1, view);
			visualShape.use();
			
			Quaternion originalGlobalRotation = grabbedComponent.getRotation();
			Vector3 originalGlobalPosition = grabbedComponent.getPosition();
			
			Vector3 globalPosition = placementData.getPosition();
			grabbedComponent.setPosition(globalPosition);
			
			Vector3 upVectorGlobal = Vector3.yp;
			Vector3 upVectorLocal = originalGlobalRotation.multiply(upVectorGlobal);
			Quaternion upVectorLocalRotation = MathHelper.rotationFromVectors(Vector3.yp, upVectorLocal);
			Quaternion rotationLocal = upVectorLocalRotation.multiply(originalGlobalRotation);
			Quaternion modelRotation = Quaternion.angleAxis(grabRotation, Vector3.yn);
			Quaternion rotatedRotation = modelRotation.multiply(rotationLocal);
			Quaternion compRotation = MathHelper.rotationFromVectors(Vector3.yp, placementData.getNormal());
			rotatedRotation = rotatedRotation.multiply(compRotation);
			grabbedComponent.setRotation(rotatedRotation);
			Matrix rotMat = new Matrix(rotatedRotation.createMatrix());
			
			for(Meshable meshable : grabbedComponent.getModelHolder().getSolid())
			{
				CubeFull c = (CubeFull) meshable;
				m.identity();
				m.translate((float) globalPosition.getX(), (float) globalPosition.getY(), (float) globalPosition.getZ());
				m.multiply(rotMat);
				Vector3 offPos = grabbedComponent.getModelHolder().getPlacementOffset();
				m.translate((float) offPos.getX(), (float) offPos.getY(), (float) offPos.getZ());
				Vector3 mPos = c.getPosition();
				m.translate((float) mPos.getX(), (float) mPos.getY(), (float) mPos.getZ());
				Vector3 size = c.getSize();
				m.scale((float) size.getX(), (float) size.getY(), (float) size.getZ());
				visualShapeShader.setUniform(2, m.getMat());
				visualShapeShader.setUniformV4(3, c.getColorArray());
				visualShape.draw();
			}
			
			for(Blot blot : grabbedComponent.getBlots())
			{
				CubeFull c = blot.getModel();
				m.identity();
				m.translate((float) globalPosition.getX(), (float) globalPosition.getY(), (float) globalPosition.getZ());
				m.multiply(rotMat);
				Vector3 offPos = grabbedComponent.getModelHolder().getPlacementOffset();
				m.translate((float) offPos.getX(), (float) offPos.getY(), (float) offPos.getZ());
				Vector3 mPos = c.getPosition();
				m.translate((float) mPos.getX(), (float) mPos.getY(), (float) mPos.getZ());
				Vector3 size = c.getSize();
				m.scale((float) size.getX(), (float) size.getY(), (float) size.getZ());
				visualShapeShader.setUniform(2, m.getMat());
				visualShapeShader.setUniformV4(3, (blot.getCluster().isActive() ? Color.circuitON : Color.circuitOFF).asArray());
				visualShape.draw();
			}
			
			for(Peg peg : grabbedComponent.getPegs())
			{
				CubeFull c = peg.getModel();
				m.identity();
				m.translate((float) globalPosition.getX(), (float) globalPosition.getY(), (float) globalPosition.getZ());
				m.multiply(rotMat);
				Vector3 offPos = grabbedComponent.getModelHolder().getPlacementOffset();
				m.translate((float) offPos.getX(), (float) offPos.getY(), (float) offPos.getZ());
				Vector3 mPos = c.getPosition();
				m.translate((float) mPos.getX(), (float) mPos.getY(), (float) mPos.getZ());
				Vector3 size = c.getSize();
				m.scale((float) size.getX(), (float) size.getY(), (float) size.getZ());
				visualShapeShader.setUniform(2, m.getMat());
				visualShapeShader.setUniformV4(3, (peg.getCluster().isActive() ? Color.circuitON : Color.circuitOFF).asArray());
				visualShape.draw();
			}
			
			for(Wire wire : grabbedWires)
			{
				Vector3 thisPos = wire.getConnectorA().getConnectionPoint();
				Vector3 thatPos = wire.getConnectorB().getConnectionPoint();
				
				Vector3 direction = thisPos.subtract(thatPos).divide(2);
				double distance = direction.length();
				Quaternion rotation = MathHelper.rotationFromVectors(Vector3.zp, direction.normalize());
				Vector3 position = thatPos.add(direction);
				
				m.identity();
				m.translate((float) position.getX(), (float) position.getY(), (float) position.getZ());
				m.multiply(new Matrix(rotation.createMatrix()));
				m.scale(0.025f, 0.01f, (float) distance);
				visualShapeShader.setUniformV4(3, (wire.getCluster().isActive() ? Color.circuitON : Color.circuitOFF).asArray());
				visualShapeShader.setUniform(2, m.getMat());
				visualShape.draw();
			}
			
			List<Meshable> colorables = grabbedComponent.getModelHolder().getColorables();
			for(int i = 0; i < colorables.size(); i++)
			{
				CubeFull c = (CubeFull) colorables.get(i);
				m.identity();
				m.translate((float) globalPosition.getX(), (float) globalPosition.getY(), (float) globalPosition.getZ());
				m.multiply(rotMat);
				Vector3 offPos = grabbedComponent.getModelHolder().getPlacementOffset();
				m.translate((float) offPos.getX(), (float) offPos.getY(), (float) offPos.getZ());
				Vector3 mPos = c.getPosition();
				m.translate((float) mPos.getX(), (float) mPos.getY(), (float) mPos.getZ());
				Vector3 size = c.getSize();
				m.scale((float) size.getX(), (float) size.getY(), (float) size.getZ());
				visualShapeShader.setUniform(2, m.getMat());
				visualShapeShader.setUniformV4(3, ((Colorable) grabbedComponent).getCurrentColor(i).asArray());
				visualShape.draw();
			}
			
			if(grabbedComponent instanceof CompLabel)
			{
				CompLabel label = (CompLabel) grabbedComponent;
				sdfShader.use();
				sdfShader.setUniform(1, view);
				label.activate();
				m.identity();
				m.translate((float) label.getPosition().getX(), (float) label.getPosition().getY(), (float) label.getPosition().getZ());
				m.multiply(new Matrix(label.getRotation().createMatrix()));
				sdfShader.setUniform(2, m.getMat());
				label.getModelHolder().drawTextures();
			}
			
			grabbedComponent.setPosition(originalGlobalPosition);
			grabbedComponent.setRotation(originalGlobalRotation);
			return;
		}
		
		PlaceableInfo currentPlaceable = sharedData.getCurrentPlaceable();
		if(currentPlaceable == null)
		{
			//TODO: Switch to line shader with uniform color.
			lineShader.use();
			lineShader.setUniform(1, view);
			GL30.glLineWidth(5f);
			Matrix model = new Matrix();
			model.identity();
			Vector3 datPos = placementData.getPosition().add(placementData.getNormal().multiply(0.075));
			model.translate((float) datPos.getX(), (float) datPos.getY(), (float) datPos.getZ());
			lineShader.setUniform(2, model.getMat());
			crossyIndicator.use();
			crossyIndicator.draw();
		}
		else if(currentPlaceable == CompBoard.info)
		{
			Quaternion compRotation = MathHelper.rotationFromVectors(Vector3.yp, placementData.getNormal());
			Quaternion modelRotation = Quaternion.angleAxis(placementRotation, Vector3.yn);
			Quaternion boardAlignment = Quaternion.angleAxis(placeableBoardIslaying ? 0 : 90, Vector3.xn);
			Quaternion finalRotation = boardAlignment.multiply(modelRotation).multiply(compRotation);
			
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
			boardTexture.activate();
			placeableBoardShader.use();
			placeableBoardShader.setUniform(1, view);
			placeableBoardShader.setUniform(2, matrix.getMat());
			placeableBoardShader.setUniformV2(3, new float[]{x, z});
			placeableBoardShader.setUniformV4(4, Color.boardDefault.asArray());
			visualShape.use();
			visualShape.draw();
		}
		else
		{
			Quaternion modelRotation = Quaternion.angleAxis(placementRotation, Vector3.yn);
			Quaternion compRotation = MathHelper.rotationFromVectors(Vector3.yp, placementData.getNormal());
			World3DHelper.drawModel(visualShapeShader, visualShape, currentPlaceable.getModel(), placementData.getPosition(), modelRotation.multiply(compRotation), view);
		}
	}
	
	private void drawDynamic(float[] view)
	{
		Matrix model = new Matrix();
		
		if(Settings.drawBoards)
		{
			textureMesh.draw(view);
		}
		conductorMesh.draw(view);
		if(Settings.drawMaterial)
		{
			solidMesh.draw(view);
		}
		colorMesh.draw(view);
		
		sdfShader.use();
		sdfShader.setUniform(1, view);
		for(CompLabel label : board.getLabelsToRender())
		{
			label.activate();
			model.identity();
			model.translate((float) label.getPosition().getX(), (float) label.getPosition().getY(), (float) label.getPosition().getZ());
			Matrix rotMat = new Matrix(label.getRotation().createMatrix());
			model.multiply(rotMat);
			sdfShader.setUniform(2, model.getMat());
			label.getModelHolder().drawTextures();
		}
		
		if(!wireEndsToRender.isEmpty())
		{
			lineShader.use();
			lineShader.setUniform(1, view);
			
			for(Vector3 position : wireEndsToRender)
			{
				model.identity();
				model.translate((float) position.getX(), (float) position.getY(), (float) position.getZ());
				lineShader.setUniform(2, model.getMat());
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
		
		if(part instanceof Component)
		{
			World3DHelper.drawStencilComponent(justShape, cubeVAO, (Component) part, view);
		}
		else //Connector
		{
			justShape.use();
			justShape.setUniform(1, view);
			justShape.setUniformV4(3, new float[]{0, 0, 0, 0});
			Matrix matrix = new Matrix();
			World3DHelper.drawCubeFull(justShape, cubeVAO, ((Connector) part).getModel(), part, part.getParent().getModelHolder().getPlacementOffset(), new Matrix());
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
		
		inYaFace.use();
		inYaFace.setUniformV4(0, color);
		inYaFaceVAO.use();
		inYaFaceVAO.draw();
		
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
		
		for(Wire wire : clusterToHighlight.getWires())
		{
			if(wire instanceof HiddenWire)
			{
				continue;
			}
			World3DHelper.drawStencilComponent(justShape, cubeVAO, (CompWireRaw) wire, view);
		}
		justShape.use();
		justShape.setUniform(1, view);
		justShape.setUniformV4(3, new float[]{0, 0, 0, 0});
		Matrix matrix = new Matrix();
		for(Connector connector : connectorsToHighlight)
		{
			World3DHelper.drawCubeFull(justShape, cubeVAO, connector.getModel(), connector.getParent(), connector.getParent().getModelHolder().getPlacementOffset(), matrix);
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
		
		inYaFace.use();
		inYaFace.setUniformV4(0, color);
		inYaFaceVAO.use();
		inYaFaceVAO.draw();
		
		//Restore settings:
		GL30.glStencilFunc(GL30.GL_NOTEQUAL, 1, 0xFF);
		GL30.glEnable(GL30.GL_DEPTH_TEST);
		//Clear stencil buffer:
		GL30.glClear(GL30.GL_STENCIL_BUFFER_BIT);
		//After clearing, disable usage/writing of/to stencil buffer again.
		GL30.glStencilMask(0x00);
	}
	
	private void cpuRaycast()
	{
		Vector3 cameraPosition = camera.getPosition();
		Vector3 cameraRay = Vector3.zp;
		cameraRay = Quaternion.angleAxis(camera.getNeck(), Vector3.xn).multiply(cameraRay);
		cameraRay = Quaternion.angleAxis(camera.getRotation(), Vector3.yn).multiply(cameraRay);
		
		Part match = null;
		double dist = Double.MAX_VALUE;
		for(CompBoard board : board.getBoardsToRender())
		{
			Quaternion boardRotation = board.getRotation();
			Vector3 cameraPositionBoardSpace = boardRotation.multiply(cameraPosition.subtract(board.getPosition()));
			Vector3 cameraRayBoardSpace = boardRotation.multiply(cameraRay);
			CubeFull shape = (CubeFull) board.getModelHolder().getSolid().get(0);
			Vector3 size = shape.getSize();
			if(shape.getMapper() != null)
			{
				size = shape.getMapper().getMappedSize(size, board);
			}
			
			double xr = 1.0 / cameraRayBoardSpace.getX();
			double yr = 1.0 / cameraRayBoardSpace.getY();
			double zr = 1.0 / cameraRayBoardSpace.getZ();
			
			double xA = (size.getX() - cameraPositionBoardSpace.getX()) * xr;
			double xB = ((-size.getX()) - cameraPositionBoardSpace.getX()) * xr;
			double yA = (size.getY() - cameraPositionBoardSpace.getY()) * yr;
			double yB = ((-size.getY()) - cameraPositionBoardSpace.getY()) * yr;
			double zA = (size.getZ() - cameraPositionBoardSpace.getZ()) * zr;
			double zB = ((-size.getZ()) - cameraPositionBoardSpace.getZ()) * zr;
			
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
				continue; //Behind camera.
			}
			
			if(tMin > tMax)
			{
				continue; //No collision.
			}
			
			if(tMin < dist)
			{
				match = board;
				dist = tMin;
			}
		}
		
		for(CompWireRaw wire : board.getWiresToRender())
		{
			Quaternion boardRotation = wire.getRotation();
			Vector3 cameraPositionBoardSpace = boardRotation.multiply(cameraPosition.subtract(wire.getPosition()));
			Vector3 cameraRayBoardSpace = boardRotation.multiply(cameraRay);
			CubeFull shape = (CubeFull) wire.getModelHolder().getConductors().get(0);
			Vector3 size = shape.getSize();
			if(shape.getMapper() != null)
			{
				size = shape.getMapper().getMappedSize(size, wire);
			}
			
			double xr = 1.0 / cameraRayBoardSpace.getX();
			double yr = 1.0 / cameraRayBoardSpace.getY();
			double zr = 1.0 / cameraRayBoardSpace.getZ();
			
			double xA = (size.getX() - cameraPositionBoardSpace.getX()) * xr;
			double xB = ((-size.getX()) - cameraPositionBoardSpace.getX()) * xr;
			double yA = (size.getY() - cameraPositionBoardSpace.getY()) * yr;
			double yB = ((-size.getY()) - cameraPositionBoardSpace.getY()) * yr;
			double zA = (size.getZ() - cameraPositionBoardSpace.getZ()) * zr;
			double zB = ((-size.getZ()) - cameraPositionBoardSpace.getZ()) * zr;
			
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
				continue; //Behind camera.
			}
			
			if(tMin > tMax)
			{
				continue; //No collision.
			}
			
			if(tMin < dist)
			{
				match = wire;
				dist = tMin;
			}
		}
		
		for(Component component : board.getComponentsToRender())
		{
			if(component instanceof CompSnappingWire)
			{
				continue;
			}
			
			Quaternion boardRotation = component.getRotation();
			Vector3 cameraPositionBoardSpace = boardRotation.multiply(cameraPosition.subtract(component.getPosition())).subtract(component.getModelHolder().getPlacementOffset());
			Vector3 cameraRayBoardSpace = boardRotation.multiply(cameraRay);
			
			double xr = 1.0 / cameraRayBoardSpace.getX();
			double yr = 1.0 / cameraRayBoardSpace.getY();
			double zr = 1.0 / cameraRayBoardSpace.getZ();
			
			for(Peg peg : component.getPegs())
			{
				Vector3 cameraPositionBoard = cameraPositionBoardSpace.subtract(peg.getModel().getPosition());
				CubeFull shape = peg.getModel();
				Vector3 size = shape.getSize();
				
				double xA = (size.getX() - cameraPositionBoard.getX()) * xr;
				double xB = ((-size.getX()) - cameraPositionBoard.getX()) * xr;
				double yA = (size.getY() - cameraPositionBoard.getY()) * yr;
				double yB = ((-size.getY()) - cameraPositionBoard.getY()) * yr;
				double zA = (size.getZ() - cameraPositionBoard.getZ()) * zr;
				double zB = ((-size.getZ()) - cameraPositionBoard.getZ()) * zr;
				
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
					continue; //Behind camera.
				}
				
				if(tMin > tMax)
				{
					continue; //No collision.
				}
				
				if(tMin < dist)
				{
					match = peg;
					dist = tMin;
				}
			}
			
			for(Blot blot : component.getBlots())
			{
				Vector3 cameraPositionBoard = cameraPositionBoardSpace.subtract(blot.getModel().getPosition());
				CubeFull shape = blot.getModel();
				Vector3 size = shape.getSize();
				
				double xA = (size.getX() - cameraPositionBoard.getX()) * xr;
				double xB = ((-size.getX()) - cameraPositionBoard.getX()) * xr;
				double yA = (size.getY() - cameraPositionBoard.getY()) * yr;
				double yB = ((-size.getY()) - cameraPositionBoard.getY()) * yr;
				double zA = (size.getZ() - cameraPositionBoard.getZ()) * zr;
				double zB = ((-size.getZ()) - cameraPositionBoard.getZ()) * zr;
				
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
					continue; //Behind camera.
				}
				
				if(tMin > tMax)
				{
					continue; //No collision.
				}
				
				if(tMin < dist)
				{
					match = blot;
					dist = tMin;
				}
			}
			
			for(Meshable meshable : component.getModelHolder().getSolid())
			{
				CubeFull shape = (CubeFull) meshable;
				Vector3 cameraPositionBoard = cameraPositionBoardSpace.subtract(shape.getPosition());
				Vector3 size = shape.getSize();
				
				double xA = (size.getX() - cameraPositionBoard.getX()) * xr;
				double xB = ((-size.getX()) - cameraPositionBoard.getX()) * xr;
				double yA = (size.getY() - cameraPositionBoard.getY()) * yr;
				double yB = ((-size.getY()) - cameraPositionBoard.getY()) * yr;
				double zA = (size.getZ() - cameraPositionBoard.getZ()) * zr;
				double zB = ((-size.getZ()) - cameraPositionBoard.getZ()) * zr;
				
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
					continue; //Behind camera.
				}
				
				if(tMin > tMax)
				{
					continue; //No collision.
				}
				
				if(tMin < dist)
				{
					match = component;
					dist = tMin;
				}
			}
			
			for(Meshable meshable : component.getModelHolder().getColorables())
			{
				CubeFull shape = (CubeFull) meshable;
				Vector3 cameraPositionBoard = cameraPositionBoardSpace.subtract(shape.getPosition());
				Vector3 size = shape.getSize();
				
				double xA = (size.getX() - cameraPositionBoard.getX()) * xr;
				double xB = ((-size.getX()) - cameraPositionBoard.getX()) * xr;
				double yA = (size.getY() - cameraPositionBoard.getY()) * yr;
				double yB = ((-size.getY()) - cameraPositionBoard.getY()) * yr;
				double zA = (size.getZ() - cameraPositionBoard.getZ()) * zr;
				double zB = ((-size.getZ()) - cameraPositionBoard.getZ()) * zr;
				
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
					continue; //Behind camera.
				}
				
				if(tMin > tMax)
				{
					continue; //No collision.
				}
				
				if(tMin < dist)
				{
					match = component;
					dist = tMin;
				}
			}
		}
		
		currentlySelected = match;
	}
	
	@Override
	public void newSize(int width, int height)
	{
		this.width = width;
		this.height = height;
		Matrix p = new Matrix();
		p.perspective(Settings.fov, (float) width / (float) height, 0.1f, 100000f);
		float[] projection = p.getMat();
		latestProjectionMat = projection;
		
		solidMesh.updateProjection(projection);
		conductorMesh.updateProjection(projection);
		colorMesh.updateProjection(projection);
		textureMesh.updateProjection(projection);
		
		placeableBoardShader.use();
		placeableBoardShader.setUniform(0, projection);
		visualShapeShader.use();
		visualShapeShader.setUniform(0, projection);
		sdfShader.use();
		sdfShader.setUniform(0, projection);
		lineShader.use();
		lineShader.setUniform(0, projection);
		justShape.use();
		justShape.setUniform(0, projection);
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
		
		public PlacementData(Vector3 position, Vector3 normal, CompBoard parentBoard)
		{
			this.normal = normal;
			this.position = position;
			this.parentBoard = parentBoard;
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
	}
}

package de.ecconia.java.opentung;

import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.CompDisplay;
import de.ecconia.java.opentung.components.CompLabel;
import de.ecconia.java.opentung.components.CompPanelDisplay;
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
import de.ecconia.java.opentung.libwrap.meshes.MeshTypeThing;
import de.ecconia.java.opentung.libwrap.meshes.RayCastMesh;
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
	private RayCastMesh rayCastMesh;
	private SolidMesh solidMesh;
	private ConductorMesh conductorMesh;
	private ColorMesh colorMesh;
	
	private final List<Vector3> wireEndsToRender = new ArrayList<>();
	private final LabelToolkit labelToolkit = new LabelToolkit();
	private final BlockingQueue<GPUTask> gpuTasks = new LinkedBlockingQueue<>();
	private final SharedData sharedData;
	
	//TODO: Remove this thing again from here. But later when there is more management.
	private final BoardUniverse board;
	
	private Part[] idLookup;
	private int currentlySelectedIndex = 0; //What the camera is currently looking at.
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
	}
	
	//Other:
	
	private Vector3 placementPosition;
	private Vector3 placementNormal;
	private CompBoard placementBoard;
	private boolean fullyLoaded;
	
	//Board specific values:
	private boolean placeableBoardIslaying = true;
	private boolean boardIsBeingDragged = false;
	
	//Input handling:
	
	private Controller3D controller;
	private Connector wireStartPoint; //Selected by dragging from a connector.
	private int placementRotation;
	
	public Part getCursorObject()
	{
		if(currentlySelectedIndex <= 0)
		{
			return null;
		}
		return idLookup[currentlySelectedIndex];
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
			if(clusterToHighlight == cluster)
			{
				clusterToHighlight = null;
				connectorsToHighlight = new ArrayList<>();
			}
			else
			{
				clusterToHighlight = cluster;
				connectorsToHighlight = cluster.getConnectors();
			}
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
				
				//RayCast
				Part[] idLookupClone = new Part[idLookup.length + 1];
				System.arraycopy(idLookup, 0, idLookupClone, 0, idLookup.length);
				idLookup = idLookupClone;
				if(board.getRaycastIDs().getFreeIDs() < 0)
				{
					expandLookupArray();
				}
				int rayID = board.getRaycastIDs().getNewID();
				if(rayID >= idLookup.length)
				{
					expandLookupArray();
				}
				newWire.setRayCastID(rayID);
				idLookup[rayID] = newWire;
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
		placementRotation += degrees;
		if(placementRotation >= 360)
		{
			placementRotation -= 360;
		}
	}
	
	public void placementStart()
	{
		if(placementPosition != null && sharedData.getCurrentPlaceable() == CompBoard.info)
		{
			//Start dragging until end.
			boardIsBeingDragged = true;
		}
	}
	
	public boolean attemptPlacement()
	{
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
		
		PlaceableInfo currentPlaceable = sharedData.getCurrentPlaceable();
		//TODO: Ugly, not thread-safe enough for my taste. Might even cause bugs. So eventually it has to be changed.
		if(placementPosition != null && currentPlaceable != null)
		{
			boolean isPlacingBoard = currentPlaceable == CompBoard.info;
			Quaternion rotation = Quaternion.angleAxis(placementRotation, Vector3.yn);
			Quaternion compRotation = MathHelper.rotationFromVectors(Vector3.yp, placementNormal);
			Quaternion finalRotation = rotation.multiply(compRotation);
			if(isPlacingBoard)
			{
				Quaternion boardAlignment = Quaternion.angleAxis(placeableBoardIslaying ? 0 : 90, Vector3.xn);
				finalRotation = boardAlignment.multiply(finalRotation);
			}
			Vector3 position = placementPosition;
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
				newComponent = new CompBoard(placementBoard, x, z);
			}
			else
			{
				newComponent = currentPlaceable.instance(placementBoard);
			}
			//Raycast ID:
			{
				int neededIDAmount = 1 + newComponent.getPegs().size() + newComponent.getBlots().size();
				if(board.getRaycastIDs().getFreeIDs() < neededIDAmount)
				{
					expandLookupArray();
				}
				int rayID = board.getRaycastIDs().getNewID();
				if((rayID + neededIDAmount - 1) < idLookup.length)
				{
					expandLookupArray();
				}
				//TODO: Breaks as soon as a component has over 999 conductors....
				newComponent.setRayCastID(rayID);
				idLookup[rayID] = newComponent;
				for(Peg peg : newComponent.getPegs())
				{
					rayID = board.getRaycastIDs().getNewID();
					peg.setRayCastID(rayID);
					idLookup[rayID] = peg;
				}
				for(Blot blot : newComponent.getBlots())
				{
					rayID = board.getRaycastIDs().getNewID();
					blot.setRayCastID(rayID);
					idLookup[rayID] = blot;
				}
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
						placementBoard.addChild(newComponent);
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
					placementBoard.addChild(newComponent);
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
	
	private void expandLookupArray()
	{
		int newSize = idLookup.length + 1000;
		Part[] idLookupClone = new Part[newSize];
		System.arraycopy(idLookup, 0, idLookupClone, 0, idLookup.length);
		idLookup = idLookupClone;
	}
	
	public void delete(Part toBeDeleted)
	{
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
					board.getRaycastIDs().freeID(container.getRayID());
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
			if(clusterToHighlight == wireToDelete.getCluster())
			{
				clusterToHighlight = null;
				connectorsToHighlight = new ArrayList<>();
			}
			
			board.getSimulation().updateJobNextTickThreadSafe((simulation) -> {
				if(wireToDelete.getParent() != null)
				{
					((CompContainer) wireToDelete.getParent()).remove(wireToDelete);
				}
				
				ClusterHelper.removeWire(board, simulation, wireToDelete);
				
				gpuTasks.add((unused) -> {
					board.getWiresToRender().remove(wireToDelete);
					refreshWireMeshes();
					board.getRaycastIDs().freeID(wireToDelete.getRayID());
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
				int colorablesCount = component.getModelHolder().getColorables().size();
				for(int i = 0; i < colorablesCount; i++)
				{
					board.getColorableIDs().freeID(colorable.getColorID(i));
				}
			}
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
			
			board.getSimulation().updateJobNextTickThreadSafe((simulation) -> {
				if(component.getParent() != null)
				{
					((CompContainer) component.getParent()).remove(component);
				}
				
				List<Wire> wiresToRemove = new ArrayList<>();
				List<Integer> rayIDsToRemove = new ArrayList<>();
				for(Blot blot : component.getBlots())
				{
					ClusterHelper.removeBlot(board, simulation, blot);
					wiresToRemove.addAll(blot.getWires());
					rayIDsToRemove.add(blot.getRayID());
				}
				for(Peg peg : component.getPegs())
				{
					ClusterHelper.removePeg(board, simulation, peg);
					wiresToRemove.addAll(peg.getWires());
					rayIDsToRemove.add(peg.getRayID());
				}
				rayIDsToRemove.add(component.getRayID());
				
				gpuTasks.add((unused) -> {
					board.getComponentsToRender().remove(component);
					for(Wire wire : wiresToRemove)
					{
						board.getWiresToRender().remove(wire);
					}
					for(Integer i : rayIDsToRemove)
					{
						board.getRaycastIDs().freeID(i);
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
		
		//Raycast IDs:
		{
			//Calculate the initial amount, cause the array has to be initialized:
			int amount = board.getBoardsToRender().size() + board.getWiresToRender().size() + 1;
			for(Component component : board.getComponentsToRender())
			{
				amount += 1 + component.getPegs().size() + component.getBlots().size();
			}
			System.out.println("Raycast ID amount: " + amount);
			if((amount) > 0xFFFFFF)
			{
				throw new RuntimeException("Out of raycast IDs. Tell the dev to do fancy programming, so that this never happens again.");
			}
			idLookup = new Part[amount];
			
			for(Component comp : board.getBoardsToRender())
			{
				int rayID = board.getRaycastIDs().getNewID();
				comp.setRayCastID(rayID);
				idLookup[rayID] = comp;
			}
			for(Component comp : board.getWiresToRender())
			{
				int rayID = board.getRaycastIDs().getNewID();
				comp.setRayCastID(rayID);
				idLookup[rayID] = comp;
			}
			for(Component comp : board.getComponentsToRender())
			{
				int rayID = board.getRaycastIDs().getNewID();
				comp.setRayCastID(rayID);
				idLookup[rayID] = comp;
				for(Peg peg : comp.getPegs())
				{
					rayID = board.getRaycastIDs().getNewID();
					peg.setRayCastID(rayID);
					idLookup[rayID] = peg;
				}
				for(Blot blot : comp.getBlots())
				{
					rayID = board.getRaycastIDs().getNewID();
					blot.setRayCastID(rayID);
					idLookup[rayID] = blot;
				}
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
			rayCastMesh = new RayCastMesh(board.getBoardsToRender(), board.getWiresToRender(), board.getComponentsToRender());
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
		
		System.out.println("Label amount: " + board.getLabelsToRender().size());
		System.out.println("Wire amount: " + board.getWiresToRender().size());
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
		System.out.println("[MeshDebug] Done.");
	}
	
	public void refreshComponentMeshes(boolean hasColorable)
	{
		System.out.println("[MeshDebug] Update:");
		conductorMesh.update(board.getComponentsToRender(), board.getWiresToRender());
		solidMesh.update(board.getComponentsToRender());
		rayCastMesh.update(board.getBoardsToRender(), board.getWiresToRender(), board.getComponentsToRender());
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
		rayCastMesh.update(board.getBoardsToRender(), board.getWiresToRender(), board.getComponentsToRender());
		System.out.println("[MeshDebug] Done.");
	}
	
	private void refreshBoardMeshes()
	{
		System.out.println("[MeshDebug] Update:");
		textureMesh.update(board.getBoardsToRender());
		rayCastMesh.update(board.getBoardsToRender(), board.getWiresToRender(), board.getComponentsToRender());
		System.out.println("[MeshDebug] Done.");
	}
	
	public void calculatePlacementPosition()
	{
		if(boardIsBeingDragged)
		{
			return; //Don't change anything, the camera may look somewhere else in the meantime.
		}
		
		if(currentlySelectedIndex == 0)
		{
			placementPosition = null;
			return;
		}
		
		//TODO: Also allow the tip of Mounts :)
		
		//If looking at a board
		Part part = idLookup[currentlySelectedIndex];
		if(!(part instanceof CompBoard))
		{
			placementPosition = null; //Only place on boards.
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
		
		placementPosition = board.getRotation().inverse().multiply(collisionPointBoardSpace).add(board.getPosition());
		placementNormal = board.getRotation().inverse().multiply(normalGlobal).normalize(); //Safety normalization.
		placementBoard = board;
		
		if(sharedData.getCurrentPlaceable() == CompBoard.info)
		{
			//Boards have their center within, thus the offset needs to be adjusted:
			placementPosition = placementPosition.add(placementNormal.multiply(placeableBoardIslaying ? 0.15 : (0.15 + 0.075)));
		}
	}
	
	@Override
	public void render()
	{
		calculatePlacementPosition();
		
		while(!gpuTasks.isEmpty())
		{
			gpuTasks.poll().execute(this);
		}
		
		camera.lockLocation();
		controller.doFrameCycle();
		
		float[] view = camera.getMatrix();
		if(Settings.doRaycasting)
		{
			raycast(view);
		}
		if(Settings.drawWorld)
		{
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
		
		Vector3 toPos = placementPosition;
		if(toPos == null)
		{
			Part currentlyLookingAt = getCursorObject();
			if(currentlyLookingAt instanceof Connector)
			{
				toPos = ((Connector) currentlyLookingAt).getConnectionPoint();
			}
		}
		else
		{
			//Fix offset.
			toPos = toPos.add(placementNormal.multiply(0.075));
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
		if(placementPosition == null)
		{
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
			Vector3 datPos = placementPosition.add(placementNormal.multiply(0.075));
			model.translate((float) datPos.getX(), (float) datPos.getY(), (float) datPos.getZ());
			lineShader.setUniform(2, model.getMat());
			crossyIndicator.use();
			crossyIndicator.draw();
		}
		else if(currentPlaceable == CompBoard.info)
		{
			Quaternion compRotation = MathHelper.rotationFromVectors(Vector3.yp, placementNormal);
			Quaternion modelRotation = Quaternion.angleAxis(placementRotation, Vector3.yn);
			Quaternion boardAlignment = Quaternion.angleAxis(placeableBoardIslaying ? 0 : 90, Vector3.xn);
			Quaternion finalRotation = boardAlignment.multiply(modelRotation).multiply(compRotation);
			
			int x = 1;
			int z = 1;
			Vector3 position = placementPosition;
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
			Quaternion compRotation = MathHelper.rotationFromVectors(Vector3.yp, placementNormal);
			World3DHelper.drawModel(visualShapeShader, visualShape, currentPlaceable.getModel(), placementPosition, modelRotation.multiply(compRotation), view);
		}
	}
	
	private void drawDynamic(float[] view)
	{
		OpenTUNG.setBackgroundColor();
		OpenTUNG.clear();
		
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
		if(currentlySelectedIndex == 0)
		{
			return;
		}
		
		Part part = idLookup[currentlySelectedIndex];
		
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
	
	private void raycast(float[] view)
	{
		Matrix model = new Matrix();
		
		if(Settings.drawWorld)
		{
			GL30.glViewport(0, 0, 1, 1);
		}
		GL30.glClearColor(0, 0, 0, 1);
		OpenTUNG.clear();
		
		rayCastMesh.draw(view);
		
		GL30.glFlush();
		GL30.glFinish();
		
		float[] values = new float[3];
		GL30.glReadPixels(0, 0, 1, 1, GL30.GL_RGB, GL30.GL_FLOAT, values);
//		float[] distance = new float[1];
//		GL30.glReadPixels(width / 2, height / 2, 1, 1, GL30.GL_DEPTH_COMPONENT, GL30.GL_FLOAT, distance);
		
		int id = (int) (values[0] * 255f) + (int) (values[1] * 255f) * 256 + (int) (values[2] * 255f) * 256 * 256;
		if(id > idLookup.length - 1)
		{
			System.out.println("Looking at ???? (" + id + ")");
			id = 0;
		}
		
		if(Settings.drawWorld)
		{
			GL30.glViewport(0, 0, this.width, this.height);
		}
		
		currentlySelectedIndex = id;
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
		
		rayCastMesh.updateProjection(projection);
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
}

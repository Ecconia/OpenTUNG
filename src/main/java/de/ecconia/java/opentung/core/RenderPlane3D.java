package de.ecconia.java.opentung.core;

import de.ecconia.java.opentung.OpenTUNG;
import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.CompLabel;
import de.ecconia.java.opentung.components.CompMount;
import de.ecconia.java.opentung.components.CompPanelLabel;
import de.ecconia.java.opentung.components.CompSnappingPeg;
import de.ecconia.java.opentung.components.CompSnappingWire;
import de.ecconia.java.opentung.components.conductor.Blot;
import de.ecconia.java.opentung.components.conductor.CompWireRaw;
import de.ecconia.java.opentung.components.conductor.Connector;
import de.ecconia.java.opentung.components.conductor.Peg;
import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.Meshable;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ConnectedComponent;
import de.ecconia.java.opentung.components.meta.Holdable;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.components.meta.Part;
import de.ecconia.java.opentung.components.meta.PlaceableInfo;
import de.ecconia.java.opentung.core.data.GrabContainerData;
import de.ecconia.java.opentung.core.data.GrabData;
import de.ecconia.java.opentung.core.data.Hitpoint;
import de.ecconia.java.opentung.core.data.HitpointBoard;
import de.ecconia.java.opentung.core.data.HitpointContainer;
import de.ecconia.java.opentung.core.data.ResizeData;
import de.ecconia.java.opentung.core.data.ShaderStorage;
import de.ecconia.java.opentung.core.data.SharedData;
import de.ecconia.java.opentung.core.helper.OnBoardPlacementHelper;
import de.ecconia.java.opentung.core.helper.World3DHelper;
import de.ecconia.java.opentung.core.structs.GPUTask;
import de.ecconia.java.opentung.core.structs.RenderPlane;
import de.ecconia.java.opentung.core.systems.CPURaycast;
import de.ecconia.java.opentung.core.systems.ClusterHighlighter;
import de.ecconia.java.opentung.core.systems.Skybox;
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
import de.ecconia.java.opentung.simulation.ClusterHelper;
import de.ecconia.java.opentung.simulation.HiddenWire;
import de.ecconia.java.opentung.simulation.InitClusterHelper;
import de.ecconia.java.opentung.simulation.Powerable;
import de.ecconia.java.opentung.simulation.Updateable;
import de.ecconia.java.opentung.simulation.Wire;
import de.ecconia.java.opentung.units.IconGeneration;
import de.ecconia.java.opentung.units.LabelToolkit;
import de.ecconia.java.opentung.util.Ansi;
import de.ecconia.java.opentung.util.FourDirections;
import de.ecconia.java.opentung.util.Tuple;
import de.ecconia.java.opentung.util.math.MathHelper;
import de.ecconia.java.opentung.util.math.Quaternion;
import de.ecconia.java.opentung.util.math.Vector3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
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
	private final MeshBagContainer secondaryMesh; //Used for grabbing
	
	private final List<Vector3> wireEndsToRender = new ArrayList<>();
	private final LabelToolkit labelToolkit = new LabelToolkit();
	private final BlockingQueue<GPUTask> gpuTasks = new LinkedBlockingQueue<>();
	private final SharedData sharedData;
	private final ShaderStorage shaderStorage;
	private final Skybox skybox;
	
	private final WireRayCaster wireRayCaster;
	private final ClusterHighlighter clusterHighlighter;
	private final CPURaycast cpuRaycast;
	
	//TODO: Remove this thing again from here. But later when there is more management.
	private final BoardUniverse board;
	
	public RenderPlane3D(InputProcessor inputHandler, BoardUniverse board, SharedData sharedData)
	{
		this.board = board;
		this.wireRayCaster = new WireRayCaster();
		board.startFinalizeImport(gpuTasks, wireRayCaster);
		this.inputHandler = inputHandler;
		this.sharedData = sharedData;
		this.shaderStorage = sharedData.getShaderStorage();
		this.skybox = new Skybox(shaderStorage);
		sharedData.setGPUTasks(gpuTasks);
		sharedData.setRenderPlane3D(this);
		this.worldMesh = new MeshBagContainer(shaderStorage);
		this.secondaryMesh = new MeshBagContainer(shaderStorage);
		this.clusterHighlighter = new ClusterHighlighter(sharedData);
		this.cpuRaycast = new CPURaycast(); //Has internal fields, thus make it an instance.
	}
	
	public void prepareSaving(AtomicInteger pauseArrived)
	{
		//TBI: May skip the execution of some simulation tasks with external source, problem?
		board.getSimulation().pauseSimulation(pauseArrived);
		gpuTasks.add((unused) -> {
			if(isGrabbing())
			{
				//Grab aborting does not need the simulation thread, so it won't create new tasks there.
				abortGrabbing();
			}
		});
		//Following task is appended to the end of the task-queue and will allow saving.
		//TBI: Assumes that the interface is open and thus no new GPU tasks had been added.
		gpuTasks.add((unused) -> {
			hitpoint = new Hitpoint();
			boardDrawStartingPoint = null;
			wireStartPoint = null;
			pauseArrived.incrementAndGet();
		});
	}
	
	public void postSave()
	{
		board.getSimulation().resumeSimulation();
	}
	
	//Other:
	
	private Hitpoint hitpoint = new Hitpoint(); //What the camera is currently looking at.
	private PlaceableInfo currentPlaceable = null; //Backup variable, to keep the value constant while render-cycle.
	
	//STATES (And their data):
	//Is not ready yet:
	private boolean fullyLoaded; //Unset, if loading not yet done. Prevents initial interactions.
	//Is drawing a wire:
	private Connector wireStartPoint; //Selected by dragging from a connector.
	//Is drawing a board:
	private HitpointContainer boardDrawStartingPoint = null; //Scope input/(render), read on many places.
	//Is in normal placement mode:
	private boolean placeableBoardIsLaying = true;
	private double placementRotation = 0;
	//Is grabbing:
	private GrabData grabData;
	private double grabRotation;
	//Is resizing:
	private ResizeData resizeData;
	
	//Board placement offset:
	private double fineBoardOffset;
	private double xBoardOffset;
	private double zBoardOffset;
	
	//Rotation fix variables (Pretty much used everywhere):
	private Vector3 fixXAxis = Vector3.xp; //Never NPE, use +X as default.
	private Vector3 lastUpNormal = Vector3.yp; //Used whenever an item is shadow drawn, to use more natural rotations.
	
	//Input handling:
	
	private Controller3D controller;
	
	public Part getCursorObject()
	{
		Hitpoint hitpoint = this.hitpoint;
		return hitpoint != null ? hitpoint.getHitPart() : null;
	}
	
	public Hitpoint getHitpoint()
	{
		return hitpoint;
	}
	
	public boolean isGrabbing()
	{
		return grabData != null;
	}
	
	public boolean isInBoardPlacementMode()
	{
		return grabData == null //Do not flip board while currently grabbing.
				&& sharedData.getCurrentPlaceable() == CompBoard.info //Only flip a board, when actually holding a board.
				&& boardDrawStartingPoint == null //Board is currently being drawn, so do not flip it anymore.
				&& hitpoint.canBePlacedOn(); //We might want to interact with a component. So only flip when placing is possible.
	}
	
	public boolean isDraggingOrGrabbing()
	{
		return wireStartPoint != null || boardDrawStartingPoint != null || grabData != null;
	}
	
	public GrabData getGrabData()
	{
		return grabData;
	}
	
	public boolean allowBoardOffset(boolean finePlacement)
	{
		//TBI: Maybe also disallow it, while movement keys WASD are pressed.
		GrabData grabData = this.grabData;
		if(grabData != null && grabData.getComponent() instanceof CompBoard)
		{
			return true; //When grabbing/copying a board, allow board offset
		}
		Hitpoint hitpoint = this.hitpoint;
		if(finePlacement //Must only allow fine-offset.
				&& boardDrawStartingPoint == null //Must not drag boards.
				&& wireStartPoint == null //Must not draw wire.
				&& currentPlaceable == CompBoard.info //Must be a board selected in hotbar.
				&& hitpoint.getHitPart() instanceof CompBoard //Must be placed on a board.
		)
		{
			HitpointBoard hitpointBoard = (HitpointBoard) hitpoint;
			return (!placeableBoardIsLaying && hitpointBoard.getLocalNormal().getY() != 0)
					|| (placeableBoardIsLaying && hitpointBoard.getLocalNormal().getY() == 0);
		}
		return false;
	}
	
	public void boardOffset(int amount, boolean control, boolean alt)
	{
		gpuTasks.add((unused) -> {
			if(grabData != null && !control)
			{
				CompBoard board = (CompBoard) grabData.getComponent();
				double actualAmount = amount * 0.3;
				if(alt)
				{
					int z = board.getZ() - 1;
					double min = z * -0.15 - 0.00000001;
					double max = z * 0.15 + 0.00000001;
					double willBeOffset = zBoardOffset + actualAmount;
					if(willBeOffset >= min && willBeOffset <= max)
					{
						zBoardOffset += actualAmount;
					}
				}
				else
				{
					int x = board.getX() - 1;
					double min = x * -0.15 - 0.00000001;
					double max = x * 0.15 + 0.00000001;
					double willBeOffset = xBoardOffset + actualAmount;
					if(willBeOffset >= min && willBeOffset <= max)
					{
						xBoardOffset += actualAmount;
					}
				}
			}
			else
			{
				//Only fine offset:
				if(control)
				{
					fineBoardOffset += amount * 0.075;
					if(fineBoardOffset > 0.1511)
					{
						fineBoardOffset = 0.15;
					}
					else if(fineBoardOffset < -0.1511)
					{
						fineBoardOffset = -0.15;
					}
				}
			}
		});
	}
	
	//Click events:
	
	public void flipBoard()
	{
		//Right clicked while placing a board -> change layout:
		placeableBoardIsLaying = !placeableBoardIsLaying;
	}
	
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
		clusterHighlighter.componentRightClicked(part);
	}
	
	public void rightDragOnConnector(Connector connector)
	{
		wireStartPoint = connector;
	}
	
	public void rightDragOnConnectorStop(Hitpoint hitpoint)
	{
		Connector from = this.wireStartPoint;
		gpuTasks.add((unused) -> {
			//Clear this on the render thread, to prevent any state changes while rendering.
			this.wireStartPoint = null;
		});
		
		if(hitpoint == null)
		{
			return; //Abort, did not stop on a connector.
		}
		if(!fullyLoaded)
		{
			return;
		}
		
		Connector to = (Connector) hitpoint.getHitPart();
		if(to == from)
		{
			return; //Aborted on original connector, never create such a wire.
		}
		Vector3 position = hitpoint.getWireCenterPosition();
		if(position == null)
		{
			System.out.println("ERROR: Wire dragging stopped on a connector, but there is no wire-placement data cached. Render thread stuck or human too fast?");
			return;
		}
		Quaternion alignment = hitpoint.getWireAlignment();
		double length = hitpoint.getWireDistance();
		
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
		
		clusterHighlighter.clusterChanged(from.getCluster());
		clusterHighlighter.clusterChanged(to.getCluster());
		
		//Add wire:
		CompWireRaw newWire = new CompWireRaw(board.getPlaceboWireParent());
		newWire.setRotation(alignment);
		newWire.setPosition(position);
		newWire.setLength((float) length * 2f);
		
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
	
	public void rotatePlacement(boolean control)
	{
		gpuTasks.add((unused) -> {
			double degrees = control ? 22.5 : 90;
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
				if(this.currentPlaceable == CompBoard.info)
				{
					//Boards cannot be fine rotated, thus when pressing control, rotate roughly.
					degrees = 90;
				}
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
		});
	}
	
	public void rotateGrabbedBoardX()
	{
		rotateGrabbedBoard(Quaternion.angleAxis(-90, Vector3.xp));
	}
	
	public void rotateGrabbedBoardY()
	{
		rotateGrabbedBoard(Quaternion.angleAxis(-90, Vector3.yp));
	}
	
	public void rotateGrabbedBoardZ()
	{
		rotateGrabbedBoard(Quaternion.angleAxis(-90, Vector3.zp));
	}
	
	public void rotateGrabbedBoard(Quaternion rotator)
	{
		GrabContainerData grabContainerData = (GrabContainerData) grabData;
		
		if(grabContainerData.getAlignment() == null)
		{
			System.out.println("You must first find any placement position, before rotating.");
		}
		else if(hitpoint.canBePlacedOn())
		{
			Quaternion newAlignment = grabContainerData.getAlignment();
			newAlignment = newAlignment.multiply(rotator);
			grabContainerData.setAlignment(newAlignment);
		}
		else
		{
			System.out.println("Please look at some container, before attempting to rotate the grabbed board. [No placement normal vector else].");
		}
	}
	
	public void placementStart()
	{
		Hitpoint hitpoint = this.hitpoint; //Create copy of hitpoint reference, to stay thread-safe.
		//Only called when looking at a container.
		if(hitpoint.canBePlacedOn() && currentPlaceable == CompBoard.info && grabData == null && resizeData == null)
		{
			gpuTasks.add((unused) -> {
				//Start dragging until end.
				if(boardDrawStartingPoint != null)
				{
					System.out.println("Warning: Cannot start board dragging, while already dragging a board...");
				}
				else
				{
					boardDrawStartingPoint = (HitpointContainer) hitpoint;
				}
			});
		}
	}
	
	public void stopClusterHighlighting()
	{
		clusterHighlighter.stop();
	}
	
	public boolean attemptPlacement(boolean abortPlacement)
	{
		//TODO: All these values are not fully thread-safe, either wrap in one object, or make thread-safe.
		Hitpoint hitpoint = this.hitpoint;
		HitpointContainer boardDrawStartingPoint = this.boardDrawStartingPoint;
		PlaceableInfo currentPlaceable = this.currentPlaceable;
		final GrabData grabData = this.grabData;
		
		if(wireStartPoint != null)
		{
			return false; //We are dragging a wire, don't place something!
		}
		
		if(!fullyLoaded)
		{
			return false;
		}
		
		if(boardDrawStartingPoint != null)
		{
			//TBI: Maybe just get rid of this piece of code, then it is not required to prevent hotbar changes once dragging.
			if(currentPlaceable != CompBoard.info)
			{
				System.out.println("WARNING: Called normal placement code without board selected, while boardDrawStartingPoint was not null.");
//				return true;
			}
			
			if(abortPlacement)
			{
				try
				{
					gpuTasks.put((ignored) -> {
						this.boardDrawStartingPoint = null;
						this.fineBoardOffset = 0;
					});
				}
				catch(InterruptedException e)
				{
					e.printStackTrace();
				}
				return true;
			}
			
			CompContainer parent = (CompContainer) boardDrawStartingPoint.getHitPart();
			if(parent != board.getRootBoard() && parent == null)
			{
				System.out.println("Board attempted to draw board on is deleted/gone.");
				return false;
			}
			int x = boardDrawStartingPoint.getBoardX();
			int z = boardDrawStartingPoint.getBoardZ();
			Component newComponent = new CompBoard(parent, x, z);
			newComponent.setRotation(boardDrawStartingPoint.getAlignment());
			newComponent.setPosition(boardDrawStartingPoint.getBoardCenterPosition());
			
			try
			{
				gpuTasks.put((ignored) -> {
					parent.addChild(newComponent);
					parent.updateBounds();
					worldMesh.addComponent(newComponent, board.getSimulation());
					this.boardDrawStartingPoint = null;
					this.fineBoardOffset = 0;
				});
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
			return true; //Don't do all the other checks, obsolete.
		}
		
		//Do not do this check any earlier, cause for whatever reason we might be dragging a board or such.
		if(resizeData != null && grabData == null) //Just make sure that we are not dragging, if we are then something went wrong and placement should be allowed.
		{
			return false;
		}
		if(!hitpoint.canBePlacedOn())
		{
			//If not looking at a container abort.
			return false;
		}
		CompContainer parent = (CompContainer) hitpoint.getHitPart();
		if(parent != board.getRootBoard() && parent.getParent() == null)
		{
			System.out.println("Board attempted to place on is deleted/gone.");
			return false;
		}
		
		HitpointContainer hitpointContainer = (HitpointContainer) hitpoint;
		
		if(grabData != null)
		{
			Component grabbedComponent = grabData.getComponent();
			
			Quaternion deltaAlignment = hitpointContainer.getAlignment();
			Vector3 newPosition = hitpointContainer.getPosition();
			Vector3 oldPosition = grabbedComponent.getPosition();
			for(GrabData.WireContainer wireContainer : grabData.getOutgoingWiresWithSides())
			{
				Wire wire = wireContainer.wire;
				Vector3 thisPos = wire.getConnectorA().getConnectionPoint();
				Vector3 thatPos = wire.getConnectorB().getConnectionPoint();
				if(wireContainer.isGrabbedOnASide)
				{
					thisPos = thisPos.subtract(oldPosition);
					thisPos = deltaAlignment.inverse().multiply(thisPos);
					thisPos = thisPos.add(newPosition);
				}
				else
				{
					thatPos = thatPos.subtract(oldPosition);
					thatPos = deltaAlignment.inverse().multiply(thatPos);
					thatPos = thatPos.add(newPosition);
				}
				
				Vector3 direction = thisPos.subtract(thatPos).divide(2);
				double distance = direction.length();
				Quaternion rotation = MathHelper.rotationFromVectors(Vector3.zp, direction.normalize());
				Vector3 position = thatPos.add(direction);
				
				CompWireRaw cWire = (CompWireRaw) wire;
				cWire.setPosition(position);
				cWire.setRotation(rotation);
				cWire.setLength((float) distance * 2f);
			}
			for(CompWireRaw wire : grabData.getInternalWires())
			{
				alignComponent(wire, oldPosition, newPosition, deltaAlignment);
			}
			if(grabData instanceof GrabContainerData)
			{
				GrabContainerData grabContainerData = (GrabContainerData) grabData;
				for(CompSnappingWire wire : grabContainerData.getInternalSnappingWires())
				{
					alignComponent(wire, oldPosition, newPosition, deltaAlignment);
				}
			}
			
			for(Component component : grabData.getComponents())
			{
				alignComponent(component, oldPosition, newPosition, deltaAlignment);
			}
			
			gpuTasks.add((unused) -> {
				//Move to new meshes:
				for(Component component : grabData.getComponents())
				{
					secondaryMesh.removeComponent(component, board.getSimulation());
					worldMesh.addComponent(component, board.getSimulation());
					if(component instanceof CompLabel && ((CompLabel) component).hasTexture())
					{
						board.getLabelsToRender().add((CompLabel) component);
					}
				}
				
				grabbedComponent.setParent(parent);
				parent.addChild(grabbedComponent);
				grabbedComponent.updateBoundsDeep();
				parent.updateBounds();
				for(Wire wire : grabData.getOutgoingWires())
				{
					CompWireRaw cWire = (CompWireRaw) wire;
					worldMesh.addComponent(cWire, board.getSimulation());
					wireRayCaster.addWire(cWire);
				}
				for(CompWireRaw wire : grabData.getInternalWires())
				{
					secondaryMesh.removeComponent(wire, board.getSimulation());
					wireRayCaster.addWire(wire);
					worldMesh.addComponent(wire, board.getSimulation());
				}
				if(grabData.isCopy())
				{
					for(CompWireRaw wire : grabData.getInternalWires())
					{
						wire.setParent(board.getPlaceboWireParent());
						board.getWiresToRender().add(wire);
					}
				}
				if(grabData instanceof GrabContainerData)
				{
					GrabContainerData grabContainerData = (GrabContainerData) grabData;
					for(CompSnappingWire wire : grabContainerData.getInternalSnappingWires())
					{
						secondaryMesh.removeComponent(wire, board.getSimulation());
						worldMesh.addComponent(wire, board.getSimulation());
					}
					
					for(CompSnappingPeg snappingPeg : grabContainerData.getSnappingPegs())
					{
						snapSnappingPeg(snappingPeg);
					}
				}
				else if(grabData.getComponent() instanceof CompSnappingPeg)
				{
					snapSnappingPeg((CompSnappingPeg) grabData.getComponent());
				}
				
				this.grabData = null;
			});
			
			return true;
		}
		else if(currentPlaceable != null)
		{
			if(currentPlaceable == CompBoard.info)
			{
				//System.out.println("WARNING: Called normal placement code with board selected, while boardDrawStartingPoint was null.");
				//Board was very likely dragged from air to placeable location, happens. Else uff.
				//TODO: Differentiate between these two cases.
				return true;
			}
			
			if(abortPlacement)
			{
				return true;
			}
			
			Component newComponent = currentPlaceable.instance(parent);
			newComponent.setRotation(hitpointContainer.getAlignment());
			newComponent.setPosition(hitpointContainer.getPosition());
			newComponent.init(); //Initializes components such as the ThroughPeg (needs to be called after position is set). TBI: Does it?
			newComponent.initClusters(); //Creates clusters for connectors of the component.
			
			if(newComponent instanceof Updateable)
			{
				board.getSimulation().updateNextTickThreadSafe((Updateable) newComponent);
			}
			
			try
			{
				gpuTasks.put((ignored) -> {
					parent.addChild(newComponent);
					parent.updateBounds();
					worldMesh.addComponent(newComponent, board.getSimulation());
					
					//Link snapping peg:
					if(currentPlaceable == CompSnappingPeg.info)
					{
						snapSnappingPeg((CompSnappingPeg) newComponent);
					}
				});
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
			return true;
		}
		//Else, placing nothing, thus return false.
		
		return false;
	}
	
	//Must be run from render thread.
	private void snapSnappingPeg(CompSnappingPeg snappingPegA)
	{
		//Raycast and see which component gets hit:
		Vector3 snappingPegAConnectionPoint = snappingPegA.getConnectionPoint();
		Vector3 rayA = snappingPegA.getRotation().inverse().multiply(Vector3.zn);
		RayCastResult result = cpuRaycast.cpuRaycast(snappingPegAConnectionPoint, rayA, board.getRootBoard());
		//Check if the result is not null, and a SnappingPeg within 0.2 distance.
		if(result.getMatch() != null && result.getMatch() instanceof Connector && result.getMatch().getParent() instanceof CompSnappingPeg && result.getDistance() <= 0.2)
		{
			CompSnappingPeg snappingPegB = (CompSnappingPeg) result.getMatch().getParent();
			if(!snappingPegB.hasPartner()) //Do not process it further, if it is already connected to somewhere.
			{
				//Calculate their angles to each other:
				Vector3 rayB = snappingPegB.getRotation().inverse().multiply(Vector3.zn);
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
						System.out.println("[ERROR] Cannot place snapping peg wire, cause start- and end-point are probably the same... Please try to not abuse OpenTUNG. Ignore stacktrace, but maybe report this issue if its not intended.");
						return; //Do not connect these, there is something horribly wrong here.
					}
					
					snappingPegB.setPartner(snappingPegA);
					snappingPegA.setPartner(snappingPegB);
					CompSnappingWire wire = new CompSnappingWire(snappingPegA.getParent());
					wire.setLength((float) distance);
					wire.setPosition(snappingPegAConnectionPoint.add(direction.divide(2)));
					wire.setRotation(alignment);
					
					worldMesh.addComponent(wire, board.getSimulation());
					
					board.getSimulation().updateJobNextTickThreadSafe((simulation) -> {
						Map<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> updates = new HashMap<>();
						ClusterHelper.placeWire(simulation, board, snappingPegA.getPegs().get(0), snappingPegB.getPegs().get(0), wire, updates);
						gpuTasks.add((unused) -> {
							System.out.println("[ClusterUpdateDebug] Updating " + updates.size() + " conductor mesh bags.");
							for(Map.Entry<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> entry : updates.entrySet())
							{
								entry.getKey().handleUpdates(entry.getValue(), board.getSimulation());
							}
						});
					});
				}
			}
		}
	}
	
	private void alignComponent(Component component, Vector3 oldPosition, Vector3 newPosition, Quaternion deltaRotation)
	{
		component.setRotation(component.getRotation().multiply(deltaRotation));
		Vector3 newPos = component.getPosition().subtract(oldPosition);
		newPos = deltaRotation.inverse().multiply(newPos);
		newPos = newPos.add(newPosition);
		component.setPosition(newPos);
	}
	
	public void delete(Part toBeDeleted)
	{
		if(isGrabbing())
		{
			return;
		}
		if(boardDrawStartingPoint != null)
		{
			return;
		}
		if(resizeData != null)
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
				CompContainer parentContainer = (CompContainer) parent;
				parentContainer.remove(container);
				parentContainer.updateBounds();
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
					clusterHighlighter.clusterChanged(wireToDelete.getCluster());
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
				for(Wire wire : ((ConnectedComponent) component).getPegs().get(0).getWires())
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
				if(component instanceof ConnectedComponent)
				{
					ConnectedComponent con = (ConnectedComponent) component;
					for(Blot blot : con.getBlots())
					{
						ClusterHelper.removeBlot(simulation, blot, updates);
						wiresToRemove.addAll(blot.getWires());
					}
					for(Peg peg : con.getPegs())
					{
						ClusterHelper.removePeg(simulation, peg, updates);
						wiresToRemove.addAll(peg.getWires());
					}
				}
				
				gpuTasks.add((unused) -> {
					System.out.println("[ClusterUpdateDebug] Updating " + updates.size() + " conductor mesh bags.");
					for(Map.Entry<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> entry : updates.entrySet())
					{
						entry.getKey().handleUpdates(entry.getValue(), board.getSimulation());
					}
					if(component instanceof ConnectedComponent)
					{
						for(Connector connector : ((ConnectedComponent) component).getConnectors())
						{
							clusterHighlighter.clusterChanged(connector.getCluster());
						}
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
						worldMesh.removeComponent((CompWireRaw) wire, board.getSimulation());
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
		if(grabData != null)
		{
			return;
		}
		if(resizeData != null)
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
		//Setting the parent to null is a thread-safe operation. It has to be. Doing this declares this component as "busy", means no other interaction with it should be possible.
		toBeGrabbed.setParent(null);
		
		boolean isContainer = toBeGrabbed instanceof CompContainer;
		GrabData newGrabData;
		if(isContainer)
		{
			//TBI: Not 100% sure, if this separation is needed, well some data is just additional for containers and even more for boards.
			newGrabData = new GrabContainerData(parent, toBeGrabbed);
		}
		else
		{
			newGrabData = new GrabData(parent, toBeGrabbed);
		}
		
		//Collect components and connectors on the render thread. It should not be done on the other threads. Since Render is the modifying thread. This prevents modification while collecting.
		gpuTasks.add((unused) -> {
			//Remove the grabbed component from its parent, and update the parents bounds:
			parent.remove(toBeGrabbed);
			parent.updateBounds();
			
			//Collect connectors: Needed to get the wires from them on the simulation thread.
			List<Connector> connectors = new ArrayList<>();
			//Connect all snapping pegs (the unconnected is for later):
			HashSet<CompSnappingPeg> unconnectedSnappingPegs = new HashSet<>();
			{
				LinkedList<Component> queue = new LinkedList<>();
				queue.addLast(toBeGrabbed);
				while(!queue.isEmpty())
				{
					Component component = queue.removeFirst();
					
					//Add component to the list of components of this grab, to later move them to the right mesh:
					newGrabData.addComponent(component);
					
					//Handle special components:
					if(component instanceof CompLabel)
					{
						board.getLabelsToRender().remove(component); //Remove the label from visible labels.
						if(((CompLabel) component).hasTexture())
						{
							newGrabData.addLabel((CompLabel) component);
						}
					}
					else if(component instanceof CompSnappingPeg)
					{
						unconnectedSnappingPegs.add((CompSnappingPeg) component);
					}
					
					//Collect connectors and append children to process:
					if(component instanceof ConnectedComponent)
					{
						connectors.addAll(((ConnectedComponent) component).getConnectors());
					}
					else if(component instanceof CompContainer)
					{
						for(Component child : ((CompContainer) component).getChildren())
						{
							queue.addLast(child);
						}
					}
				}
			}
			
			//Collect all wires on the simulation thread, has to be done on it, cause then the simulation thread cannot modify wires and clusters while collecting:
			board.getSimulation().updateJobNextTickThreadSafe((simulation) -> {
				//This list serves are temporary list to hold all wires, but all the wires which are connected to the current grab on both sides will be removed again.
				// This way it is detected if a wire is "internal" or "outgoing".
				Map<Wire, Boolean> outgoingWires = new HashMap<>();
				//Normal not outgoing wires:
				List<CompWireRaw> internalWires = new ArrayList<>();
				//SnappingPeg not outgoing wires: Used to detect internal snapping pegs later on.
				List<CompSnappingWire> internalSnappingWires = new ArrayList<>();
				//Iterate over all connectors to capture all wires:
				for(Connector connector : connectors)
				{
					for(Wire wire : connector.getWires())
					{
						//A wire has two ends, thus it can at most be added two times into the outgoingWires set.
						// If it is removed and 'null' returned it was not in it, it is the first time it got added to the list.
						// If the result is not 'null', that means it was already added, both ends are somewhere on the grabbed component, thus it is an internal wire which should fully be grabbed.
						//All wires that remain at the end are outgoing wires which if snapping wire need to be removed, and if normal wire need to be invisible and drawn in a custom way.
						if(outgoingWires.remove(wire) != null)
						{
							if(wire instanceof CompSnappingWire)
							{
								internalSnappingWires.add((CompSnappingWire) wire);
							}
							else if(wire instanceof CompWireRaw)
							{
								internalWires.add((CompWireRaw) wire);
							}
							else if(wire instanceof HiddenWire)
							{
								//Ignore this wire. Its not exposed to anything.
							}
							else
							{
								//An issue if someone wants to mod wires in an unexpected way:
								throw new RuntimeException("Unexpected wire type received: " + wire.getClass().getSimpleName());
							}
						}
						else
						{
							//For each wire, it is important to remember which side was connected to the connector.
							outgoingWires.put(wire, wire.getConnectorA() == connector);
						}
					}
				}
				//Set the current list of internal wires, by now this could be replaced by a method call for each wire once it got detected:
				// Actually it makes much more sense for the copy to also just set the list. So lets keep it this way.
				newGrabData.setInternalWires(internalWires);
				
				//The snapping peg management can and should be done on the simulation thread as much as possible.
				// We should never delay the render thread for longer than required, the simulation we can halt as long as we please for bigger things.
				if(isContainer)
				{
					//Filter the connected snapping pegs from the list of unconnected ones.
					for(CompSnappingWire wire : internalSnappingWires)
					{
						unconnectedSnappingPegs.remove(wire.getConnectorA().getParent());
						unconnectedSnappingPegs.remove(wire.getConnectorB().getParent());
					}
					((GrabContainerData) newGrabData).setUnconnectedSnappingPegs(unconnectedSnappingPegs);
					((GrabContainerData) newGrabData).setInternalSnappingWires(internalSnappingWires);
				}
				else if(!internalSnappingWires.isEmpty())
				{
					//If we only grab a single component, then that one cannot contain an internal snapping wire, since that requires two grabbed snapping peg components.
					// So this code is a failsafe, if such a wire is collected nevertheless.
					new RuntimeException("Internal snapping peg list was not empty. Although it was not a container. Continuing anyway.").printStackTrace(System.out);
				}
				
				//Sort all the outgoing wires to snapping or normal wires:
				// Snapping wires will have to be removed, since we rip the board with them out of where it used to be.
				List<CompSnappingWire> snappingWiresToRemove = new ArrayList<>();
				Map<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> updates = new HashMap<>(); //For simulation updates caused by snapping wire removal.
				for(Map.Entry<Wire, Boolean> outgoingWireEntry : outgoingWires.entrySet())
				{
					Wire wire = outgoingWireEntry.getKey();
					if(wire instanceof CompSnappingWire)
					{
						//Remember the snapping wire for visual updates later:
						snappingWiresToRemove.add((CompSnappingWire) wire);
						//Actually disconnect the snapping peg connections:
						CompSnappingPeg aSide = (CompSnappingPeg) wire.getConnectorA().getParent();
						CompSnappingPeg bSide = (CompSnappingPeg) wire.getConnectorB().getParent();
						aSide.setPartner(null);
						bSide.setPartner(null);
						ClusterHelper.removeWire(simulation, wire, updates);
					}
					else
					{
						newGrabData.addWire((CompWireRaw) wire, outgoingWireEntry.getValue());
					}
				}
				
				//Do the finalization on the render thread, that is doing the mesh modification for the most part:
				gpuTasks.add((unused2) -> {
					//Updates caused by the simulation when ripping out snapping wire connections.
					// In general happens when a cluster gets divided by this action, so that one of the cluster half's is now off.
					System.out.println("[ClusterUpdateDebug] (Grabbing/SnappingWireRemoval) Updating " + updates.size() + " conductor mesh bags.");
					for(Map.Entry<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> entry : updates.entrySet())
					{
						entry.getKey().handleUpdates(entry.getValue(), board.getSimulation());
					}
					
					//If there is a cluster highlighted, for now we want to stop highlighting it, since it might have changed (by ripping snapping connections apart), it is also not possible to highlight the cluster positions if they are on the secondary mesh.
					for(Connector connector : connectors)
					{
						clusterHighlighter.clusterChanged(connector.getCluster());
					}
					
					//Move components into the grab/secondary-mesh: Done in this stage, as a final visual confirmation to the user. Could be done in the first render stage.
					for(Component component : newGrabData.getComponents())
					{
						worldMesh.removeComponent(component, board.getSimulation());
						secondaryMesh.addComponent(component, board.getSimulation());
					}
					//Wires: The general rule for them is, move from primary to secondary mesh. Discard outgoing snapping wires, and remove normal wires from the wire-ray-caster.
					for(CompSnappingWire wire : snappingWiresToRemove)
					{
						worldMesh.removeComponent(wire, board.getSimulation());
					}
					for(CompWireRaw wire : newGrabData.getOutgoingWires())
					{
						wireRayCaster.removeWire(wire);
						worldMesh.removeComponent(wire, board.getSimulation());
					}
					for(CompSnappingWire wire : internalSnappingWires)
					{
						worldMesh.removeComponent(wire, board.getSimulation());
						secondaryMesh.addComponent(wire, board.getSimulation());
					}
					for(CompWireRaw wire : internalWires)
					{
						//Also remove these from the ray-caster:
						wireRayCaster.removeWire(wire);
						worldMesh.removeComponent(wire, board.getSimulation());
						secondaryMesh.addComponent(wire, board.getSimulation());
					}
					
					//Finished the processing, now apply the grabbing:
					fixXAxis = toBeGrabbed.getRotation().inverse().multiply(Vector3.xp);
					lastUpNormal = toBeGrabbed.getRotation().inverse().multiply(Vector3.yp);
					grabRotation = 0;
					if(toBeGrabbed instanceof CompBoard)
					{
						CompBoard board = (CompBoard) toBeGrabbed;
						this.xBoardOffset = ((board.getX() & 1) == 0) ? -0.15 : 0;
						this.zBoardOffset = ((board.getZ() & 1) == 0) ? -0.15 : 0;
					}
					else //Any other component:
					{
						this.xBoardOffset = 0;
						this.zBoardOffset = 0;
					}
					this.fineBoardOffset = 0;
					this.grabData = newGrabData;
				});
			});
		});
	}
	
	public void copy(Component componentToCopy)
	{
		if(wireStartPoint != null)
		{
			return; //We are dragging a wire, don't grab something!
		}
		if(grabData != null)
		{
			return;
		}
		if(resizeData != null)
		{
			return;
		}
		if(componentToCopy instanceof Wire)
		{
			//We don't copy wires.
			return;
		}
		
		//No need to do the parent check, since we want to copy the component and not change it.
		
		gpuTasks.add((unused) -> {
			boolean isContainer = componentToCopy instanceof CompContainer;
			GrabData grabData;
			if(isContainer)
			{
				//TBI: Not 100% sure, if this separation is needed, well some data is just additional for containers and even more for boards.
				grabData = new GrabContainerData(null, null);
			}
			else
			{
				grabData = new GrabData(null, null);
			}
			grabData.setCopy(); //Sets a flag telling the placement code, that this is a copy and not the original. Adds special parents and such. Also totally different abort/delete code.
			
			//Later when working with the wires, we will encounter original components which need to be mapped to their copies.
			// Original => Copy
			HashMap<Component, Component> copiesLookup = new HashMap<>();
			
			//Collect connectors: Needed to get the wires from them on the simulation thread.
			List<Connector> connectors = new ArrayList<>();
			//Connect all snapping pegs (the unconnected is for later):
			HashSet<CompSnappingPeg> unconnectedSnappingPegs = new HashSet<>();
			{
				LinkedList<Tuple<Component>> queue = new LinkedList<>();
				//Add initial component (null parent, since never placed before):
				// Structure of the tuple is <Parent Copy; Original Component>
				queue.add(new Tuple<>(null, componentToCopy));
				while(!queue.isEmpty())
				{
					Tuple<Component> tuple = queue.removeFirst();
					CompContainer parentCopy = (CompContainer) tuple.getFirst(); //Null in the very first loop!
					Component originalComponent = tuple.getSecond();
					
					//Create copy of this component:
					Component copyComponent = originalComponent.copy();
					copiesLookup.put(originalComponent, copyComponent); //Add to the lookup map.
					if(parentCopy != null) //Kind of "uff" that this is only once false (in the first loop).
					{
						//Link the copy parent with its copy child:
						copyComponent.setParent(parentCopy);
						parentCopy.addChild(copyComponent);
					}
					//Add component to the list of components of this copy, to later move them to the right mesh:
					grabData.addComponent(copyComponent);
					
					//Handle special components:
					if(copyComponent instanceof CompLabel)
					{
						if(((CompLabel) copyComponent).hasTexture())
						{
							grabData.addLabel((CompLabel) copyComponent);
						}
					}
					else if(copyComponent instanceof CompSnappingPeg)
					{
						unconnectedSnappingPegs.add((CompSnappingPeg) copyComponent);
					}
					
					//Collect connectors and append children to process:
					if(copyComponent instanceof ConnectedComponent)
					{
						connectors.addAll(((ConnectedComponent) originalComponent).getConnectors());
					}
					else if(copyComponent instanceof CompContainer)
					{
						for(Component child : ((CompContainer) originalComponent).getChildren())
						{
							queue.addLast(new Tuple<>(copyComponent, child));
						}
					}
				}
			}
			//Due to not knowing the copy at the time GrabData got created, it has to be set now - after creating all the copies.
			// The first copy ever made is the copy of the original to be copied component.
			grabData.overwriteGrabbedComponent(grabData.getComponents().get(0));
			
			//Collect all wires on the simulation thread, has to be done on it, cause then the simulation thread cannot modify wires and clusters while collecting:
			board.getSimulation().updateJobNextTickThreadSafe((simulation) -> {
				//This list serves are temporary list to hold all wires, but all the wires which are connected to the current grab on both sides will be removed again.
				// This way it is detected if a wire is "internal" or "outgoing".
				Map<Wire, Boolean> outgoingWires = new HashMap<>(); //When copying, the outgoing wires are only temporary and ignored later on.
				//Normal not outgoing wires:
				List<CompWireRaw> internalWires = new ArrayList<>();
				//SnappingPeg not outgoing wires: Used to detect internal snapping pegs later on.
				List<CompSnappingWire> internalSnappingWires = new ArrayList<>();
				//Iterate over all connectors to capture all wires:
				for(Connector connector : connectors)
				{
					for(Wire wire : connector.getWires())
					{
						//A wire has two ends, thus it can at most be added two times into the outgoingWires set.
						// If it is removed and 'null' returned it was not in it, it is the first time it got added to the list.
						// If the result is not 'null', that means it was already added, both ends are somewhere on the grabbed component, thus it is an internal wire which should fully be grabbed.
						//All wires that remain at the end are outgoing wires which if snapping wire need to be removed, and if normal wire need to be invisible and drawn in a custom way.
						if(outgoingWires.remove(wire) != null)
						{
							if(wire instanceof CompSnappingWire)
							{
								internalSnappingWires.add((CompSnappingWire) wire);
							}
							else if(wire instanceof CompWireRaw)
							{
								internalWires.add((CompWireRaw) wire);
							}
							else if(wire instanceof HiddenWire)
							{
								//Ignore this wire. Its not exposed to anything.
							}
							else
							{
								//An issue if someone wants to mod wires in an unexpected way:
								throw new RuntimeException("Unexpected wire type received: " + wire.getClass().getSimpleName());
							}
						}
						else
						{
							//For each wire, it is important to remember which side was connected to the connector.
							outgoingWires.put(wire, wire.getConnectorA() == connector);
						}
					}
				}
				//Do not set the internal wires yet, they have to be copied first.
				
				//The snapping peg management can and should be done on the simulation thread as much as possible.
				// We should never delay the render thread for longer than required, the simulation we can halt as long as we please for bigger things.
				if(isContainer)
				{
					//All internal snapping peg wires have to be copied:
					List<CompSnappingWire> internalSnappingWiresCopy = new ArrayList<>(internalSnappingWires.size());
					for(CompSnappingWire wire : internalSnappingWires)
					{
						//Get the SnappingPeg copies, by looking them up using the original parents:
						CompSnappingPeg copyA = (CompSnappingPeg) copiesLookup.get(wire.getConnectorA().getParent());
						CompSnappingPeg copyB = (CompSnappingPeg) copiesLookup.get(wire.getConnectorB().getParent());
						//Copy the snapping wire:
						CompSnappingWire copy = (CompSnappingWire) wire.copy();
						internalSnappingWiresCopy.add(copy);
						//Remove all internally connected snapping pegs:
						unconnectedSnappingPegs.remove(copyA);
						unconnectedSnappingPegs.remove(copyB);
						//Link the two snapping pegs with the new wire:
						copyA.setPartner(copyB);
						copyB.setPartner(copyA);
						copy.setConnectorA(copyA.getPegs().get(0));
						copy.setConnectorB(copyB.getPegs().get(0));
						copyA.getPegs().get(0).addWire(copy);
						copyB.getPegs().get(0).addWire(copy);
					}
					//This list is used to connect the snapping pegs on placement:
					((GrabContainerData) grabData).setUnconnectedSnappingPegs(unconnectedSnappingPegs);
					//This list is only to place the wires later on, and add them to the secondary mesh:
					((GrabContainerData) grabData).setInternalSnappingWires(internalSnappingWiresCopy);
				}
				else if(!internalSnappingWires.isEmpty())
				{
					//If we only grab a single component, then that one cannot contain an internal snapping wire, since that requires two grabbed snapping peg components.
					// So this code is a failsafe, if such a wire is collected nevertheless.
					new RuntimeException("Internal snapping peg list was not empty. Although it was not a container. Continuing anyway.").printStackTrace(System.out);
				}
				
				//All the internal wires have to be copied:
				List<CompWireRaw> internalWiresCopy = new ArrayList<>(internalWires.size());
				for(CompWireRaw wire : internalWires)
				{
					//Get the copies of the two wire end components:
					ConnectedComponent copyA = (ConnectedComponent) copiesLookup.get(wire.getConnectorA().getParent());
					ConnectedComponent copyB = (ConnectedComponent) copiesLookup.get(wire.getConnectorB().getParent());
					//For both sides perform the same code (with other variables).
					// Here if the connector was a blot, we access the blots of the copy via index, since blots know their index.
					// But if it was a peg, we have to compare them, since they are not aware of their index/position.
					Connector connectorCopyA = null;
					{
						if(wire.getConnectorA().getClass() == Blot.class)
						{
							connectorCopyA = copyA.getBlots().get(((Blot) wire.getConnectorA()).getIndex());
						}
						else
						{
							List<Peg> pegs = ((ConnectedComponent) wire.getConnectorA().getParent()).getPegs();
							for(int i = 0; i < pegs.size(); i++)
							{
								if(pegs.get(i) == wire.getConnectorA())
								{
									connectorCopyA = copyA.getPegs().get(i);
									break;
								}
							}
							if(connectorCopyA == null)
							{
								throw new RuntimeException("Could find the connector of a copy, which a wire was connected to.");
							}
						}
					}
					Connector connectorCopyB = null;
					{
						if(wire.getConnectorB().getClass() == Blot.class)
						{
							connectorCopyB = copyB.getBlots().get(((Blot) wire.getConnectorB()).getIndex());
						}
						else
						{
							List<Peg> pegs = ((ConnectedComponent) wire.getConnectorB().getParent()).getPegs();
							for(int i = 0; i < pegs.size(); i++)
							{
								if(pegs.get(i) == wire.getConnectorB())
								{
									connectorCopyB = copyB.getPegs().get(i);
									break;
								}
							}
							if(connectorCopyB == null)
							{
								throw new RuntimeException("Could find the connector of a copy, which a wire was connected to.");
							}
						}
					}
					
					//Actually copy the wire and set both connectors:
					CompWireRaw copy = (CompWireRaw) wire.copy();
					internalWiresCopy.add(copy);
					copy.setConnectorA(connectorCopyA);
					copy.setConnectorB(connectorCopyB);
					connectorCopyA.addWire(copy);
					connectorCopyB.addWire(copy);
				}
				grabData.setInternalWires(internalWiresCopy);
				
				//Create clusters for all components:
				// Basically the initial cluster creation code is used, the one in charge when loading boards. Same thing happening here.
				for(Component comp : grabData.getComponents())
				{
					if(comp instanceof ConnectedComponent)
					{
						for(Blot blot : ((ConnectedComponent) comp).getBlots())
						{
							InitClusterHelper.createBlottyCluster(blot);
						}
					}
				}
				//First Blots then Pegs:
				for(Component comp : grabData.getComponents())
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
				
				//Copy state and prime clusters:
				for(Map.Entry<Component, Component> entry : copiesLookup.entrySet())
				{
					Component original = entry.getKey();
					Component copy = entry.getValue();
					if(original instanceof Powerable)
					{
						//Component was powerable, that basically means it stores a bit for its Blot output. Copy that bit/state.
						//TODO: Use the correct index, currently components only have one Blot, if that changes, this code fails.
						((Powerable) copy).setPowered(0, ((Powerable) original).isPowered(0));
						//This call tells the Blot/Source-Cluster connected to the component to be updated, this has to be done, since normally blots query the state of the component and not the other way round.
						((Powerable) copy).forceUpdateOutput();
					}
					if(copy instanceof Updateable)
					{
						//Each and every component will be queued up in the simulation queue, if it can be updated. They are designed to handle that.
						// With that, the simulation naturally continues as if the copy never happened.
						simulation.updateNextTick((Updateable) copy);
					}
				}
				
				//Do the finalization on the render thread, that is doing the mesh modification for the most part:
				gpuTasks.add((stillUnused) -> {
					//Add copied components into the grab/secondary-mesh: Done in this stage, as a final visual confirmation to the user. Could be done in the first render stage.
					for(Component comp : grabData.getComponents())
					{
						secondaryMesh.addComponent(comp, board.getSimulation());
					}
					//Wires: The general rule for them is, add them to the secondary mesh. Skip any outgoing wire.
					for(CompWireRaw wire : internalWiresCopy)
					{
						secondaryMesh.addComponent(wire, board.getSimulation());
					}
					if(isContainer)
					{
						//Has to be wrapped in isContainer, since we need the copy of the list, not the originals.
						for(CompSnappingWire wire : ((GrabContainerData) grabData).getInternalSnappingWires())
						{
							secondaryMesh.addComponent(wire, board.getSimulation());
						}
					}
					
					//Finished the processing, now apply the grabbing:
					this.fixXAxis = componentToCopy.getRotation().inverse().multiply(Vector3.xp);
					this.lastUpNormal = componentToCopy.getRotation().inverse().multiply(Vector3.yp);
					this.grabRotation = 0;
					if(componentToCopy instanceof CompBoard)
					{
						CompBoard board = (CompBoard) componentToCopy;
						this.xBoardOffset = ((board.getX() & 1) == 0) ? -0.15 : 0;
						this.zBoardOffset = ((board.getZ() & 1) == 0) ? -0.15 : 0;
					}
					else //Any other component:
					{
						this.xBoardOffset = 0;
						this.zBoardOffset = 0;
					}
					this.fineBoardOffset = 0;
					this.grabData = grabData;
				});
			});
		});
	}
	
	public void deleteGrabbed()
	{
		{
			GrabData gd = grabData;
			if(gd != null && gd.isCopy())
			{
				abortGrabbing();
				return;
			}
		}
		board.getSimulation().updateJobNextTickThreadSafe((unused) -> {
			GrabData grabDataCopy = grabData;
			if(grabDataCopy == null)
			{
				System.out.println("Attempted to delete grabbed, while currently not grabbing.");
				//Was deleted. But data is no longer valid/present - how though?.
				return;
			}
			Map<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> updates = new HashMap<>();
			//TBI: Removing wires first? Or just all blots/pegs?
			for(Wire wire : grabDataCopy.getOutgoingWires())
			{
				//Outgoing wires:
				ClusterHelper.removeWire(board.getSimulation(), wire, updates);
			}
			if(grabDataCopy instanceof GrabContainerData)
			{
				GrabContainerData grabContainerData = (GrabContainerData) grabDataCopy;
				for(CompSnappingWire wire : grabContainerData.getInternalSnappingWires())
				{
					ClusterHelper.removeWire(board.getSimulation(), wire, updates);
				}
				for(CompWireRaw wire : grabContainerData.getInternalWires())
				{
					ClusterHelper.removeWire(board.getSimulation(), wire, updates);
				}
			}
			for(Component component : grabDataCopy.getComponents())
			{
				if(component instanceof ConnectedComponent)
				{
					ConnectedComponent con = (ConnectedComponent) component;
					for(Peg peg : con.getPegs())
					{
						ClusterHelper.removePeg(board.getSimulation(), peg, updates);
					}
					for(Blot blot : con.getBlots())
					{
						ClusterHelper.removeBlot(board.getSimulation(), blot, updates);
					}
				}
			}
			
			gpuTasks.add((unused2) -> {
				System.out.println("[ClusterUpdateDebug] Updating " + updates.size() + " conductor mesh bags.");
				for(Map.Entry<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> entry : updates.entrySet())
				{
					entry.getKey().handleUpdates(entry.getValue(), board.getSimulation());
				}
				for(Component component : grabDataCopy.getComponents())
				{
					secondaryMesh.removeComponent(component, board.getSimulation());
					if(component instanceof CompLabel)
					{
						((CompLabel) component).unload();
					}
				}
				if(grabDataCopy instanceof GrabContainerData)
				{
					GrabContainerData grabContainerData = (GrabContainerData) grabDataCopy;
					for(CompSnappingWire wire : grabContainerData.getInternalSnappingWires())
					{
						secondaryMesh.removeComponent(wire, board.getSimulation());
					}
					for(CompWireRaw wire : grabContainerData.getInternalWires())
					{
						secondaryMesh.removeComponent(wire, board.getSimulation());
						board.getWiresToRender().remove(wire);
					}
				}
				for(Wire wire : grabDataCopy.getOutgoingWires())
				{
					//Now snapping nor hidden wires at this point.
					board.getWiresToRender().remove(wire);
				}
				
				//That's pretty much it. Just make the clipboard invisible:
				grabData = null;
			});
		});
	}
	
	public void abortGrabbing()
	{
		gpuTasks.add((unused) -> {
			if(grabData == null)
			{
				System.out.println("Attempted to abort grabbing, while currently not grabbing.");
				return; //Lol how did we even got here?
			}
			System.out.println("Abort grabbing...");
			
			if(grabData.isCopy())
			{
				for(CompLabel label : grabData.getLabels())
				{
					label.unload();
				}
				for(Component comp : grabData.getComponents())
				{
					secondaryMesh.removeComponent(comp, board.getSimulation());
				}
				if(grabData instanceof GrabContainerData)
				{
					GrabContainerData gcd = (GrabContainerData) grabData;
					for(CompWireRaw wire : gcd.getInternalWires())
					{
						secondaryMesh.removeComponent(wire, board.getSimulation());
					}
					for(CompSnappingWire wire : gcd.getInternalSnappingWires())
					{
						secondaryMesh.removeComponent(wire, board.getSimulation());
					}
					board.getSimulation().updateJobNextTickThreadSafe((simulation) -> {
						//Supply 'null' as update collection map, cause the wires are all invisible by now, code will never be called.
						for(CompWireRaw wire : gcd.getInternalWires())
						{
							ClusterHelper.removeWire(simulation, wire, null);
						}
						//Iterating over connectors is obsolete once all wires are removed.
					});
				}
				
				grabData = null;
				return;
			}
			
			for(Component component : grabData.getComponents())
			{
				secondaryMesh.removeComponent(component, board.getSimulation());
				worldMesh.addComponent(component, board.getSimulation());
				if(component instanceof CompLabel)
				{
					if(((CompLabel) component).hasTexture())
					{
						board.getLabelsToRender().add((CompLabel) component);
					}
				}
			}
			
			for(Wire wire : grabData.getOutgoingWires())
			{
				CompWireRaw cWire = (CompWireRaw) wire;
				worldMesh.addComponent(cWire, board.getSimulation());
				//Snapping peg wires should not and technically can't be in the wire ray-caster...
				wireRayCaster.addWire(cWire);
			}
			if(grabData instanceof GrabContainerData)
			{
				GrabContainerData grabContainerData = (GrabContainerData) grabData;
				for(CompWireRaw wire : grabContainerData.getInternalWires())
				{
					CompWireRaw compWireRaw = wire;
					secondaryMesh.removeComponent(compWireRaw, board.getSimulation());
					wireRayCaster.addWire(compWireRaw);
					worldMesh.addComponent(compWireRaw, board.getSimulation());
				}
				for(CompSnappingWire wire : grabContainerData.getInternalSnappingWires())
				{
					secondaryMesh.removeComponent(wire, board.getSimulation());
					worldMesh.addComponent(wire, board.getSimulation());
				}
			}
			
			//Update parent relation:
			Component grabbedComponent = grabData.getComponent();
			CompContainer grabbedParent = grabData.getParent();
			grabbedParent.addChild(grabbedComponent);
			grabbedParent.updateBounds();
			grabbedComponent.setParent(grabbedParent);
			
			grabData = null;
		});
	}
	
	public Quaternion getDeltaGrabRotation(HitpointContainer hitpoint)
	{
		Component component = grabData.getComponent();
		boolean grabbingBoard = component instanceof CompBoard;
		
		//Calculate the new alignment:
		Quaternion absAlignment = MathHelper.rotationFromVectors(Vector3.yp, hitpoint.getNormal()); //Get the direction of the new placement position (with invalid rotation).
		double normalAxisRotationAngle = calculateFixRotationOffset(absAlignment, hitpoint);
		if(!grabbingBoard)
		{
			normalAxisRotationAngle -= grabRotation;
		}
		Quaternion normalAxisRotation = Quaternion.angleAxis(normalAxisRotationAngle, hitpoint.getNormal()); //Create rotation Quaternion.
		absAlignment = absAlignment.multiply(normalAxisRotation); //Apply rotation onto new direction to get alignment.
		
		if(grabbingBoard)
		{
			Quaternion currentAlignment = ((GrabContainerData) grabData).getAlignment();
			if(currentAlignment == null)
			{
				//Generate the alignment:
				currentAlignment = component.getRotation().multiply(absAlignment.inverse());
				((GrabContainerData) grabData).setAlignment(currentAlignment);
			}
			absAlignment = currentAlignment.multiply(absAlignment);
		}
		
		//Since the Rotation cannot be changed, it must be modified. So we undo the old rotation and apply the new one.
		return component.getRotation().inverse().multiply(absAlignment);
	}
	
	private double getBoardDistance(Quaternion alignment, CompBoard board)
	{
		alignment = alignment.inverse();
		double distance = 0;
		if(isAlignedWithYAxis(alignment, Vector3.xp))
		{
			distance = (double) board.getX() * 0.15D;
		}
		else if(isAlignedWithYAxis(alignment, Vector3.yp))
		{
			distance = 0.075;
		}
		else if(isAlignedWithYAxis(alignment, Vector3.zp))
		{
			distance = (double) board.getZ() * 0.15D;
		}
		else
		{
			System.out.println("WARNING, no axis of the grabbed board matches the Y-Axis. Abort.");
		}
		return distance;
	}
	
	private boolean isAlignedWithYAxis(Quaternion alignment, Vector3 probe)
	{
		//Apply the rotation onto the vector, and check if the result is aligned with the Y-Axis.
		Vector3 changed = alignment.multiply(probe);
		return changed.getY() > 0.98D || changed.getY() < -0.98D;
	}
	
	public boolean isResizing()
	{
		return resizeData != null;
	}
	
	public void boardResize()
	{
		Hitpoint hitpoint = this.hitpoint; //Save as first action, regardless of state.
		
		if(resizeData == null)
		{
			//Start resizing
			if(hitpoint.getHitPart() instanceof CompBoard)
			{
				System.out.println("Starting board resizing.");
				gpuTasks.add((unused) -> {
					CompBoard board = (CompBoard) hitpoint.getHitPart();
					worldMesh.removeComponent(board, this.board.getSimulation());
					resizeData = new ResizeData(board);
					//TODO: Find minimal expansion for each side.
				});
			}
			else
			{
				//TBI: Maybe just loop to the parent until board found?
				System.out.println("Only boards can be resized, please look at one when starting resizing.");
			}
		}
		else
		{
			System.out.println("Stopping board resizing.");
			//Apply resizing
			gpuTasks.add((unused) -> {
				if(resizeData == null)
				{
					return; //No resize to apply (anymore).
				}
				CompBoard board = resizeData.getBoard();
				board.setPosition(resizeData.getPosition());
				board.setSize(resizeData.getBoardX(), resizeData.getBoardZ());
				board.createOwnBounds(); //The board's own size has changed, update its bounds.
				board.updateBounds(); //Notify the parents that the bounds of this component changed.
				worldMesh.addComponent(board, this.board.getSimulation());
				resizeData = null;
			});
		}
	}
	
	public void abortResizing()
	{
		System.out.println("Aborting board resizing.");
		//For now just set the resize data to null, which stops resizing.
		gpuTasks.add((unused) -> {
			if(resizeData == null)
			{
				return; //Already aborted/finished.
			}
			worldMesh.addComponent(resizeData.getBoard(), board.getSimulation());
			resizeData = null;
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
		
		skybox.setup();
		
		camera = new Camera();
		
		worldMesh.setup(board, board.getWiresToRender(), board.getSimulation());
		
		gpuTasks.add(world3D -> {
			IconGeneration.render(shaderStorage);
			//Restore the projection matrix and viewport of this shader, since they got abused.
			shaderStorage.resetViewportAndVisibleCubeShader();
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
	
	@Override
	public void render()
	{
		//Handle jobs passed from other threads:
		while(!gpuTasks.isEmpty())
		{
			gpuTasks.poll().execute(this);
		}
		
		//Cleanup all meshes before using them:
		worldMesh.rebuildDirty(board.getSimulation());
		secondaryMesh.rebuildDirty(board.getSimulation());
		
		//Handle inputs:
		camera.lockLocation();
		controller.doFrameCycle();
		float[] view = camera.getMatrix();
		
		//Use current camera to find the thing its pointing at:
		doPlacementStuff();
		
		//Actually draw the world:
		if(Settings.drawWorld)
		{
			OpenTUNG.setBackgroundColor();
			OpenTUNG.clear();
			
			drawDynamic(view);
			if(wireStartPoint != null)
			{
				drawWireToBePlaced(view);
			}
			else if(boardDrawStartingPoint != null)
			{
				drawBoardToBePlaced(view);
			}
			else if(grabData != null)
			{
				if(hitpoint.canBePlacedOn())
				{
					drawGrabbed(view);
				}
			}
			else if(resizeData != null)
			{
				drawBoardResize(view);
			}
			else if(hitpoint.canBePlacedOn())
			{
				drawPlacementPosition(view);
			}
			
			clusterHighlighter.highlightCluster(view);
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
			
			if(Settings.drawSkybox)
			{
				//Warning this instruction destroys the view-matrix.
				skybox.render(view);
			}
		}
	}
	
	private void doPlacementStuff()
	{
		if(!fullyLoaded)
		{
			//The world is not fully loaded yet, thus prevent any interaction (by never looking at anything).
			return;
		}
		if(sharedData.isSaving())
		{
			//Currently saving, that means - leave everything as is, do not touch anything.
			return;
		}
		
		//TODO: Raycasting setting where and how? Well not now.
		
		Hitpoint hitpoint = calculateHitpoint();
		PlaceableInfo currentPlaceable = sharedData.getCurrentPlaceable();
		
		boolean skipHitpoint = false;
		if(resizeData != null)
		{
			//Check collision with drag-area:
			Quaternion alignment = resizeData.getBoard().getRotation();
			Vector3 position = resizeData.getPosition();
			
			//Calculate the camera ray in board space:
			Vector3 cameraPosition = camera.getPosition();
			Vector3 cameraRay = Vector3.zp;
			cameraRay = Quaternion.angleAxis(camera.getNeck(), Vector3.xn).multiply(cameraRay);
			cameraRay = Quaternion.angleAxis(camera.getRotation(), Vector3.yn).multiply(cameraRay);
			Vector3 cameraRayBoardSpace = alignment.multiply(cameraRay);
			Vector3 cameraPositionBoardSpace = alignment.multiply(cameraPosition.subtract(position));
			
			//Calculate hitpoint of Y=0 layer:
			boolean isMouseDown = controller.isLeftMouseDown();
			double rayDistanceMultiplicator = -cameraPositionBoardSpace.getY() / cameraRayBoardSpace.getY();
			if(rayDistanceMultiplicator >= 0)
			{
				Vector3 cameraToCollisionVector = cameraRayBoardSpace.multiply(rayDistanceMultiplicator);
				Vector3 collisionPoint = cameraPositionBoardSpace.add(cameraToCollisionVector);
				double collisionX = collisionPoint.getX();
				double collisionZ = collisionPoint.getZ();
				
				if(!resizeData.isMouseDown() && isMouseDown)
				{
					double xVal = Math.abs(collisionX);
					double zVal = Math.abs(collisionZ);
					boolean xMatch = xVal > (double) resizeData.getBoardX() * 0.15;
					boolean zMatch = zVal > (double) resizeData.getBoardZ() * 0.15;
					if(xMatch != zMatch)
					{
						xVal -= (double) resizeData.getBoardX() * 0.15;
						zVal -= (double) resizeData.getBoardZ() * 0.15;
						xMatch &= xVal < 0.6;
						zMatch &= zVal < 0.6;
						if(xMatch || zMatch)
						{
							skipHitpoint = true;
							resizeData.setPoints(collisionX, collisionZ);
							resizeData.setAxisX(xMatch);
							resizeData.setNegative(xMatch ? collisionX < 0 : collisionZ < 0);
						}
					}
				}
				else if(resizeData.hasPoints() && isMouseDown)
				{
					skipHitpoint = true;
					double relevantOldAxisPoint = resizeData.isAxisX() ? resizeData.getPointX() : resizeData.getPointZ();
					double relevantCollisionAxis = resizeData.isAxisX() ? collisionX : collisionZ;
					double diff = relevantOldAxisPoint - relevantCollisionAxis;
					if(!resizeData.isNegative())
					{
						diff = -diff;
					}
					int squareChange = (int) (diff / 0.3);
					resizeData.adjustSize(squareChange);
				}
			}
			if(resizeData.hasPoints() && !isMouseDown && resizeData.isMouseDown())
			{
				resizeData.setPoints(null, null);
			}
			resizeData.setMouseDown(isMouseDown);
		}
		
		//Always calculate the hitpoint, if things could be placed on, regardless of used. It always has to be ready to be used in the next cycle and between.
		if(hitpoint.canBePlacedOn())
		{
			//TBI: When drawing, the hitpoint is inaccurate, cause it might consider the wrong target position (when control).
			HitpointContainer hitpointContainer = (HitpointContainer) hitpoint;
			
			//Calculate normal (it is almost always required for later rotation steps):
			if(hitpoint.isBoard())
			{
				HitpointBoard hitpointBoard = (HitpointBoard) hitpoint;
				hitpointBoard.setNormal(hitpoint.getHitPart().getRotation().inverse().multiply(hitpointBoard.getLocalNormal()).normalize());
			}
			else //TBI: Currently just assume Y-Pos
			{
				hitpointContainer.setNormal(hitpoint.getHitPart().getRotation().inverse().multiply(Vector3.yp).normalize());
			}
			
			if(grabData != null)
			{
				Quaternion deltaAlignment = getDeltaGrabRotation(hitpointContainer);
				hitpointContainer.setAlignment(deltaAlignment);
				
				//Figure out the base position:
				Vector3 position;
				{
					//Calculate new position:
					CompContainer parent = (CompContainer) hitpoint.getHitPart();
					if(hitpoint.isBoard())
					{
						HitpointBoard hitpointBoard = (HitpointBoard) hitpoint;
						OnBoardPlacementHelper placementHelper = new OnBoardPlacementHelper((CompBoard) parent, hitpointBoard.getLocalNormal(), hitpointBoard.getCollisionPointBoardSpace());
						position = placementHelper.middleEither();
						position = parent.getRotation().inverse().multiply(position).add(parent.getPosition());
						if(grabData.getComponent() instanceof CompBoard)
						{
							boolean isLaying;
							boolean isSideX = false; //Which the top/bottom is not facing.
							{
								Vector3 rotated = ((GrabContainerData) grabData).getAlignment().multiply(Vector3.yp);
								isLaying = rotated.getY() > 0.9 || rotated.getY() < -0.9;
								if(!isLaying)
								{
									isSideX = rotated.getZ() > 0.9 || rotated.getZ() < -0.9;
								}
							}
							
							double x = isLaying || isSideX ? xBoardOffset : 0;
							double y = 0;
							double z = isLaying || !isSideX ? zBoardOffset : 0;
							if(placementHelper.isSide())
							{
								if(isLaying)
								{
									//TODO: This code depends on where the normal of the parent points, instead of the rotation of the child.
									//Code should work like the one above, the problem is, that the offset has to be applied to either X or Z depending on rotation.
									Vector3 offset = new Vector3(0, fineBoardOffset, 0); //In parent board space, thus only up/down = Y.
									offset = parent.getRotation().inverse().multiply(offset);
									position = position.add(offset);
								}
							}
							else if(!isLaying) //isTop/Bottom and standing
							{
								y = fineBoardOffset;
							}
							Vector3 offset = new Vector3(x, y, z);
							Quaternion absAlignment = grabData.getComponent().getRotation().multiply(deltaAlignment);
							offset = absAlignment.inverse().multiply(offset);
							position = position.add(offset);
						}
						else if(grabData.getComponent() instanceof CompMount)
						{
							if(!placementHelper.isSide() && !inputHandler.getController3D().isControl())
							{
								//Apply offset:
								Vector3 offset = new Vector3(0, 0, -0.15);
								Quaternion absAlignment = grabData.getComponent().getRotation().multiply(deltaAlignment);
								position = position.add(absAlignment.inverse().multiply(offset));
							}
						}
					}
					else //Mount:
					{
						position = parent.getPosition().add(hitpointContainer.getNormal().multiply(CompMount.MOUNT_HEIGHT));
					}
				}
				
				Component grabComponent = grabData.getComponent();
				if(grabComponent instanceof CompBoard)
				{
					//Grabbing board:
					Quaternion alignment = ((GrabContainerData) grabData).getAlignment();
					CompBoard board = (CompBoard) grabData.getComponent();
					//Calculate distance of grabbed board to parent board:
					position = position.add(hitpointContainer.getNormal().multiply(getBoardDistance(alignment, board) + 0.075D));
				}
				hitpointContainer.setPosition(position);
			}
			else //Normal placement (drawPlacementPosition)
			{
				if(currentPlaceable == null)
				{
					//TBI: 0.075 Offset here or when drawing?
					//Not attempting to draw anything, thus only prepare for drawing the cross, that means lifting the position up to surface:
					CompContainer parent = (CompContainer) hitpoint.getHitPart();
					if(hitpoint.isBoard())
					{
						HitpointBoard hitpointBoard = (HitpointBoard) hitpoint;
						OnBoardPlacementHelper helper = new OnBoardPlacementHelper((CompBoard) parent, hitpointBoard.getLocalNormal(), hitpointBoard.getCollisionPointBoardSpace());
						Vector3 collisionPointBoardSpace = helper.middleEither();
						hitpointContainer.setPosition(parent.getRotation().inverse().multiply(collisionPointBoardSpace)
								.add(parent.getPosition())
								.add(hitpointContainer.getNormal().multiply(0.075))
						);
					}
					else //Mount:
					{
						hitpointContainer.setPosition(parent.getPosition()
								.add(hitpointContainer.getNormal().multiply(CompMount.MOUNT_HEIGHT + 0.075D)));
					}
				}
				else //Placing something:
				{
					//Calculate new alignment:
					Quaternion alignment = MathHelper.rotationFromVectors(Vector3.yp, hitpointContainer.getNormal());
					double rotation = placementRotation;
					if(currentPlaceable == CompBoard.info)
					{
						//Boards cannot be fine-rotated.
						rotation = toRoughRotation();
					}
					double normalAxisRotationAngle = -rotation + calculateFixRotationOffset(alignment, hitpoint);
					Quaternion normalAxisRotation = Quaternion.angleAxis(normalAxisRotationAngle, hitpointContainer.getNormal());
					alignment = alignment.multiply(normalAxisRotation);
					if(currentPlaceable == CompBoard.info)
					{
						//Specific board rotation:
						Quaternion boardAlignment = Quaternion.angleAxis(placeableBoardIsLaying ? 0 : 90, Vector3.xn);
						alignment = boardAlignment.multiply(alignment);
					}
					hitpointContainer.setAlignment(alignment);
					
					//Calculate new position:
					CompContainer parent = (CompContainer) hitpoint.getHitPart();
					if(hitpoint.isBoard())
					{
						HitpointBoard hitpointBoard = (HitpointBoard) hitpoint;
						OnBoardPlacementHelper placementHelper = new OnBoardPlacementHelper((CompBoard) parent, hitpointBoard.getLocalNormal(), hitpointBoard.getCollisionPointBoardSpace());
						Vector3 position = placementHelper.auto(currentPlaceable.getModel(), inputHandler.getController3D().isControl(), alignment);
						if(position == null && currentPlaceable == CompSnappingPeg.info)
						{
							//Attempt again without control: (Should center it).
							position = placementHelper.auto(currentPlaceable.getModel(), false, alignment);
						}
						if(position == null)
						{
							hitpoint = new Hitpoint(hitpoint.getHitPart(), hitpoint.getDistance()); //Prevent the component from being drawn, by just changing the hitpoint type. [pretend non-container]
						}
						else
						{
							position = parent.getRotation().inverse().multiply(position).add(parent.getPosition());
							if(currentPlaceable == CompMount.info)
							{
								if(!placementHelper.isSide() && !inputHandler.getController3D().isControl())
								{
									//Apply offset:
									Vector3 offset = new Vector3(0, 0, -0.15);
									position = position.add(alignment.inverse().multiply(offset));
								}
							}
							else if(currentPlaceable == CompBoard.info)
							{
								double distance = 0.15D;
								if(!placeableBoardIsLaying)
								{
									distance += 0.075D;
									if(!placementHelper.isSide() && fineBoardOffset != 0)
									{
										Vector3 offset = new Vector3(0, fineBoardOffset, 0);
										offset = alignment.inverse().multiply(offset);
										position = position.add(offset);
									}
								}
								else
								{
									if(placementHelper.isSide() && fineBoardOffset != 0)
									{
										//TODO: This code depends on where the normal of the parent points, instead of the rotation of the child.
										//Code should work like the one above, the problem is, that the offset has to be applied to either X or Z depending on rotation.
										Vector3 offset = new Vector3(0, fineBoardOffset, 0); //In parent board space, thus only up/down = Y.
										offset = parent.getRotation().inverse().multiply(offset);
										position = position.add(offset);
									}
								}
								position = position.add(hitpointContainer.getNormal().multiply(distance));
							}
							hitpointContainer.setPosition(position);
						}
					}
					else //Mount:
					{
						ModelHolder model = currentPlaceable.getModel();
						if(currentPlaceable == CompBoard.info)
						{
							double extraY = 0.15;
							if(!placeableBoardIsLaying)
							{
								extraY += 0.075;
							}
							hitpointContainer.setPosition(parent.getPosition().add(hitpointContainer.getNormal().multiply(CompMount.MOUNT_HEIGHT + extraY)));
						}
						else if(model.canBePlacedOnMounts())
						{
							hitpointContainer.setPosition(parent.getPosition().add(hitpointContainer.getNormal().multiply(CompMount.MOUNT_HEIGHT)));
						}
						else
						{
							hitpoint = new Hitpoint(hitpoint.getHitPart(), hitpoint.getDistance()); //Prevent the component from being drawn, by just changing the hitpoint type. [pretend non-container]
						}
					}
				}
			}
		}
		
		if(boardDrawStartingPoint != null)
		{
			//Drawing a board:
			Quaternion alignment = boardDrawStartingPoint.getAlignment();
			Vector3 position = boardDrawStartingPoint.getPosition();
			int x = 1;
			int z = 1;
			
			//Calculate the camera ray in board space:
			Vector3 cameraPosition = camera.getPosition();
			Vector3 cameraRay = Vector3.zp;
			cameraRay = Quaternion.angleAxis(camera.getNeck(), Vector3.xn).multiply(cameraRay);
			cameraRay = Quaternion.angleAxis(camera.getRotation(), Vector3.yn).multiply(cameraRay);
			Vector3 cameraRayBoardSpace = alignment.multiply(cameraRay);
			Vector3 cameraPositionBoardSpace = alignment.multiply(cameraPosition.subtract(position));
			
			boolean calculateSizeAndPos = true;
			
			if(boardDrawStartingPoint.getCameraPosition() == null && boardDrawStartingPoint.getCameraRay() == null)
			{
				//If both values are null, then we are running this code for the very first time. Thus apply the values:
				boardDrawStartingPoint.setCamera(cameraPositionBoardSpace, cameraRayBoardSpace);
				calculateSizeAndPos = false; //Once this runs for the very first time, the camera probably has not moved, thus do not grow the board.
			}
			else if(boardDrawStartingPoint.getCameraRay() != null)
			{
				//At least one of the two values is not null, check if camera-ray got reset, which indicates, that the user interacted with the board.
				//Calculate camera ray angle change and camera position change:
				double angle = MathHelper.angleFromVectors(cameraRayBoardSpace, boardDrawStartingPoint.getCameraRay());
				double distance = cameraPositionBoardSpace.subtract(boardDrawStartingPoint.getCameraPosition()).length();
				
				if(angle > 1.5 || distance > 0.1)
				{
					//User moved enough to disable this check.
					boardDrawStartingPoint.setCameraRay(null);
				}
				else
				{
					//User did not move enough, prevent board growing.
					calculateSizeAndPos = false;
				}
			}
			
			if(calculateSizeAndPos)
			{
				double rayDistanceMultiplicatorUp = -(cameraPositionBoardSpace.getY() + 0.075D) / cameraRayBoardSpace.getY();
				double rayDistanceMultiplicatorBot = -(cameraPositionBoardSpace.getY() - 0.075D) / cameraRayBoardSpace.getY();
				if(rayDistanceMultiplicatorUp >= 0 || rayDistanceMultiplicatorBot >= 0) //One of the two values is above or 0.
				{
					double rayDistanceMultiplicator;
					//Choose the shorter multiplicator, to get the nearer plane.
					if(rayDistanceMultiplicatorUp < 0 //If the Up value is negative, choose the Bottom value, since Bottom must be positive.
							|| //The Up value is non-negative!
							(rayDistanceMultiplicatorBot >= 0 //And the Bottom value is non-negative (if it would be negative, choose Up value.
									&& //Now both values are non-negative.
									rayDistanceMultiplicatorBot < rayDistanceMultiplicatorUp //Choose Bottom if it is smaller.
							))
					{
						rayDistanceMultiplicator = rayDistanceMultiplicatorBot;
					}
					else
					{
						rayDistanceMultiplicator = rayDistanceMultiplicatorUp;
					}
					
					Vector3 cameraToCollisionVector = cameraRayBoardSpace.multiply(rayDistanceMultiplicator);
					double cameraToCollisionVectorLength = cameraToCollisionVector.length();
					
					//Abort if one is not looking at the plane, but on the placement parent:
					if(hitpoint.getHitPart() != boardDrawStartingPoint.getHitPart() || hitpoint.getDistance() >= cameraToCollisionVectorLength)
					{
						double unitRayLength = cameraRayBoardSpace.length();
						if(cameraToCollisionVectorLength - unitRayLength > 20)
						{
							//TBI: Is this okay?
							//Limits the length of the camera to collision vector to 20.
							cameraToCollisionVector = cameraToCollisionVector.multiply(1.0 / cameraToCollisionVector.length() * 20);
						}
						Vector3 collisionPoint = cameraPositionBoardSpace.add(cameraToCollisionVector);
						double collisionX = collisionPoint.getX();
						double collisionZ = collisionPoint.getZ();
						
						//Y should be at 0 or very close to it - x and z can be used as are.
						x = (int) ((Math.abs(collisionX) + 0.15f) / 0.3f) + 1;
						z = (int) ((Math.abs(collisionZ) + 0.15f) / 0.3f) + 1;
						Vector3 roundedCollisionPoint = new Vector3((x - 1) * 0.15 * (collisionX >= 0 ? 1f : -1f), 0, (z - 1) * 0.15 * (collisionZ >= 0 ? 1f : -1f));
						position = position.add(alignment.inverse().multiply(roundedCollisionPoint));
					}
				}
			}
			boardDrawStartingPoint.setBoardData(position, x, z);
		}
		else if(wireStartPoint != null)
		{
			//Drawing a wire:
			Vector3 toPos = null;
			if(hitpoint.canBePlacedOn())
			{
				HitpointContainer hitpointContainer = (HitpointContainer) hitpoint;
				toPos = hitpointContainer.getPosition();
				//Move placement position to the surface (it is always below it):
				toPos = toPos.add(hitpointContainer.getNormal().multiply(0.075));
			}
			else if(!hitpoint.isEmpty())
			{
				Part lookingAt = hitpoint.getHitPart();
				if(lookingAt instanceof Connector)
				{
					toPos = ((Connector) lookingAt).getConnectionPoint();
				}
			}
			
			if(toPos != null && wireStartPoint != hitpoint.getHitPart())
			{
				//Draw wire between placementPosition and startingPos:
				Vector3 startingPos = wireStartPoint.getConnectionPoint();
				Vector3 direction = toPos.subtract(startingPos).divide(2);
				double distance = direction.length();
				Quaternion alignment = MathHelper.rotationFromVectors(Vector3.zp, direction.normalize());
				if(Double.isNaN(alignment.getA()))
				{
					System.out.println("[WARNING] Cannot place wire, cause start- and end-point are the same... Please try to not abuse OpenTUNG. Ignore stacktrace above and do not report it.");
					hitpoint.setWireData(null, null, 0);
				}
				else
				{
					Vector3 position = startingPos.add(direction);
					hitpoint.setWireData(alignment, position, distance);
				}
			}
			else
			{
				hitpoint.setWireData(null, null, 0);
			}
		}
		
		this.currentPlaceable = currentPlaceable;
		this.hitpoint = skipHitpoint ? new Hitpoint() : hitpoint;
	}
	
	private double toRoughRotation()
	{
		double rotation = placementRotation;
		double remains = rotation % 90;
		return rotation - remains;
	}
	
	private Hitpoint calculateHitpoint()
	{
		CPURaycast.RaycastResult raycastResult = cpuRaycast.cpuRaycast(camera, board.getRootBoard(), wireStartPoint != null, wireRayCaster);
		Part lookingAt = raycastResult.getPart();
		if(lookingAt == null)
		{
			return new Hitpoint();
		}
		
		if(lookingAt instanceof CompContainer)
		{
			if(lookingAt instanceof CompBoard)
			{
				CPURaycast.CollisionResult result = CPURaycast.collisionPoint((CompBoard) lookingAt, camera);
				return new HitpointBoard(lookingAt, raycastResult.getDistance(), result.getLocalNormal(), result.getCollisionPointBoardSpace());
			}
			else
			{
				return new HitpointContainer(lookingAt, raycastResult.getDistance());
			}
		}
		else
		{
			return new Hitpoint(lookingAt, raycastResult.getDistance());
		}
	}
	
	private void drawWireToBePlaced(float[] view)
	{
		Vector3 position = hitpoint.getWireCenterPosition();
		if(position != null)
		{
			Quaternion alignment = hitpoint.getWireAlignment();
			double length = hitpoint.getWireDistance();
			
			//Draw wire:
			Matrix model = new Matrix();
			model.translate((float) position.getX(), (float) position.getY(), (float) position.getZ());
			model.multiply(new Matrix(alignment.createMatrix()));
			Vector3 size = new Vector3(0.025, 0.01, length);
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
	
	private void drawGrabbed(float[] view)
	{
		HitpointContainer hitpointContainer = (HitpointContainer) hitpoint;
		
		Matrix modelMatrix = new Matrix();
		
		//Move the component to the new placement position;
		Vector3 newPosition = hitpointContainer.getPosition();
		Matrix tmpMat = new Matrix();
		tmpMat.translate(
				(float) newPosition.getX(),
				(float) newPosition.getY(),
				(float) newPosition.getZ());
		modelMatrix.multiply(tmpMat);
		
		//Rotate the mesh to new direction/rotation:
		Quaternion newRelativeAlignment = hitpointContainer.getAlignment();
		modelMatrix.multiply(new Matrix(newRelativeAlignment.createMatrix()));
		
		//Move the component back to the world-origin:
		Component grabbedComponent = grabData.getComponent();
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
		List<GrabData.WireContainer> grabbedWires = grabData.getOutgoingWiresWithSides();
		for(GrabData.WireContainer wireContainer : grabbedWires)
		{
			Wire wire = wireContainer.wire;
			Vector3 thisPos = wire.getConnectorA().getConnectionPoint();
			Vector3 thatPos = wire.getConnectorB().getConnectionPoint();
			if(wireContainer.isGrabbedOnASide)
			{
				thisPos = thisPos.subtract(oldPosition);
				thisPos = newRelativeAlignment.inverse().multiply(thisPos);
				thisPos = thisPos.add(newPosition);
			}
			else
			{
				thatPos = thatPos.subtract(oldPosition);
				thatPos = newRelativeAlignment.inverse().multiply(thatPos);
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
		
		if(grabData.hasLabels())
		{
			ShaderProgram sdfShader = shaderStorage.getSdfShader();
			sdfShader.use();
			sdfShader.setUniformM4(1, view);
			for(CompLabel label : grabData.getLabels())
			{
				Vector3 position = label.getPosition();
				position = position.subtract(oldPosition);
				position = newRelativeAlignment.inverse().multiply(position);
				position = position.add(newPosition);
				Quaternion alignment = label.getRotation().multiply(newRelativeAlignment);
				
				m.identity();
				m.translate((float) position.getX(), (float) position.getY(), (float) position.getZ());
				m.multiply(new Matrix(alignment.createMatrix()));
				sdfShader.setUniformM4(2, m.getMat());
				
				label.activate();
				label.getModelHolder().drawTextures();
			}
		}
	}
	
	private void drawBoardToBePlaced(float[] view)
	{
		Quaternion alignment = boardDrawStartingPoint.getAlignment();
		Vector3 position = boardDrawStartingPoint.getBoardCenterPosition();
		int x = boardDrawStartingPoint.getBoardX();
		int z = boardDrawStartingPoint.getBoardZ();
		
		//TBI: Ehh skip the model? (For now yes, the component is very defined in TUNG and LW).
		Matrix matrix = new Matrix();
		//Apply global position:
		matrix.translate((float) position.getX(), (float) position.getY(), (float) position.getZ());
		matrix.multiply(new Matrix(alignment.createMatrix())); //Apply global rotation.
		//The cube is centered, no translation.
		matrix.scale((float) x * 0.15f, 0.075f, (float) z * 0.15f);
		
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
	
	private void drawPlacementPosition(float[] view)
	{
		HitpointContainer hitpointContainer = (HitpointContainer) hitpoint;
		if(currentPlaceable == null)
		{
			ShaderProgram lineShader = shaderStorage.getLineShader();
			//TODO: Switch to line shader with uniform color.
			lineShader.use();
			lineShader.setUniformM4(1, view);
			GL30.glLineWidth(5f);
			Matrix model = new Matrix();
			model.identity();
			Vector3 datPos = hitpointContainer.getPosition();
			model.translate((float) datPos.getX(), (float) datPos.getY(), (float) datPos.getZ());
			lineShader.setUniformM4(2, model.getMat());
			GenericVAO crossyIndicator = shaderStorage.getCrossyIndicator();
			crossyIndicator.use();
			crossyIndicator.draw();
		}
		else if(currentPlaceable == CompBoard.info)
		{
			int x = 1;
			int z = 1;
			//TBI: Ehh skip the model? (For now yes, the component is very defined in TUNG and LW).
			Matrix matrix = new Matrix();
			//Apply global position:
			Vector3 position = hitpointContainer.getPosition();
			matrix.translate((float) position.getX(), (float) position.getY(), (float) position.getZ());
			Quaternion newAlignment = hitpointContainer.getAlignment();
			matrix.multiply(new Matrix(newAlignment.createMatrix())); //Apply global rotation.
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
			ShaderProgram visibleCubeShader = shaderStorage.getVisibleCubeShader();
			GenericVAO visibleCube = shaderStorage.getVisibleOpTexCube();
			World3DHelper.drawModel(visibleCubeShader, visibleCube, currentPlaceable.getModel(), hitpointContainer.getPosition(), hitpointContainer.getAlignment(), view);
		}
	}
	
	private double calculateFixRotationOffset(Quaternion newGlobalAlignment, Hitpoint hitpoint)
	{
		HitpointContainer hitpointContainer = (HitpointContainer) hitpoint;
		FourDirections axes = new FourDirections(hitpoint.isBoard() ? ((HitpointBoard) hitpoint).getLocalNormal() : Vector3.yp, hitpoint.getHitPart().getRotation());
		
		//Get the angle, from the new X axis, to an "optimal" X axis.
		Vector3 newVirtualXAxis = newGlobalAlignment.inverse().multiply(Vector3.xp);
		Vector3 newRandomXAxis = axes.getFitting(fixXAxis);
		boolean diff = newRandomXAxis == null;
		if(diff)
		{
			//All angles are 90° case. Rotate old axis.
			//TBI: Target * inverse(Source) = diff //Will that work, cause rotation?
			Quaternion normalRotation = MathHelper.rotationFromVectors(lastUpNormal, hitpointContainer.getNormal()).inverse();
			//TBI: Rotation may be 180° in that case its pretty much random, but reliably in most cases.
			newRandomXAxis = normalRotation.multiply(fixXAxis);
			Vector3 fallbackAxis = newRandomXAxis;
			newRandomXAxis = axes.getFitting(newRandomXAxis); //Replace with one of the 4, axes, although only minor change.
			if(newRandomXAxis == null)
			{
				//Still fails, could be a 45° angle. For that rotate the last fixXAxis and get an axis for that.
				normalRotation = Quaternion.angleAxis(2, hitpointContainer.getNormal());
				Vector3 slightlyRotated = normalRotation.multiply(fixXAxis);
				newRandomXAxis = axes.getFitting(slightlyRotated);
				if(newRandomXAxis == null)
				{
					System.out.println(Ansi.red + "[ERROR] ROTATION CODE FAILED!" + Ansi.r
							+ "\n Placement-Vector: " + hitpointContainer.getNormal()
							+ "\n LastNormal: " + lastUpNormal
							+ "\n RotationResult: " + fallbackAxis);
					//ChooseAnyAlternative:
					newRandomXAxis = axes.getA();
				}
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
		boolean grabBoardHighlight = false;
		if(grabData != null)
		{
			grabBoardHighlight = (grabData.getComponent() instanceof CompBoard) && hitpoint.canBePlacedOn();
			if(!grabBoardHighlight)
			{
				return;
			}
		}
		else if(hitpoint.isEmpty())
		{
			return;
		}
		else
		{
			Part part = hitpoint.getHitPart();
			
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
		}
		
		Part part = hitpoint.getHitPart();
		
		//Enable drawing to stencil buffer
		GL30.glStencilMask(0xFF);
		
		ShaderProgram invisibleCubeShader = shaderStorage.getInvisibleCubeShader();
		GenericVAO invisibleCube = shaderStorage.getInvisibleCube();
		if(grabBoardHighlight)
		{
			//Do very very ugly drawing of board:
			Component grabbedComponent = grabData.getComponent();
			Meshable meshable = grabbedComponent.getModelHolder().getSolid().get(0); //Grabbed board or mount, either way -> solid
			
			HitpointContainer hitpointContainer = (HitpointContainer) hitpoint;
			Vector3 position = hitpointContainer.getPosition();
			//Construct absolute rotation again...
			Quaternion rotation = grabbedComponent.getRotation().multiply(hitpointContainer.getAlignment());
			
			invisibleCubeShader.use();
			invisibleCubeShader.setUniformM4(1, view);
			invisibleCubeShader.setUniformV4(3, new float[]{0, 0, 0, 0});
			World3DHelper.drawCubeFull(invisibleCubeShader, invisibleCube, (CubeFull) meshable, position, grabbedComponent, rotation, grabbedComponent.getModelHolder().getPlacementOffset(), new Matrix());
		}
		else if(part instanceof Component)
		{
			World3DHelper.drawStencilComponent(invisibleCubeShader, invisibleCube, (Component) part, view);
		}
		else //Connector
		{
			invisibleCubeShader.use();
			invisibleCubeShader.setUniformM4(1, view);
			invisibleCubeShader.setUniformV4(3, new float[]{0, 0, 0, 0});
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
	
	private void drawBoardResize(float[] view)
	{
		CompBoard board = resizeData.getBoard();
		
		//Draw board:
		{
			Vector3 position = resizeData.getPosition();
			int x = resizeData.getBoardX();
			int z = resizeData.getBoardZ();
			
			//TBI: Ehh skip the model? (For now yes, the component is very defined in TUNG and LW).
			Matrix matrix = new Matrix();
			//Apply global position:
			matrix.translate((float) position.getX(), (float) position.getY(), (float) position.getZ());
			matrix.multiply(new Matrix(board.getRotation().createMatrix())); //Apply global rotation.
			//The cube is centered, no translation.
			matrix.scale((float) x * 0.15f, 0.075f, (float) z * 0.15f);
			
			//Draw the board:
			shaderStorage.getBoardTexture().activate();
			ShaderProgram textureCubeShader = shaderStorage.getTextureCubeShader();
			textureCubeShader.use();
			textureCubeShader.setUniformM4(1, view);
			textureCubeShader.setUniformM4(2, matrix.getMat());
			textureCubeShader.setUniformV2(3, new float[]{x, z});
			textureCubeShader.setUniformV4(4, board.getColor().asArray());
			GenericVAO textureCube = shaderStorage.getVisibleOpTexCube();
			textureCube.use();
			textureCube.draw();
		}
		
		//Draw outer drag-surfaces:
		ShaderProgram resizeShader = shaderStorage.getResizeShader();
		GenericVAO vao = shaderStorage.getResizeSurface();
		GenericVAO vaoBorder = shaderStorage.getResizeBorder();
		
		resizeShader.use();
		resizeShader.setUniformM4(1, view);
		
		Matrix parentRotation = new Matrix(board.getRotation().createMatrix());
		
		Matrix modelMatrix = new Matrix();
		modelMatrix.translate(
				(float) resizeData.getPosition().getX(),
				(float) resizeData.getPosition().getY(),
				(float) resizeData.getPosition().getZ()
		);
		modelMatrix.multiply(parentRotation);
		
		GL30.glDisable(GL30.GL_CULL_FACE);
		GL30.glDepthFunc(GL30.GL_ALWAYS);
		
		int x = resizeData.getBoardX();
		int z = resizeData.getBoardZ();
		
		//PosX
		Matrix copyMatrix = modelMatrix.copy();
		copyMatrix.translate(0, 0, z * 0.15f + 0.3f);
		copyMatrix.scale(x * 0.30f, 1, 0.6f);
		resizeShader.setUniformM4(2, copyMatrix.getMat());
		resizeShader.setUniformV4(3, new float[] {1.0f, 1.0f, 0.0f, 0.4f});
		vao.use();
		vao.draw();
		resizeShader.setUniformV4(3, new float[] {1.0f, 1.0f, 0.0f, 1.0f});
		vaoBorder.use();
		vaoBorder.draw();
		//PosZ
		copyMatrix = modelMatrix.copy();
		copyMatrix.translate(x * 0.15f + 0.3f, 0, 0);
		copyMatrix.scale(0.6f, 1, z * 0.30f);
		resizeShader.setUniformM4(2, copyMatrix.getMat());
		resizeShader.setUniformV4(3, new float[] {1.0f, 1.0f, 0.0f, 0.4f});
		vao.use();
		vao.draw();
		resizeShader.setUniformV4(3, new float[] {1.0f, 1.0f, 0.0f, 1.0f});
		vaoBorder.use();
		vaoBorder.draw();
		//NegX
		copyMatrix = modelMatrix.copy();
		copyMatrix.translate(0, 0, -z * 0.15f - 0.3f);
		copyMatrix.scale(x * 0.30f, 1, 0.6f);
		resizeShader.setUniformM4(2, copyMatrix.getMat());
		resizeShader.setUniformV4(3, new float[] {1.0f, 1.0f, 0.0f, 0.4f});
		vao.use();
		vao.draw();
		resizeShader.setUniformV4(3, new float[] {1.0f, 1.0f, 0.0f, 1.0f});
		vaoBorder.use();
		vaoBorder.draw();
		//NegZ
		copyMatrix = modelMatrix.copy();
		copyMatrix.translate(-x * 0.15f - 0.3f, 0, 0);
		copyMatrix.scale(0.6f, 1, z * 0.30f);
		resizeShader.setUniformM4(2, copyMatrix.getMat());
		resizeShader.setUniformV4(3, new float[] {1.0f, 1.0f, 0.0f, 0.4f});
		vao.use();
		vao.draw();
		resizeShader.setUniformV4(3, new float[] {1.0f, 1.0f, 0.0f, 1.0f});
		vaoBorder.use();
		vaoBorder.draw();
		
		GL30.glEnable(GL30.GL_CULL_FACE);
		GL30.glDepthFunc(GL30.GL_LESS);
	}
	
	@Override
	public void newSize(int width, int height)
	{
	}
	
	public Camera getCamera()
	{
		return camera;
	}
}

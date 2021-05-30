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
import de.ecconia.java.opentung.core.data.Hitpoint;
import de.ecconia.java.opentung.core.data.HitpointBoard;
import de.ecconia.java.opentung.core.data.HitpointContainer;
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
import de.ecconia.java.opentung.simulation.Updateable;
import de.ecconia.java.opentung.simulation.Wire;
import de.ecconia.java.opentung.units.IconGeneration;
import de.ecconia.java.opentung.units.LabelToolkit;
import de.ecconia.java.opentung.util.Ansi;
import de.ecconia.java.opentung.util.FourDirections;
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
	
	public boolean isGrabbingBoard()
	{
		return (grabData != null) && (grabData.getComponent() instanceof CompBoard);
	}
	
	public GrabData getGrabData()
	{
		return grabData;
	}
	
	public boolean isPlacingBoard(boolean finePlacement)
	{
		GrabData grabData = this.grabData;
		return (grabData != null && grabData.getComponent() instanceof CompBoard) || (finePlacement && boardDrawStartingPoint == null && currentPlaceable == CompBoard.info);
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
		if(hitpoint.canBePlacedOn() && currentPlaceable == CompBoard.info)
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
			if(currentPlaceable != CompBoard.info)
			{
				System.out.println("ERROR: Called normal placement code without board selected, while boardDrawStartingPoint was not null.");
				return true;
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
			for(GrabData.WireContainer wireContainer : grabData.getWiresWithSides())
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
			if(grabData instanceof GrabContainerData)
			{
				GrabContainerData grabContainerData = (GrabContainerData) grabData;
				for(CompSnappingWire wire : grabContainerData.getInternalSnappingWires())
				{
					alignComponent(wire, oldPosition, newPosition, deltaAlignment);
				}
				for(CompWireRaw wire : grabContainerData.getInternalWires())
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
				for(Wire wire : grabData.getWires())
				{
					CompWireRaw cWire = (CompWireRaw) wire;
					worldMesh.addComponent(cWire, board.getSimulation());
					wireRayCaster.addWire(cWire);
				}
				if(grabData instanceof GrabContainerData)
				{
					GrabContainerData grabContainerData = (GrabContainerData) grabData;
					for(CompSnappingWire wire : grabContainerData.getInternalSnappingWires())
					{
						secondaryMesh.removeComponent(wire, board.getSimulation());
						worldMesh.addComponent(wire, board.getSimulation());
					}
					for(CompWireRaw wire : grabContainerData.getInternalWires())
					{
						secondaryMesh.removeComponent(wire, board.getSimulation());
						wireRayCaster.addWire(wire);
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
			newComponent.init(); //Inits components such as the ThroughPeg (needs to be called after position is set).
			
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
			CompContainer container = (CompContainer) toBeGrabbed;
			gpuTasks.add((unused) -> {
				//Remove this board from its parent.
				parent.remove(toBeGrabbed);
				GrabContainerData newGrabData = new GrabContainerData(parent, toBeGrabbed);
				
				List<CompSnappingWire> internalSnappingWires = new ArrayList<>();
				HashSet<CompSnappingPeg> unconnectedSnappingPegs = new HashSet<>();
				List<CompWireRaw> internalWires = new ArrayList<>();
				Map<Wire, Boolean> outgoingWires = new HashMap<>();
				{
					LinkedList<Component> queue = new LinkedList<>();
					queue.addLast(container);
					while(!queue.isEmpty())
					{
						Component component = queue.removeFirst();
						newGrabData.addComponent(component);
						worldMesh.removeComponent(component, board.getSimulation());
						secondaryMesh.addComponent(component, board.getSimulation());
						
						if(component instanceof CompLabel)
						{
							board.getLabelsToRender().remove(component);
							newGrabData.addLabel((CompLabel) component);
						}
						else if(component instanceof CompSnappingPeg)
						{
							unconnectedSnappingPegs.add((CompSnappingPeg) component);
						}
						
						if(component instanceof ConnectedComponent)
						{
							for(Connector connector : ((ConnectedComponent) component).getConnectors())
							{
								for(Wire wire : connector.getWires())
								{
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
											throw new RuntimeException("Unexpected wire type received: " + wire.getClass().getSimpleName());
										}
									}
									else
									{
										outgoingWires.put(wire, wire.getConnectorA() == connector);
									}
								}
							}
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
				
				for(CompWireRaw wire : internalWires)
				{
					worldMesh.removeComponent(wire, board.getSimulation());
					wireRayCaster.removeWire(wire);
					secondaryMesh.addComponent(wire, board.getSimulation());
				}
				newGrabData.setInternalWires(internalWires);
				for(CompSnappingWire wire : internalSnappingWires)
				{
					unconnectedSnappingPegs.remove(wire.getConnectorA().getParent());
					unconnectedSnappingPegs.remove(wire.getConnectorB().getParent());
					worldMesh.removeComponent(wire, board.getSimulation());
					secondaryMesh.addComponent(wire, board.getSimulation());
				}
				newGrabData.setUnconnectedSnappingPegs(unconnectedSnappingPegs);
				newGrabData.setInternalSnappingWires(internalSnappingWires);
				List<CompSnappingWire> snappingWiresToRemove = new ArrayList<>();
				for(Map.Entry<Wire, Boolean> outgoingWireEntry : outgoingWires.entrySet())
				{
					Wire outgoingWire = outgoingWireEntry.getKey();
					worldMesh.removeComponent((Component) outgoingWire, board.getSimulation());
					if(outgoingWire instanceof CompSnappingWire)
					{
						snappingWiresToRemove.add((CompSnappingWire) outgoingWire);
					}
					else
					{
						//Snapping peg wires should not and technically can't be in the wire ray-caster...
						wireRayCaster.removeWire((CompWireRaw) outgoingWire);
						newGrabData.addWire(outgoingWire, outgoingWireEntry.getValue());
					}
				}
				
				fixXAxis = container.getRotation().inverse().multiply(Vector3.xp);
				lastUpNormal = toBeGrabbed.getRotation().inverse().multiply(Vector3.yp);
				grabRotation = 0;
				//Activate grabbing:
				if(container instanceof CompBoard)
				{
					CompBoard board = (CompBoard) container;
					this.xBoardOffset = ((board.getX() & 1) == 0) ? -0.15 : 0;
					this.zBoardOffset = ((board.getZ() & 1) == 0) ? -0.15 : 0;
				}
				else //Mount:
				{
					this.xBoardOffset = 0;
					this.zBoardOffset = 0;
				}
				this.grabData = newGrabData;
				this.fineBoardOffset = 0;
				
				board.getSimulation().updateJobNextTickThreadSafe((simulation) -> {
					Map<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> updates = new HashMap<>();
					for(CompSnappingWire wire : snappingWiresToRemove)
					{
						CompSnappingPeg aSide = (CompSnappingPeg) wire.getConnectorA().getParent();
						CompSnappingPeg bSide = (CompSnappingPeg) wire.getConnectorB().getParent();
						aSide.setPartner(null);
						bSide.setPartner(null);
						ClusterHelper.removeWire(simulation, wire, updates);
					}
					gpuTasks.add((unused2) -> {
						System.out.println("[ClusterUpdateDebug] Updating " + updates.size() + " conductor mesh bags.");
						for(Map.Entry<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> entry : updates.entrySet())
						{
							entry.getKey().handleUpdates(entry.getValue(), board.getSimulation());
						}
					});
				});
			});
			return;
		}
		
		GrabData newGrabData = new GrabData(parent, toBeGrabbed);
		
		//Remove the snapping wire fully.
		if(toBeGrabbed instanceof CompSnappingPeg)
		{
			CompSnappingPeg snappingPeg = (CompSnappingPeg) toBeGrabbed;
			if(snappingPeg.hasPartner())
			{
				for(Wire wire : ((ConnectedComponent) toBeGrabbed).getPegs().get(0).getWires())
				{
					if(wire instanceof CompSnappingWire)
					{
						board.getSimulation().updateJobNextTickThreadSafe((simulation) -> {
							Map<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> updates = new HashMap<>();
							ClusterHelper.removeWire(simulation, wire, updates);
							snappingPeg.getPartner().setPartner(null);
							snappingPeg.setPartner(null);
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
		}
		
		board.getSimulation().updateJobNextTickThreadSafe((unused) -> {
			//Collect wires:
			newGrabData.addComponent(toBeGrabbed); //Must be done manually
			if(toBeGrabbed instanceof CompLabel)
			{
				newGrabData.addLabel((CompLabel) toBeGrabbed);
			}
			if(toBeGrabbed instanceof ConnectedComponent)
			{
				for(Connector connector : ((ConnectedComponent) toBeGrabbed).getConnectors())
				{
					for(Wire wire : connector.getWires())
					{
						//Skip HiddenWires, snapping wires have already been removed in a previous simulation task.
						if(wire instanceof CompWireRaw)
						{
							newGrabData.addWire(wire, wire.getConnectorA() == connector);
						}
					}
				}
			}
			
			gpuTasks.add((unused2) -> {
				if(toBeGrabbed instanceof ConnectedComponent)
				{
					for(Connector connector : ((ConnectedComponent) toBeGrabbed).getConnectors())
					{
						clusterHighlighter.clusterChanged(connector.getCluster());
					}
				}
				if(toBeGrabbed instanceof CompLabel)
				{
					//Remove regardless of it having texture or not.
					board.getLabelsToRender().remove(toBeGrabbed);
				}
				//Remove from meshes on render thread
				worldMesh.removeComponent(toBeGrabbed, board.getSimulation());
				for(Wire wire : newGrabData.getWires())
				{
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
				this.xBoardOffset = 0;
				this.zBoardOffset = 0;
				this.fineBoardOffset = 0;
				grabData = newGrabData;
			});
		});
	}
	
	public void deleteGrabbed()
	{
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
			for(Wire wire : grabDataCopy.getWires())
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
				for(Wire wire : grabDataCopy.getWires())
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
			
			for(Wire wire : grabData.getWires())
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
						rotation = toRoughRotation();
					}
					else if(currentPlaceable == CompMount.info)
					{
					
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
						if(position == null)
						{
							hitpoint = new Hitpoint(hitpoint.getHitPart()); //Prevent the component from being drawn, by just changing the hitpoint type. [pretend non-container]
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
							hitpoint = new Hitpoint(hitpoint.getHitPart()); //Prevent the component from being drawn, by just changing the hitpoint type. [pretend non-container]
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
				position = position.add(alignment.inverse().multiply(roundedCollisionPoint));
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
		this.hitpoint = hitpoint;
	}
	
	private double toRoughRotation()
	{
		double rotation = placementRotation;
		double remains = rotation % 90;
		return rotation - remains;
	}
	
	private Hitpoint calculateHitpoint()
	{
		Part lookingAt = cpuRaycast.cpuRaycast(camera, board.getRootBoard(), wireStartPoint != null, wireRayCaster);
		if(lookingAt == null)
		{
			return new Hitpoint(null);
		}
		
		if(lookingAt instanceof CompContainer)
		{
			if(lookingAt instanceof CompBoard)
			{
				CPURaycast.CollisionResult result = CPURaycast.collisionPoint((CompBoard) lookingAt, camera);
				return new HitpointBoard(lookingAt, result.getLocalNormal(), result.getCollisionPointBoardSpace());
			}
			else
			{
				return new HitpointContainer(lookingAt);
			}
		}
		else
		{
			return new Hitpoint(lookingAt);
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
		List<GrabData.WireContainer> grabbedWires = grabData.getWiresWithSides();
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
	
	@Override
	public void newSize(int width, int height)
	{
	}
	
	public Camera getCamera()
	{
		return camera;
	}
}

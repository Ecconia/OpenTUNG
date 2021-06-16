package de.ecconia.java.opentung.core;

import de.ecconia.java.opentung.OpenTUNG;
import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.CompLabel;
import de.ecconia.java.opentung.components.CompMount;
import de.ecconia.java.opentung.components.CompPanelLabel;
import de.ecconia.java.opentung.components.CompSnappingPeg;
import de.ecconia.java.opentung.components.CompSnappingWire;
import de.ecconia.java.opentung.components.conductor.CompWireRaw;
import de.ecconia.java.opentung.components.conductor.Connector;
import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.Holdable;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.components.meta.Part;
import de.ecconia.java.opentung.components.meta.PlaceableInfo;
import de.ecconia.java.opentung.core.data.Hitpoint;
import de.ecconia.java.opentung.core.data.HitpointBoard;
import de.ecconia.java.opentung.core.data.HitpointContainer;
import de.ecconia.java.opentung.core.data.ShaderStorage;
import de.ecconia.java.opentung.core.data.SharedData;
import de.ecconia.java.opentung.core.helper.OnBoardPlacementHelper;
import de.ecconia.java.opentung.core.helper.World3DHelper;
import de.ecconia.java.opentung.core.structs.GPUTask;
import de.ecconia.java.opentung.core.structs.RenderPlane;
import de.ecconia.java.opentung.core.systems.CPURaycast;
import de.ecconia.java.opentung.core.systems.ClusterHighlighter;
import de.ecconia.java.opentung.core.systems.Skybox;
import de.ecconia.java.opentung.core.tools.Delete;
import de.ecconia.java.opentung.core.tools.DrawBoard;
import de.ecconia.java.opentung.core.tools.DrawWire;
import de.ecconia.java.opentung.core.tools.GrabCopy;
import de.ecconia.java.opentung.core.tools.Resize;
import de.ecconia.java.opentung.core.tools.Tool;
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
import de.ecconia.java.opentung.simulation.Updateable;
import de.ecconia.java.opentung.units.IconGeneration;
import de.ecconia.java.opentung.units.LabelToolkit;
import de.ecconia.java.opentung.util.Ansi;
import de.ecconia.java.opentung.util.FourDirections;
import de.ecconia.java.opentung.util.math.MathHelper;
import de.ecconia.java.opentung.util.math.Quaternion;
import de.ecconia.java.opentung.util.math.Vector3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
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
	private int gpuTasksCurrentSize;
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
	
	public void prepareSaving()
	{
		hitpoint = new Hitpoint();
		if(primaryTool != null)
		{
			//Actually only grabbing has to be aborted (for now), but lets do it in a generic way.
			primaryTool.abort();
		}
	}
	
	//Other:
	
	private Hitpoint hitpoint = new Hitpoint(); //What the camera is currently looking at.
	private PlaceableInfo currentPlaceable = null; //Backup variable, to keep the value constant while render-cycle.
	
	//STATES (And their data):
	//Is not ready yet:
	private boolean fullyLoaded; //Unset, if loading not yet done. Prevents initial interactions.
	//Is in normal placement mode:
	private boolean placeableBoardIsLaying = true;
	private double placementRotation = 0;
	
	//Board placement offset:
	private double fineBoardOffset;
	
	//Rotation fix variables (Pretty much used everywhere):
	private Vector3 fixXAxis = Vector3.xp; //Never NPE, use +X as default.
	private Vector3 lastUpNormal = Vector3.yp; //Used whenever an item is shadow drawn, to use more natural rotations.
	
	//### Tool code: ###################################
	
	private final List<Tool> tools = new ArrayList<>();
	
	private void setupTools()
	{
		tools.add(new Resize(sharedData));
		tools.add(new GrabCopy(sharedData));
		tools.add(new Delete(sharedData));
		tools.add(new DrawWire(sharedData));
		tools.add(new DrawBoard(sharedData));
	}
	
	//Primary tool:
	private Tool primaryToolReserve; //This variable is to be written to by the input thread.
	private boolean acceptInputs; //Set to false on the input thread, as soon as the tool intends to stop.
	
	public boolean checkToolActivation(int scancode, boolean control)
	{
		if(primaryToolReserve != null)
		{
			return false;
		}
		
		//Primary tool is not active when calling this method.
		Hitpoint hitpoint = this.hitpoint;
		for(Tool tool : tools)
		{
			Boolean res = tool.activateKeyUp(hitpoint, scancode, control);
			if(res != null)
			{
				if(res)
				{
					primaryToolReserve = tool; //Reserve this tool.
				}
				return true;
			}
		}
		return false;
	}
	
	public boolean checkToolActivationMouseDown(int buttonCode, boolean control)
	{
		if(primaryToolReserve != null)
		{
			return false;
		}
		
		//Primary tool is not active when calling this method.
		Hitpoint hitpoint = this.hitpoint;
		for(Tool tool : tools)
		{
			Boolean res = tool.activateMouseDown(hitpoint, buttonCode, control);
			if(res != null)
			{
				if(res)
				{
					primaryToolReserve = tool; //Reserve this tool.
				}
				return true;
			}
		}
		return false;
	}
	
	private Tool primaryTool; //This variable may only be modified by the render thread.
	
	//Tool own state management:
	
	public void toolReady()
	{
		acceptInputs = true;
		primaryTool = primaryToolReserve;
	}
	
	public void toolStopInputs()
	{
		acceptInputs = false;
	}
	
	public void toolDisable()
	{
		//Must be called by the tool itself.
		primaryTool = null;
		acceptInputs = false; //For safety reasons, call here too.
		primaryToolReserve = null; //Also allow the input thread to set a new tool.
	}
	
	//Input thread access:
	
	public boolean toolAbort()
	{
		if(!acceptInputs)
		{
			return false;
		}
		Tool primaryTool = this.primaryTool;
		if(primaryTool == null)
		{
			return false;
		}
		return primaryTool.abort();
	}
	
	public boolean toolKeyUp(int scancode, boolean control)
	{
		if(!acceptInputs)
		{
			return false;
		}
		Tool primaryTool = this.primaryTool;
		if(primaryTool == null)
		{
			return false;
		}
		return primaryTool.keyUp(scancode, control);
	}
	
	public boolean toolScroll(int val, boolean control, boolean alt)
	{
		if(!acceptInputs)
		{
			return false;
		}
		Tool primaryTool = this.primaryTool;
		if(primaryTool == null)
		{
			return false;
		}
		return primaryTool.scroll(val, control, alt);
	}
	
	public boolean toolMouseLeftUp()
	{
		if(!acceptInputs)
		{
			return false;
		}
		Tool primaryTool = this.primaryTool;
		if(primaryTool == null)
		{
			return false;
		}
		return primaryTool.mouseLeftUp();
	}
	
	public boolean toolMouseRightUp()
	{
		if(!acceptInputs)
		{
			return false;
		}
		Tool primaryTool = this.primaryTool;
		if(primaryTool == null)
		{
			return false;
		}
		return primaryTool.mouseRightUp();
	}
	
	//### Input handling: #################################
	
	private Controller3D controller;
	
	public Part getCursorObject()
	{
		Hitpoint hitpoint = this.hitpoint;
		return hitpoint != null ? hitpoint.getHitPart() : null;
	}
	
	public SharedData getSharedData()
	{
		return sharedData;
	}
	
	public int getGpuTasksCurrentSize()
	{
		return gpuTasksCurrentSize;
	}
	
	public boolean isInBoardPlacementMode()
	{
		return primaryToolReserve == null //Do not flip board while currently grabbing.
				&& sharedData.getCurrentPlaceable() == CompBoard.info //Only flip a board, when actually holding a board.
				&& hitpoint.canBePlacedOn(); //We might want to interact with a component. So only flip when placing is possible.
	}
	
	public boolean isDraggingOrPrimaryToolActive()
	{
		return primaryToolReserve != null;
	}
	
	public boolean allowBoardOffset(boolean finePlacement)
	{
		//TBI: Maybe also disallow it, while movement keys WASD are pressed.
		Hitpoint hitpoint = this.hitpoint;
		if(finePlacement //Must only allow fine-offset.
				&& primaryToolReserve != null //Must not do anything else. These handle board offset themself.
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
	
	public void boardOffset(int amount, boolean control)
	{
		gpuTasks.add((unused) -> {
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
	
	public void rotatePlacement(boolean control)
	{
		gpuTasks.add((unused) -> {
			if(primaryTool != null)
			{
				return;
			}
			double degrees = control ? 22.5 : 90;
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
		});
	}
	
	//TODO: Re-add.
	public void stopClusterHighlighting()
	{
		clusterHighlighter.stop();
	}
	
	public boolean attemptPlacement(boolean abortPlacement)
	{
		//TODO: All these values are not fully thread-safe, either wrap in one object, or make thread-safe.
		Hitpoint hitpoint = this.hitpoint;
		PlaceableInfo currentPlaceable = this.currentPlaceable;
		
		if(!fullyLoaded)
		{
			return false;
		}
		
		//Do not do this check any earlier, cause for whatever reason we might be dragging a board or such.
		if(primaryTool != null) //Just make sure that we are not dragging, if we are then something went wrong and placement should be allowed.
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
		
		if(currentPlaceable != null)
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
	public void snapSnappingPeg(CompSnappingPeg snappingPegA)
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
		setupTools();
		
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
		gpuTasksCurrentSize = gpuTasks.size();
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
			
			//Draw world:
			drawDynamic(view);
			//Draw primary tool in world:
			if(primaryTool != null)
			{
				primaryTool.renderWorld(view);
			}
			else if(hitpoint.canBePlacedOn())
			{
				drawPlacementPosition(view);
			}
			
			//Draw secondary overlays:
			clusterHighlighter.highlightCluster(view);
			if(primaryTool == null)
			{
				//Only highlight, when not having primary tool active.
				drawHighlight(view);
			}
			
			//Draw primary tool overlay:
			if(primaryTool != null)
			{
				primaryTool.renderOverlay(view);
			}
			
			//Draw old things:
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
			
			//Draw the skybox as last step:
			//TODO: This should be done before the overlay.
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
		}
		
		if(primaryTool != null)
		{
			hitpoint = primaryTool.adjustHitpoint(hitpoint);
		}
		else
		{
			//Always calculate the hitpoint, if things could be placed on, regardless of used. It always has to be ready to be used in the next cycle and between.
			if(hitpoint.canBePlacedOn())
			{
				//TBI: When drawing, the hitpoint is inaccurate, cause it might consider the wrong target position (when control).
				HitpointContainer hitpointContainer = (HitpointContainer) hitpoint;
				
				//Normal placement (drawPlacementPosition)
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
		//TODO: Add wire-cast flag. Accessible by the tools.
		CPURaycast.RaycastResult raycastResult = cpuRaycast.cpuRaycast(camera, board.getRootBoard(), primaryTool != null && primaryTool.getClass() == DrawWire.class, wireRayCaster);
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
	
	public double calculateFixRotationOffset(Quaternion newGlobalAlignment, Hitpoint hitpoint)
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
		if(hitpoint.isEmpty())
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
		
		float[] color = new float[]{
				Settings.highlightColorR,
				Settings.highlightColorG,
				Settings.highlightColorB,
				Settings.highlightColorA
		};
		
		Part part = hitpoint.getHitPart();
		
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
			World3DHelper.drawCubeFull(invisibleCubeShader, invisibleCube, ((Connector) part).getModel(), part, part.getParent().getModelHolder().getPlacementOffset(), new Matrix());
		}
		
		//Draw on top
		GL30.glDisable(GL30.GL_DEPTH_TEST);
		//Only draw if stencil bit is set.
		GL30.glStencilFunc(GL30.GL_EQUAL, 1, 0xFF);
		
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
	
	public MeshBagContainer getWorldMesh()
	{
		return worldMesh;
	}
	
	public Controller3D getController()
	{
		return controller;
	}
	
	public MeshBagContainer getSecondaryMesh()
	{
		return secondaryMesh;
	}
	
	public WireRayCaster getWireRayCaster()
	{
		return wireRayCaster;
	}
	
	public void resetFixPos(Vector3 x, Vector3 y)
	{
		this.fixXAxis = x;
		this.lastUpNormal = y;
	}
	
	public void clusterChanged(Cluster cluster)
	{
		clusterHighlighter.clusterChanged(cluster);
	}
	
	public void resetFineBoardOffset()
	{
		this.fineBoardOffset = 0;
	}
}

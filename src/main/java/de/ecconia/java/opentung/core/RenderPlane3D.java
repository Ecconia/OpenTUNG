package de.ecconia.java.opentung.core;

import de.ecconia.java.opentung.OpenTUNG;
import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.CompLabel;
import de.ecconia.java.opentung.components.CompPanelLabel;
import de.ecconia.java.opentung.components.CompSnappingPeg;
import de.ecconia.java.opentung.components.CompSnappingWire;
import de.ecconia.java.opentung.components.conductor.CompWireRaw;
import de.ecconia.java.opentung.components.conductor.Connector;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Holdable;
import de.ecconia.java.opentung.components.meta.Part;
import de.ecconia.java.opentung.core.data.Hitpoint;
import de.ecconia.java.opentung.core.data.HitpointBoard;
import de.ecconia.java.opentung.core.data.HitpointContainer;
import de.ecconia.java.opentung.core.data.ShaderStorage;
import de.ecconia.java.opentung.core.data.SharedData;
import de.ecconia.java.opentung.core.structs.GPUTask;
import de.ecconia.java.opentung.core.structs.RenderPlane;
import de.ecconia.java.opentung.core.systems.CPURaycast;
import de.ecconia.java.opentung.core.systems.ClusterHighlighter;
import de.ecconia.java.opentung.core.systems.Skybox;
import de.ecconia.java.opentung.core.tools.Delete;
import de.ecconia.java.opentung.core.tools.DrawBoard;
import de.ecconia.java.opentung.core.tools.DrawWire;
import de.ecconia.java.opentung.core.tools.EditWindow;
import de.ecconia.java.opentung.core.tools.GrabCopy;
import de.ecconia.java.opentung.core.tools.NormalPlacement;
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
import javax.swing.JOptionPane;

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
	
	//STATES (And their data):
	//Is not ready yet:
	private boolean fullyLoaded; //Unset, if loading not yet done. Prevents initial interactions.
	
	//Rotation fix variables (Pretty much used everywhere):
	private Vector3 fixXAxis = Vector3.xp; //Never NPE, use +X as default.
	private Vector3 lastUpNormal = Vector3.yp; //Used whenever an item is shadow drawn, to use more natural rotations.
	
	//### Tool code: ###################################
	
	private final List<Tool> tools = new ArrayList<>();
	private NormalPlacement defaultTool;
	
	private void setupTools()
	{
		primaryTool = defaultTool = new NormalPlacement(sharedData);
		tools.add(new Resize(sharedData));
		tools.add(new GrabCopy(sharedData));
		tools.add(new Delete(sharedData));
		tools.add(new DrawWire(sharedData));
		tools.add(new DrawBoard(sharedData));
		tools.add(new EditWindow(sharedData));
	}
	
	private void toolDebug(String message)
	{
		System.out.println("[ActiveToolDebug] " + message + " (Active: " + (primaryTool == null ? "null" : primaryTool.getClass().getSimpleName()) + " | PreActive: " + (primaryToolReserve == null ? "null" : primaryToolReserve.getClass().getSimpleName()) + ")");
	}
	
	//Primary tool:
	private Tool primaryToolReserve; //This variable is to be written to by the input thread.
	private boolean acceptInputs = true; //Set to false on the input thread, as soon as the tool intends to stop.
	
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
					toolDebug("Pre-Activating tool: " + tool.getClass().getSimpleName());
					acceptInputs = false;
					primaryToolReserve = tool; //Reserve this tool.
					tool.activateNow(hitpoint);
				}
				else
				{
					toolDebug("Passive-Activating tool: " + tool.getClass().getSimpleName());
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
					toolDebug("Pre-Activating tool: " + tool.getClass().getSimpleName());
					acceptInputs = false;
					primaryToolReserve = tool; //Reserve this tool.
					tool.activateNow(hitpoint);
				}
				else
				{
					toolDebug("Passive-Activating tool: " + tool.getClass().getSimpleName());
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
		toolDebug("Activating tool, its ready.");
		acceptInputs = true;
		primaryTool = primaryToolReserve;
	}
	
	public void toolStopInputs()
	{
		toolDebug("Pre-Stop tool.");
		acceptInputs = false;
	}
	
	public void toolDisable()
	{
		toolDebug("Disabling tool.");
		//Must be called by the tool itself.
		primaryTool = defaultTool;
		acceptInputs = true; //For safety reasons, call here too.
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
	
	public boolean toolMouseRightDown()
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
		return primaryTool.mouseRightDown(hitpoint);
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
	
	public boolean isPrimaryToolActive()
	{
		return primaryToolReserve != null;
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
		clusterHighlighter.componentRightClicked(part);
	}
	
	public void stopClusterHighlighting()
	{
		clusterHighlighter.abortHighlighting();
	}
	
	//Must be run from render thread.
	public void snapSnappingPeg(CompSnappingPeg snappingPegA)
	{
		//Raycast and see which component gets hit:
		Vector3 snappingPegAConnectionPoint = snappingPegA.getConnectionPoint();
		Vector3 rayA = snappingPegA.getAlignmentGlobal().inverse().multiply(Vector3.zn);
		RayCastResult result = cpuRaycast.cpuRaycast(snappingPegAConnectionPoint, rayA, board.getRootBoard());
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
					//Do ray-cast from the other side:
					Vector3 snappingPegBConnectionPoint = snappingPegB.getConnectionPoint();
					result = cpuRaycast.cpuRaycast(snappingPegBConnectionPoint, rayB, board.getRootBoard());
					if(result.getMatch() != null && result.getMatch().getParent() == snappingPegA)
					{
						//Angles and ray-cast match, now perform the actual linking:
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
						wire.setPositionGlobal(snappingPegAConnectionPoint.add(direction.divide(2)));
						wire.setAlignmentGlobal(alignment);
						
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
		
		System.out.println("Setting up primary world-mesh... (May take a long time)");
		worldMesh.setup(board, board.getWiresToRender(), board.getSimulation());
		System.out.println("Finished generating world mesh.");
		
		gpuTasks.add(world3D -> {
			IconGeneration.render(shaderStorage);
			//Restore the projection matrix and viewport of this shader, since they got abused.
			shaderStorage.resetViewportAndVisibleCubeShader();
		});
		
		//Do not start receiving events before here. Be sure the whole thing is properly setted up.
		controller = new Controller3D(this);
		setupTools(); //Needs the controller. TBI: How to make this better?
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
	
	private boolean doNotShowPopupAgain = false;
	
	@Override
	public void render()
	{
		//Handle jobs passed from other threads:
		gpuTasksCurrentSize = gpuTasks.size();
		while(!gpuTasks.isEmpty())
		{
			try
			{
				gpuTasks.poll().execute(this);
			}
			catch(Throwable t)
			{
				System.out.println("Failed to run job on render thread. Stacktrace:");
				t.printStackTrace(System.out);
				if(!doNotShowPopupAgain)
				{
					doNotShowPopupAgain = true;
					JOptionPane.showMessageDialog(null, "Failed to run job on render thread. World is probably corrupted now. Please report stacktrace. And restart. This message will not be shown again.");
				}
			}
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
			if(fullyLoaded)
			{
				//Draw primary tool in world:
				if(primaryTool != null)
				{
					primaryTool.renderWorld(view);
				}
				
				//Draw secondary overlays:
				clusterHighlighter.highlightCluster(view);
				
				//Draw primary tool overlay:
				if(primaryTool != null)
				{
					primaryTool.renderOverlay(view);
				}
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
		
		if(hitpoint.canBePlacedOn())
		{
			//TBI: When drawing, the hitpoint is inaccurate, cause it might consider the wrong target position (when control).
			HitpointContainer hitpointContainer = (HitpointContainer) hitpoint;
			
			//Calculate normal (it is almost always required for later rotation steps):
			if(hitpoint.isBoard())
			{
				HitpointBoard hitpointBoard = (HitpointBoard) hitpoint;
				hitpointBoard.setNormal(hitpoint.getHitPart().getAlignmentGlobal().inverse().multiply(hitpointBoard.getLocalNormal()).normalize());
			}
			else //TBI: Currently just assume Y-Pos
			{
				hitpointContainer.setNormal(hitpoint.getHitPart().getAlignmentGlobal().inverse().multiply(Vector3.yp).normalize());
			}
		}
		
		if(primaryTool != null)
		{
			hitpoint = primaryTool.adjustHitpoint(hitpoint);
		}
		
		this.hitpoint = hitpoint;
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
	
	public double calculateFixRotationOffset(Quaternion newGlobalAlignment, Hitpoint hitpoint)
	{
		HitpointContainer hitpointContainer = (HitpointContainer) hitpoint;
		FourDirections axes = new FourDirections(hitpoint.isBoard() ? ((HitpointBoard) hitpoint).getLocalNormal() : Vector3.yp, hitpoint.getHitPart().getAlignmentGlobal());
		
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
			model.translate((float) label.getPositionGlobal().getX(), (float) label.getPositionGlobal().getY(), (float) label.getPositionGlobal().getZ());
			Matrix rotMat = new Matrix(label.getAlignmentGlobal().createMatrix());
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
	
	//### Cluster-Highlighter: ###
	
	//Must be called on the simulation thread.
	public void clustersChanged(List<Cluster> clusters)
	{
		clusterHighlighter.clustersChanged(clusters);
	}
	
	//Must be called on the simulation thread.
	public void clustersOutOfPlace(List<Cluster> clusters)
	{
		clusterHighlighter.clustersOutOfPlace(clusters);
	}
	
	//Must be called on the render thread.
	public void clustersBackInPlace()
	{
		clusterHighlighter.clustersBackInPlace();
	}
	
	//### Getter and stuff: ###
	
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
	
	public void resetFineBoardOffset()
	{
		defaultTool.resetFineBoardOffset();
	}
	
	public Hitpoint getHitpoint()
	{
		return hitpoint;
	}
}

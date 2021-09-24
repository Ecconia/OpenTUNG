package de.ecconia.java.opentung.core.tools.grabbing;

import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.CompLabel;
import de.ecconia.java.opentung.components.CompMount;
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
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.core.BoardUniverse;
import de.ecconia.java.opentung.core.data.Hitpoint;
import de.ecconia.java.opentung.core.data.HitpointBoard;
import de.ecconia.java.opentung.core.data.HitpointContainer;
import de.ecconia.java.opentung.core.data.ShaderStorage;
import de.ecconia.java.opentung.core.data.SharedData;
import de.ecconia.java.opentung.core.helper.OnBoardPlacementHelper;
import de.ecconia.java.opentung.core.helper.World3DHelper;
import de.ecconia.java.opentung.core.structs.GPUTask;
import de.ecconia.java.opentung.core.tools.Tool;
import de.ecconia.java.opentung.core.tools.grabbing.data.GrabContainerData;
import de.ecconia.java.opentung.core.tools.grabbing.data.GrabData;
import de.ecconia.java.opentung.inputs.Controller3D;
import de.ecconia.java.opentung.interfaces.RenderPlane2D;
import de.ecconia.java.opentung.interfaces.windows.ExportWindow;
import de.ecconia.java.opentung.libwrap.Matrix;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;
import de.ecconia.java.opentung.meshing.ConductorMeshBag;
import de.ecconia.java.opentung.meshing.MeshBagContainer;
import de.ecconia.java.opentung.raycast.WireRayCaster;
import de.ecconia.java.opentung.savefile.Saver;
import de.ecconia.java.opentung.settings.Settings;
import de.ecconia.java.opentung.settings.keybinds.Keybindings;
import de.ecconia.java.opentung.simulation.Cluster;
import de.ecconia.java.opentung.simulation.ClusterHelper;
import de.ecconia.java.opentung.simulation.HiddenWire;
import de.ecconia.java.opentung.simulation.InitClusterHelper;
import de.ecconia.java.opentung.simulation.Powerable;
import de.ecconia.java.opentung.simulation.SimulationManager;
import de.ecconia.java.opentung.simulation.Updateable;
import de.ecconia.java.opentung.simulation.Wire;
import de.ecconia.java.opentung.util.Tuple;
import de.ecconia.java.opentung.util.math.MathHelper;
import de.ecconia.java.opentung.util.math.Quaternion;
import de.ecconia.java.opentung.util.math.Vector3;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import javax.swing.JOptionPane;
import org.lwjgl.opengl.GL30;

public class Grabbing implements Tool
{
	private final SharedData sharedData;
	private final Controller3D controller;
	private final BlockingQueue<GPUTask> gpuTasks;
	private final ShaderStorage shaderStorage;
	private final MeshBagContainer worldMesh;
	private final MeshBagContainer secondaryMesh;
	private final SimulationManager simulation;
	private final BoardUniverse board;
	private final WireRayCaster wireRayCaster;
	
	private Hitpoint hitpoint; //Hitpoint is not thread-safe and constantly changes.
	
	private GrabData grabData;
	
	private double fineBoardOffset;
	private double xBoardOffset;
	private double zBoardOffset;
	private double grabRotation;
	
	//ActivationNow transfer values:
	private boolean isGrabbing;
	private Component component;
	private CompContainer parent;
	
	private ExportWindow exportWindow;
	
	public Grabbing(SharedData sharedData)
	{
		this.sharedData = sharedData;
		
		controller = sharedData.getRenderPlane3D().getController();
		gpuTasks = sharedData.getGpuTasks();
		shaderStorage = sharedData.getShaderStorage();
		worldMesh = sharedData.getRenderPlane3D().getWorldMesh();
		secondaryMesh = sharedData.getRenderPlane3D().getSecondaryMesh();
		simulation = sharedData.getBoardUniverse().getSimulation();
		board = sharedData.getBoardUniverse();
		wireRayCaster = sharedData.getRenderPlane3D().getWireRayCaster();
		
		sharedData.getRenderPlane3D().addTool(new ImportTool(sharedData, this));
		RenderPlane2D interfaceRenderer = sharedData.getRenderPlane2D();
		exportWindow = new ExportWindow(this, interfaceRenderer);
		interfaceRenderer.addWindow(exportWindow);
	}
	
	@Override
	public Boolean activateKeyUp(Hitpoint hitpoint, int scancode, boolean control)
	{
		if(scancode == Keybindings.KeyGrab)
		{
			if(hitpoint.isEmpty())
			{
				System.out.println("Look at something to grab/copy it.");
				return false;
			}
			if(hitpoint.getHitPart() instanceof Wire)
			{
				System.out.println("Cannot grab/copy wires.");
				return false;
			}
			Component component = hitpoint.getHitPart() instanceof Connector ? hitpoint.getHitPart().getParent() : (Component) hitpoint.getHitPart();
			this.hitpoint = hitpoint;
			if(control)
			{
				isGrabbing = false;
				return copy(component);
			}
			else
			{
				isGrabbing = true;
				return grab(component);
			}
		}
		return null;
	}
	
	@Override
	public void activateNow(Hitpoint hitpoint)
	{
		if(isGrabbing)
		{
			grabImpl();
		}
		else
		{
			copyImpl();
		}
	}
	
	//Input events:
	
	@Override
	public boolean keyUp(int scancode, boolean control)
	{
		//Currently grabbing:
		if(scancode == Keybindings.KeyGrabAbort)
		{
			sharedData.getRenderPlane3D().toolStopInputs();
			abort();
			return true;
		}
		else if(scancode == Keybindings.KeyGrabDelete)
		{
			sharedData.getRenderPlane3D().toolStopInputs();
			deleteGrabbed();
			return true;
		}
		else if(grabData.getComponent() instanceof CompBoard)
		{
			if(scancode == Keybindings.KeyGrabExport)
			{
				if(grabData.getComponent().getClass() == CompBoard.class)
				{
					exportWindow.activate();
				}
				else
				{
					System.out.println("Can only export boards.");
				}
				return true;
			}
			else if(scancode == Keybindings.KeyGrabRotateY)
			{
				rotateGrabbedBoard(Quaternion.angleAxis(-90, Vector3.yp));
				return true;
			}
			else if(scancode == Keybindings.KeyGrabRotateX)
			{
				rotateGrabbedBoard(Quaternion.angleAxis(-90, Vector3.xp));
				return true;
			}
			else if(scancode == Keybindings.KeyGrabRotateZ)
			{
				rotateGrabbedBoard(Quaternion.angleAxis(-90, Vector3.zp));
				return true;
			}
		}
		else //Not grabbing/copying a board:
		{
			if(scancode == Keybindings.KeyGrabRotate)
			{
				rotatePlacement(control);
				return true;
			}
		}
		
		return false;
	}
	
	private void rotateGrabbedBoard(Quaternion rotator)
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
	
	private void rotatePlacement(boolean control)
	{
		gpuTasks.add((unused) -> {
			grabRotation += control ? 22.5 : 90;
			if(grabRotation >= 360)
			{
				grabRotation -= 360;
			}
			if(grabRotation <= 0)
			{
				grabRotation += 360;
			}
		});
	}
	
	@Override
	public boolean mouseLeftUp()
	{
		final Hitpoint hitpoint = this.hitpoint;
		
		//Abort if not fully loaded
		if(!hitpoint.canBePlacedOn())
		{
			//If not looking at a container abort.
			return true;
		}
		
		CompContainer parent = (CompContainer) hitpoint.getHitPart();
		if(parent != board.getRootBoard() && parent.getParent() == null)
		{
			System.out.println("Board attempted to place on is deleted/gone.");
			return true;
		}
		
		//From here on it will be executed no more turning back:
		sharedData.getRenderPlane3D().toolStopInputs();
		
		HitpointContainer hitpointContainer = (HitpointContainer) hitpoint;
		Component grabbedComponent = grabData.getComponent();
		
		Quaternion deltaAlignment = hitpointContainer.getAlignment(); //TODO: In some cases this is null, which causes an NPE. Happened when spam stacking. Still the case 2021.09.16
		//Round the rotation before placement, prevents horrible (deforming) issues:
		{
			double realAngleHalf = Math.acos(deltaAlignment.getA());
			double divisor = Math.sin(realAngleHalf);
			if(divisor != 0) //Do not round an sin(angle) of 0, causes division by 0 -> NaN
			{
				Vector3 realVector = deltaAlignment.getV().divide(divisor);
				if(realVector.lengthSquared() > 0) //Always positive.
				{
					deltaAlignment = Quaternion.angleAxis(
							//The real angle is actually always with *2.0, but for optimization reasons it has been moved down here.
							Math.round(Math.toDegrees(realAngleHalf * 2.0) * 100.0) / 100.0,
							realVector.normalize()
					);
				}
			}
		}
		if(Double.isNaN(deltaAlignment.getA()) || Double.isNaN(deltaAlignment.getV().getX()) || Double.isNaN(deltaAlignment.getV().getY()) || Double.isNaN(deltaAlignment.getV().getZ()))
		{
			System.out.println("Rounding the placement alignment went horribly wrong (or something before it), aborting your grabment for safety. Please report and reconstruct.");
			JOptionPane.showMessageDialog(null, "Rounding the placement alignment went horribly wrong (or something before it), aborting your grabment for safety. Please report and reconstruct.");
			abort();
			return true;
		}
		
		Vector3 newPosition = hitpointContainer.getPosition();
		Vector3 oldPosition = grabbedComponent.getPositionGlobal();
		for(GrabData.WireContainer wireContainer : grabData.getOutgoingWiresWithSides())
		{
			CompWireRaw wire = wireContainer.wire;
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
			
			CompWireRaw cWire = wire;
			cWire.setPositionGlobal(position);
			cWire.setAlignmentGlobal(rotation);
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
		
		gpuTasks.add((worldRenderer) -> {
			if(!grabData.isCopy())
			{
				worldRenderer.clustersBackInPlace();
			}
			
			//Move to new meshes:
			for(Component component : grabData.getComponents())
			{
				secondaryMesh.removeComponent(component, board.getSimulation());
				worldMesh.addComponent(component, board.getSimulation());
				if(component instanceof CompLabel && ((CompLabel) component).hasText())
				{
					board.getLabelsToRender().add((CompLabel) component);
				}
			}
			
			grabbedComponent.setParent(parent);
			parent.addChild(grabbedComponent);
			grabbedComponent.updateBoundsDeep();
			parent.updateBounds();
			for(CompWireRaw wire : grabData.getOutgoingWires())
			{
				worldMesh.addComponent(wire, board.getSimulation());
				wireRayCaster.addWire(wire);
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
				
				for(CompSnappingPeg snappingPeg : grabContainerData.getUnconnectedSnappingPegs())
				{
					sharedData.getRenderPlane3D().snapSnappingPeg(snappingPeg);
				}
			}
			else if(grabData.getComponent() instanceof CompSnappingPeg)
			{
				sharedData.getRenderPlane3D().snapSnappingPeg((CompSnappingPeg) grabData.getComponent());
			}
			
			//TODO: snapSnappingPeg spawns further jobs. Do not disable before they are done.
			worldRenderer.toolDisable(); //Done execution.
			this.grabData = null;
		});
		
		return true;
	}
	
	@Override
	public boolean scroll(int amount, boolean control, boolean alt)
	{
		if(grabData.getComponent() instanceof CompBoard)
		{
			gpuTasks.add((unused) -> {
				if(!control)
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
			return true; //When grabbing/copying a board, allow board offset
		}
		return false;
	}
	
	//TODO: Properly handle abort, while it is still initializing.
	
	@Override
	public boolean abort()
	{
		System.out.println("Abort grabbing...");
		gpuTasks.add((worldRenderer) -> {
			if(grabData.isCopy())
			{
				for(CompLabel label : grabData.getLabels())
				{
					//Unload the copied texture:
					label.unload();
				}
				for(Component comp : grabData.getComponents())
				{
					secondaryMesh.removeComponent(comp, simulation);
				}
				if(grabData instanceof GrabContainerData)
				{
					GrabContainerData gcd = (GrabContainerData) grabData;
					for(CompWireRaw wire : gcd.getInternalWires())
					{
						secondaryMesh.removeComponent(wire, simulation);
					}
					for(CompSnappingWire wire : gcd.getInternalSnappingWires())
					{
						secondaryMesh.removeComponent(wire, simulation);
					}
					simulation.updateJobNextTickThreadSafe((simulation) -> {
						//Supply 'null' as update collection map, cause the wires are all invisible by now, code will never be called.
						for(CompWireRaw wire : gcd.getInternalWires())
						{
							ClusterHelper.removeWire(simulation, wire, null);
						}
						//Iterating over connectors is obsolete once all wires are removed.
					});
				}
				
				grabData = null;
				//Can be called before the simulation thread is done, cause the next tool will have to wait for that to be done. It does not spawn jobs.
				worldRenderer.toolDisable();
				return;
			}
			
			//Update parent relation: (Update them first, so that the ray-casting for snapping pegs works)
			Component grabbedComponent = grabData.getComponent();
			CompContainer grabbedParent = grabData.getParent();
			grabbedParent.addChild(grabbedComponent);
			grabbedParent.updateBounds();
			grabbedComponent.setParent(grabbedParent);
			
			worldRenderer.clustersBackInPlace();
			for(Component component : grabData.getComponents())
			{
				secondaryMesh.removeComponent(component, simulation);
				worldMesh.addComponent(component, simulation);
				if(component instanceof CompLabel)
				{
					if(((CompLabel) component).hasText())
					{
						board.getLabelsToRender().add((CompLabel) component);
					}
				}
			}
			
			for(CompWireRaw wire : grabData.getOutgoingWires())
			{
				worldMesh.addComponent(wire, simulation);
				//Snapping peg wires should not and technically can't be in the wire ray-caster...
				wireRayCaster.addWire(wire);
			}
			if(grabData instanceof GrabContainerData)
			{
				GrabContainerData grabContainerData = (GrabContainerData) grabData;
				for(CompWireRaw wire : grabContainerData.getInternalWires())
				{
					CompWireRaw compWireRaw = wire;
					secondaryMesh.removeComponent(compWireRaw, simulation);
					wireRayCaster.addWire(compWireRaw);
					worldMesh.addComponent(compWireRaw, simulation);
				}
				for(CompSnappingWire wire : grabContainerData.getInternalSnappingWires())
				{
					secondaryMesh.removeComponent(wire, simulation);
					worldMesh.addComponent(wire, simulation);
				}
				//TODO: snapSnappingPeg spawns further jobs. Do not disable before they are done.
				for(CompSnappingPeg snappingPeg : grabContainerData.getUnconnectedSnappingPegs())
				{
					sharedData.getRenderPlane3D().snapSnappingPeg(snappingPeg);
				}
			}
			else if(grabData.getComponent() instanceof CompSnappingPeg)
			{
				sharedData.getRenderPlane3D().snapSnappingPeg((CompSnappingPeg) grabData.getComponent());
			}
			
			grabData = null;
			worldRenderer.toolDisable();
		});
		return true;
	}
	
	//Take overs:
	
	public void takeImportOver(GrabContainerData grabData)
	{
		//Data post-processing has already been done.
		
		//Simulation trigger:
		simulation.updateJobNextTickThreadSafe((unused -> {
			for(Component component : grabData.getComponents())
			{
				if(component instanceof Updateable)
				{
					simulation.updateNextTickThreadSafe((Updateable) component);
				}
			}
			
			//GPU stuff:
			gpuTasks.add((world3D -> {
				for(Component comp : grabData.getComponents())
				{
					secondaryMesh.addComponent(comp, simulation);
				}
				
				for(CompWireRaw wire : grabData.getInternalWires())
				{
					secondaryMesh.addComponent(wire, simulation);
				}
				
				for(CompSnappingWire wire : grabData.getInternalSnappingWires())
				{
					secondaryMesh.addComponent(wire, simulation);
				}
				
				Component component = grabData.getComponent();
				Quaternion inverse = component.getAlignmentGlobal().inverse();
				sharedData.getRenderPlane3D().resetFixPos(inverse.multiply(Vector3.xp), inverse.multiply(Vector3.yp));
				this.grabRotation = 0;
				if(component instanceof CompBoard)
				{
					CompBoard board = (CompBoard) component;
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
				
				world3D.toolReady();
			}));
		}));
	}
	
	public void guiExportClosed(Path chosenPath)
	{
		if(chosenPath == null)
		{
			return;
		}
		
		String fileName = chosenPath.getFileName().toString();
		int endingIndex = fileName.lastIndexOf('.');
		if(endingIndex < 0)
		{
			chosenPath = chosenPath.resolveSibling(fileName + ".opentung");
		}
		else
		{
			String ending = fileName.substring(endingIndex + 1);
			if(!ending.equals("opentung"))
			{
				//Just overwrite the path with something ending on '.opentung':
				chosenPath = chosenPath.resolveSibling(chosenPath.getFileName().toString() + ".opentung");
			}
		}
		
		Saver.save((CompBoard) grabData.getComponent(), grabData.getInternalWires(), chosenPath);
		System.out.println("Exported board to: " + chosenPath);
	}
	
	//Internal:
	
	public void deleteGrabbed()
	{
		{
			GrabData gd = grabData;
			if(gd != null && gd.isCopy())
			{
				abort();
				return;
			}
		}
		simulation.updateJobNextTickThreadSafe((unused) -> {
			Map<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> updates = new HashMap<>();
			List<Cluster> modifiedClusters = null;
			boolean isContainer = grabData instanceof GrabContainerData;
			if(!isContainer) //Has to be done first before all cluster modifications. Delete only removes clusters.
			{
				//Collect clusters:
				if(grabData.getComponent() instanceof ConnectedComponent)
				{
					List<Connector> connectors = ((ConnectedComponent) grabData.getComponent()).getConnectors();
					modifiedClusters = new ArrayList<>(connectors.size());
					for(Connector connector : connectors)
					{
						modifiedClusters.add(connector.getCluster());
					}
				}
			}
			//TBI: Removing wires first? Or just all blots/pegs?
			for(CompWireRaw wire : grabData.getOutgoingWires())
			{
				//Outgoing wires:
				wire.setParent(null);
				ClusterHelper.removeWire(simulation, wire, updates);
			}
			if(isContainer)
			{
				GrabContainerData grabContainerData = (GrabContainerData) grabData;
				for(CompSnappingWire wire : grabContainerData.getInternalSnappingWires())
				{
					wire.setParent(null);
					ClusterHelper.removeWire(simulation, wire, updates);
				}
				for(CompWireRaw wire : grabContainerData.getInternalWires())
				{
					wire.setParent(null);
					ClusterHelper.removeWire(simulation, wire, updates);
				}
			}
			for(Component component : grabData.getComponents())
			{
				if(component instanceof ConnectedComponent)
				{
					ConnectedComponent con = (ConnectedComponent) component;
					for(Peg peg : con.getPegs())
					{
						ClusterHelper.removePeg(simulation, peg, updates);
					}
					for(Blot blot : con.getBlots())
					{
						ClusterHelper.removeBlot(simulation, blot, updates);
					}
				}
			}
			//Now that all grab-deleted components have no parent and all clusters are taken apart, regenerate the highlighted cluster if required:
			sharedData.getRenderPlane3D().clustersChanged(modifiedClusters);
			
			gpuTasks.add((worldRenderer) -> {
				worldRenderer.clustersBackInPlace(); //Although possibly heavily modified...
				System.out.println("[ClusterUpdateDebug] Updating " + updates.size() + " conductor mesh bags.");
				for(Map.Entry<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> entry : updates.entrySet())
				{
					entry.getKey().handleUpdates(entry.getValue(), simulation);
				}
				for(Component component : grabData.getComponents())
				{
					secondaryMesh.removeComponent(component, simulation);
					if(component instanceof CompLabel)
					{
						//Unload the texture, assuming there is one - code handles that.
						((CompLabel) component).unload();
					}
				}
				if(grabData instanceof GrabContainerData)
				{
					GrabContainerData grabContainerData = (GrabContainerData) grabData;
					for(CompSnappingWire wire : grabContainerData.getInternalSnappingWires())
					{
						secondaryMesh.removeComponent(wire, simulation);
					}
					for(CompWireRaw wire : grabContainerData.getInternalWires())
					{
						secondaryMesh.removeComponent(wire, simulation);
						board.getWiresToRender().remove(wire);
					}
				}
				for(CompWireRaw wire : grabData.getOutgoingWires())
				{
					//Now snapping nor hidden wires at this point.
					board.getWiresToRender().remove(wire);
				}
				
				//That's pretty much it. Just make the clipboard invisible:
				grabData = null;
				worldRenderer.toolDisable();
			});
		});
	}
	
	public boolean grab(Component toBeGrabbed)
	{
		parent = (CompContainer) toBeGrabbed.getParent();
		if(parent == null)
		{
			System.out.println("Can't grab component, since its either the root board or soon deleted.");
			return false;
		}
		//Setting the parent to null is a thread-safe operation. It has to be. Doing this declares this component as "busy", means no other interaction with it should be possible.
		toBeGrabbed.setParent(null);
		
		component = toBeGrabbed;
		return true; //Causes activateNow() to be called.
	}
	
	private void grabImpl()
	{
		CompContainer parent = this.parent;
		this.parent = null;
		Component toBeGrabbed = this.component;
		this.component = null;
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
						if(((CompLabel) component).hasText())
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
			simulation.updateJobNextTickThreadSafe((simulation) -> {
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
				
				if(!isContainer)
				{
					List<Cluster> modifiedClusters = new ArrayList<>(connectors.size());
					for(Connector connector : connectors)
					{
						modifiedClusters.add(connector.getCluster());
					}
					sharedData.getRenderPlane3D().clustersOutOfPlace(modifiedClusters);
				}
				else
				{
					sharedData.getRenderPlane3D().clustersOutOfPlace(null);
				}
				
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
				gpuTasks.add((worldRenderer) -> {
					//Updates caused by the simulation when ripping out snapping wire connections.
					// In general happens when a cluster gets divided by this action, so that one of the cluster half's is now off.
					System.out.println("[ClusterUpdateDebug] (Grabbing/SnappingWireRemoval) Updating " + updates.size() + " conductor mesh bags.");
					for(Map.Entry<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> entry : updates.entrySet())
					{
						entry.getKey().handleUpdates(entry.getValue(), simulation);
					}
					
					//Move components into the grab/secondary-mesh: Done in this stage, as a final visual confirmation to the user. Could be done in the first render stage.
					for(Component component : newGrabData.getComponents())
					{
						worldMesh.removeComponent(component, simulation);
						secondaryMesh.addComponent(component, simulation);
					}
					//Wires: The general rule for them is, move from primary to secondary mesh. Discard outgoing snapping wires, and remove normal wires from the wire-ray-caster.
					for(CompSnappingWire wire : snappingWiresToRemove)
					{
						worldMesh.removeComponent(wire, simulation);
					}
					for(CompWireRaw wire : newGrabData.getOutgoingWires())
					{
						wireRayCaster.removeWire(wire);
						worldMesh.removeComponent(wire, simulation);
					}
					for(CompSnappingWire wire : internalSnappingWires)
					{
						worldMesh.removeComponent(wire, simulation);
						secondaryMesh.addComponent(wire, simulation);
					}
					for(CompWireRaw wire : internalWires)
					{
						//Also remove these from the ray-caster:
						wireRayCaster.removeWire(wire);
						worldMesh.removeComponent(wire, simulation);
						secondaryMesh.addComponent(wire, simulation);
					}
					
					//Finished the processing, now apply the grabbing:
					Quaternion inverse = toBeGrabbed.getAlignmentGlobal().inverse();
					sharedData.getRenderPlane3D().resetFixPos(inverse.multiply(Vector3.xp), inverse.multiply(Vector3.yp));
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
					
					worldRenderer.toolReady();
				});
			});
		});
	}
	
	private void alignComponent(Component component, Vector3 oldPosition, Vector3 newPosition, Quaternion deltaRotation)
	{
		component.setAlignmentGlobal(component.getAlignmentGlobal().multiply(deltaRotation).normalize()); //Normalization shall prevent parent quaternion corruption to spread further.
		Vector3 newPos = component.getPositionGlobal().subtract(oldPosition);
		newPos = deltaRotation.inverse().multiply(newPos);
		newPos = newPos.add(newPosition);
		component.setPositionGlobal(newPos);
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
	
	public boolean copy(Component componentToCopy)
	{
		if(grabData != null)
		{
			return false;
		}
		if(componentToCopy instanceof Wire)
		{
			//We don't copy wires.
			return false;
		}
		
		//No need to do the parent check, since we want to copy the component and not change it.
		this.component = componentToCopy;
		return true; //Causes activateNow() to be called.
	}
	
	private void copyImpl()
	{
		Component componentToCopy = this.component;
		this.component = null;
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
						if(((CompLabel) copyComponent).hasText())
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
			simulation.updateJobNextTickThreadSafe((simulation) -> {
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
				gpuTasks.add((worldRenderer) -> {
					//Add copied components into the grab/secondary-mesh: Done in this stage, as a final visual confirmation to the user. Could be done in the first render stage.
					for(Component comp : grabData.getComponents())
					{
						secondaryMesh.addComponent(comp, simulation);
					}
					//Wires: The general rule for them is, add them to the secondary mesh. Skip any outgoing wire.
					for(CompWireRaw wire : internalWiresCopy)
					{
						secondaryMesh.addComponent(wire, simulation);
					}
					if(isContainer)
					{
						//Has to be wrapped in isContainer, since we need the copy of the list, not the originals.
						for(CompSnappingWire wire : ((GrabContainerData) grabData).getInternalSnappingWires())
						{
							secondaryMesh.addComponent(wire, simulation);
						}
					}
					
					//Finished the processing, now apply the grabbing:
					Quaternion inverse = componentToCopy.getAlignmentGlobal().inverse();
					sharedData.getRenderPlane3D().resetFixPos(inverse.multiply(Vector3.xp), inverse.multiply(Vector3.yp));
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
					
					worldRenderer.toolReady();
				});
			});
		});
	}
	
	public Quaternion getAbsoluteGrabAlignment(HitpointContainer hitpoint)
	{
		Component component = grabData.getComponent();
		boolean grabbingBoard = component instanceof CompBoard;
		
		//Calculate the new alignment:
		Quaternion absAlignment = MathHelper.rotationFromVectors(Vector3.yp, hitpoint.getNormal()); //Get the direction of the new placement position (with invalid rotation).
		double normalAxisRotationAngle = sharedData.getRenderPlane3D().calculateFixRotationOffset(absAlignment, hitpoint);
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
				currentAlignment = component.getAlignmentGlobal().multiply(absAlignment.inverse());
				((GrabContainerData) grabData).setAlignment(currentAlignment);
			}
			absAlignment = currentAlignment.multiply(absAlignment);
		}
		
		//Since the Rotation cannot be changed, it must be modified. So we undo the old rotation and apply the new one.
		return absAlignment;
	}
	
	//Math:
	
	@Override
	public Hitpoint adjustHitpoint(Hitpoint hitpoint)
	{
		//TODO: Fix hitpoint access, it should not
		this.hitpoint = hitpoint; //Update the hitpoint.
		
		if(hitpoint.canBePlacedOn())
		{
			//Calculate new position:
			CompContainer parent = (CompContainer) hitpoint.getHitPart();
			
			HitpointContainer hitpointContainer = (HitpointContainer) hitpoint;
			
			Quaternion absoluteAlignment = getAbsoluteGrabAlignment(hitpointContainer);
			Quaternion relativeAlignment = grabData.getComponent().getAlignmentGlobal().inverse().multiply(absoluteAlignment);
			relativeAlignment = relativeAlignment.normalize(); //Already normalize the relative alignment. Since it is used for drawing.
			hitpointContainer.setAlignment(relativeAlignment);
			
			//Figure out the base position:
			Vector3 position;
			{
				if(hitpoint.isBoard()) //Placing on board
				{
					HitpointBoard hitpointBoard = (HitpointBoard) hitpoint;
					OnBoardPlacementHelper placementHelper = new OnBoardPlacementHelper((CompBoard) parent, hitpointBoard.getLocalNormal(), hitpointBoard.getCollisionPointBoardSpace());
					
					if(grabData.getComponent() instanceof CompBoard) //Special handling for boards, since it does not use "auto" placement mode as base.
					{
						position = placementHelper.middleEither();
						position = parent.getAlignmentGlobal().inverse().multiply(position).add(parent.getPositionGlobal());
						
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
								offset = parent.getAlignmentGlobal().inverse().multiply(offset);
								position = position.add(offset);
							}
						}
						else if(!isLaying) //isTop/Bottom and standing
						{
							y = fineBoardOffset;
						}
						Vector3 offset = new Vector3(x, y, z);
						offset = absoluteAlignment.inverse().multiply(offset);
						position = position.add(offset);
					}
					else //Is a normal component
					{
						position = placementHelper.auto(grabData.getComponent().getInfo().getModel(), controller.isControl(), absoluteAlignment);
						if(position == null && grabData.getComponent() instanceof CompSnappingPeg)
						{
							//Attempt again without control: (Should center it).
							position = placementHelper.auto(grabData.getComponent().getInfo().getModel(), false, absoluteAlignment);
						}
						if(position == null)
						{
							this.hitpoint = new Hitpoint(hitpoint.getHitPart(), hitpoint.getDistance()); //Prevent the component from being drawn, by just changing the hitpoint type. [pretend non-container]
							return this.hitpoint;
						}
						else
						{
							position = parent.getAlignmentGlobal().inverse().multiply(position).add(parent.getPositionGlobal());
							if(grabData.getComponent() instanceof CompMount)
							{
								if(!placementHelper.isSide() && !controller.isControl())
								{
									//Apply offset:
									Vector3 offset = new Vector3(0, 0, -0.15);
									position = position.add(absoluteAlignment.inverse().multiply(offset));
								}
							}
						}
					}
				}
				else //Placing on mount
				{
					ModelHolder model = grabData.getComponent().getModelHolder();
					if(model.canBePlacedOnMounts())
					{
						position = parent.getPositionGlobal().add(hitpointContainer.getNormal().multiply(CompMount.MOUNT_HEIGHT));
						if(grabData.getComponent() instanceof CompBoard)
						{
							//Apply offsets:
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
							double z = isLaying || !isSideX ? zBoardOffset : 0;
							
							Vector3 offset = new Vector3(x, 0, z);
							Quaternion absAlignment = grabData.getComponent().getAlignmentGlobal().multiply(relativeAlignment);
							offset = absAlignment.inverse().multiply(offset);
							position = position.add(offset);
						}
					}
					else
					{
						this.hitpoint = new Hitpoint(hitpoint.getHitPart(), hitpoint.getDistance()); //No placement.
						return this.hitpoint;
					}
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
		
		return hitpoint;
	}
	
	//Visuals:
	
	@Override
	public void renderWorld(float[] view)
	{
		if(!hitpoint.canBePlacedOn())
		{
			return;
		}
		
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
		Vector3 oldPosition = grabbedComponent.getPositionGlobal();
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
				if(!label.hasTexture())
				{
					continue;
				}
				Vector3 position = label.getPositionGlobal();
				position = position.subtract(oldPosition);
				position = newRelativeAlignment.inverse().multiply(position);
				position = position.add(newPosition);
				Quaternion alignment = label.getAlignmentGlobal().multiply(newRelativeAlignment);
				
				m.identity();
				m.translate((float) position.getX(), (float) position.getY(), (float) position.getZ());
				m.multiply(new Matrix(alignment.createMatrix()));
				sdfShader.setUniformM4(2, m.getMat());
				
				label.activate();
				label.getModelHolder().drawTextures();
			}
		}
	}
	
	@Override
	public void renderOverlay(float[] view)
	{
		if(!(grabData.getComponent() instanceof CompBoard) || !hitpoint.canBePlacedOn())
		{
			return;
		}
		
		//Enable drawing to stencil buffer
		GL30.glStencilMask(0xFF);
		
		ShaderProgram invisibleCubeShader = shaderStorage.getInvisibleCubeShader();
		GenericVAO invisibleCube = shaderStorage.getInvisibleCube();
		
		//Do very very ugly drawing of board:
		Component grabbedComponent = grabData.getComponent();
		Meshable meshable = grabbedComponent.getModelHolder().getSolid().get(0); //Grabbed board or mount, either way -> solid
		
		HitpointContainer hitpointContainer = (HitpointContainer) hitpoint;
		Vector3 position = hitpointContainer.getPosition();
		//Construct absolute rotation again...
		Quaternion rotation = grabbedComponent.getAlignmentGlobal().multiply(hitpointContainer.getAlignment());
		
		invisibleCubeShader.use();
		invisibleCubeShader.setUniformM4(1, view);
		invisibleCubeShader.setUniformV4(3, new float[]{0, 0, 0, 0});
		World3DHelper.drawCubeFull(invisibleCubeShader, invisibleCube, (CubeFull) meshable, position, grabbedComponent, rotation, grabbedComponent.getModelHolder().getPlacementOffset(), new Matrix());
		
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
}

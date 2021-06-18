package de.ecconia.java.opentung.core.tools;

import de.ecconia.java.opentung.components.conductor.Blot;
import de.ecconia.java.opentung.components.conductor.CompWireRaw;
import de.ecconia.java.opentung.components.conductor.Connector;
import de.ecconia.java.opentung.core.BoardUniverse;
import de.ecconia.java.opentung.core.data.Hitpoint;
import de.ecconia.java.opentung.core.data.ShaderStorage;
import de.ecconia.java.opentung.core.data.SharedData;
import de.ecconia.java.opentung.core.helper.World3DHelper;
import de.ecconia.java.opentung.core.structs.GPUTask;
import de.ecconia.java.opentung.core.systems.CPURaycast;
import de.ecconia.java.opentung.inputs.InputProcessor;
import de.ecconia.java.opentung.libwrap.Matrix;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;
import de.ecconia.java.opentung.meshing.ConductorMeshBag;
import de.ecconia.java.opentung.meshing.MeshBagContainer;
import de.ecconia.java.opentung.raycast.WireRayCaster;
import de.ecconia.java.opentung.simulation.ClusterHelper;
import de.ecconia.java.opentung.simulation.SimulationManager;
import de.ecconia.java.opentung.simulation.Wire;
import de.ecconia.java.opentung.util.math.MathHelper;
import de.ecconia.java.opentung.util.math.Quaternion;
import de.ecconia.java.opentung.util.math.Vector3;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import org.lwjgl.opengl.GL30;

public class DrawWire implements Tool
{
	private static final float[] wireColor = {
			1.0f,
			0.0f,
			1.0f,
			1.0f
	};
	
	private static final float[] group1 = {
			1.0f,
			0.5f,
			0.0f,
			0.7f
	};
	
	private static final float[] group2 = {
			1.0f,
			1.0f,
			0.0f,
			0.7f
	};
	
	private final SharedData sharedData;
	private final BlockingQueue<GPUTask> gpuTasks;
	private final SimulationManager simulation;
	private final ShaderStorage shaderStorage;
	private final MeshBagContainer worldMesh;
	private final WireRayCaster wireRayCaster;
	private final BoardUniverse board;
	
	private Connector firstConnector;
	private Connector secondConnector;
	
	private boolean unArmed;
	private FrameData frameData;
	private boolean isChoosingSecondGroup;
	
	private static final FrameData emptyFrame = new FrameData(new LinkedList<>(), false, null, null);
	
	public DrawWire(SharedData sharedData)
	{
		this.sharedData = sharedData;
		
		board = sharedData.getBoardUniverse();
		gpuTasks = sharedData.getGpuTasks();
		simulation = sharedData.getBoardUniverse().getSimulation();
		shaderStorage = sharedData.getShaderStorage();
		worldMesh = sharedData.getRenderPlane3D().getWorldMesh();
		wireRayCaster = sharedData.getRenderPlane3D().getWireRayCaster();
	}
	
	@Override
	public boolean abort()
	{
		sharedData.getRenderPlane3D().toolStopInputs();
		gpuTasks.add((worldRenderer) -> {
			firstConnector = secondConnector = null;
			isChoosingSecondGroup = false;
			frameData = emptyFrame;
			worldRenderer.toolDisable();
		});
		return true;
	}
	
	@Override
	public Boolean activateMouseDown(Hitpoint hitpoint, int buttonCode, boolean control)
	{
		if(buttonCode != InputProcessor.MOUSE_RIGHT)
		{
			return null;
		}
		
		if(hitpoint.getHitPart() instanceof Connector)
		{
			this.unArmed = true;
			this.firstConnector = (Connector) hitpoint.getHitPart(); //Start with the connector we hit the mouse down.
			this.secondConnector = null; //There is no second connector yet.
			frameData = emptyFrame; //Set the initial empty frame data.
			isChoosingSecondGroup = false;
			gpuTasks.add((worldRenderer) -> {
				worldRenderer.toolReady();
			});
			
			return true;
		}
		return null;
	}
	
	@Override
	public Hitpoint adjustHitpoint(Hitpoint hitpoint)
	{
		boolean isControl = sharedData.getRenderPlane3D().getController().isControl();
		if(unArmed) //The tool only activates, if MWP or moving the cursor off the first connector.
		{
			if(isControl || hitpoint.getHitPart() != firstConnector)
			{
				unArmed = false;
			}
			return hitpoint; //Do not do anything for now.
		}
		
		if(firstConnector == null)
		{
			//Do not change anything anymore. Currently there is no first position, so keep everything as is.
			return hitpoint;
		}
		
		Connector secondConnector = this.secondConnector;
		
		//If looking at a connector, apply that connector:
		if(!hitpoint.isEmpty() && hitpoint.getHitPart() instanceof Connector)
		{
			secondConnector = (Connector) hitpoint.getHitPart();
		}
		
		boolean isMWP = isChoosingSecondGroup || isControl;
		if(secondConnector == this.secondConnector && isMWP == frameData.isMWP())
		{
			//Connector and the MWP mode did not change, thus there is no need to run the following code again.
			return hitpoint;
		}
		
		LinkedList<WireToBeDrawn> newWires = new LinkedList<>();
		List<Connector> groupA = frameData.getGroupA();
		List<Connector> groupB = frameData.getGroupB();
		
		if(isMWP)
		{
			List<Connector> group = new LinkedList<>();
			if(firstConnector == secondConnector)
			{
				group.add(firstConnector);
			}
			else
			{
				group.add(firstConnector);
				if(secondConnector != null)
				{
					Vector3 position = firstConnector.getConnectionPoint();
					Vector3 ray = secondConnector.getConnectionPoint().subtract(position);
					List<CPURaycast.CollectionEntry> result = CPURaycast.collectConnectors(board.getRootBoard(), position, ray.normalize(), ray.length());
					result.sort((a, b) -> (int) ((a.getDistance() - b.getDistance()) * 1000.0));
					for(CPURaycast.CollectionEntry entry : result)
					{
						group.add(entry.getConnector());
					}
				}
			}
			if(isChoosingSecondGroup)
			{
				groupB = group;
				List<Connector> smaller = groupA.size() < groupB.size() ? groupA : groupB;
				List<Connector> bigger = groupA.size() < groupB.size() ? groupB : groupA;
				for(int i = 0; i < smaller.size(); i++)
				{
					WireToBeDrawn wire = WireToBeDrawn.construct(smaller.get(i), bigger.get(i));
					if(wire != null)
					{
						newWires.add(wire);
					}
				}
				Connector last = smaller.get(smaller.size() - 1);
				for(int i = smaller.size(); i < bigger.size(); i++)
				{
					WireToBeDrawn wire = WireToBeDrawn.construct(last, bigger.get(i));
					if(wire != null)
					{
						newWires.add(wire);
					}
				}
			}
			else
			{
				groupA = group;
				//No wires to add.
			}
		}
		else
		{
			//Reset this, in case that the user aborted MWP and went back to SMP:
			groupA = null;
			if(secondConnector != null)
			{
				//Add wire for SWP:
				WireToBeDrawn wire = WireToBeDrawn.construct(firstConnector, secondConnector);
				if(wire != null)
				{
					newWires.add(wire);
				}
				if(newWires.isEmpty())
				{
					secondConnector = null; //Invalid connector. Unset.
				}
			}
		}
		
		this.secondConnector = secondConnector;
		this.frameData = new FrameData(newWires, isMWP, groupA, groupB);
		
		return hitpoint;
	}
	
	@Override
	public boolean mouseRightUp()
	{
		if(unArmed) //If still unarmed, abort. We need to arm first.
		{
			disable();
			return false;
		}
		if(firstConnector == null) //If the first connector is missing, we are in MWP mode before the second stage, one failed to click a connector.
		{
			return false;
		}
		
		FrameData frameData = this.frameData;
		if(frameData.shouldEndTool()) //If SWP or if both groups set
		{
			sharedData.getRenderPlane3D().toolStopInputs();
		}
		
		gpuTasks.add((worldRenderer) -> {
			//Regardless of where the render thread got. Restore the frame when the input interacted:
			this.frameData = frameData; //That way the right data is shown for the time until the next wire follows.
			
			//Check if in MWP mode, check if second group exists, if not
			if(frameData.isMWP())
			{
				if(!frameData.isMWPDone())
				{
					//Prepare data for next iteration:
					this.firstConnector = null;
					this.secondConnector = null;
					this.isChoosingSecondGroup = true;
					return; //Still about to place another group.
				}
			}
			
			List<CompWireRaw> newWires = new LinkedList<>();
			for(WireToBeDrawn wire : frameData.getWires())
			{
				CompWireRaw newWire = new CompWireRaw(board.getPlaceboWireParent());
				newWire.setRotation(wire.getAlignment());
				newWire.setPosition(wire.getPosition());
				newWire.setLength((float) wire.getLength() * 2f);
				newWire.setConnectorA(wire.getA());
				newWire.setConnectorB(wire.getB());
				newWires.add(newWire);
			}
			
			simulation.updateJobNextTickThreadSafe((simulation) -> {
				{
					Iterator<CompWireRaw> it = newWires.iterator();
					while(it.hasNext())
					{
						CompWireRaw newWire = it.next();
						Connector from = newWire.getConnectorA();
						Connector to = newWire.getConnectorB();
						for(Wire wire : from.getWires())
						{
							if(wire.getOtherSide(from) == to)
							{
								if(!frameData.isMWP())
								{
									System.out.println("[WireDrawing] Connectors are already connected, cannot draw wire.");
								}
								it.remove();
							}
						}
						//Wire okay, lets place it:
					}
				}
				
				if(newWires.isEmpty())
				{
					disable();
					return;
				}
				Map<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> updates = new HashMap<>();
				for(CompWireRaw wire : newWires)
				{
					//Places the wires and updates clusters as needed. Also finishes the wire linking.
					ClusterHelper.placeWire(simulation, board, wire.getConnectorA(), wire.getConnectorB(), wire, updates);
				}
				
				//Once it is fully prepared by simulation thread, cause the graphic thread to draw it.
				gpuTasks.add((unused) -> {
					System.out.println("[ClusterUpdateDebug] Updating " + updates.size() + " conductor mesh bags.");
					for(Map.Entry<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> entry : updates.entrySet())
					{
						entry.getKey().handleUpdates(entry.getValue(), board.getSimulation());
					}
					
					for(CompWireRaw wire : newWires)
					{
						sharedData.getRenderPlane3D().clusterChanged(wire.getConnectorA().getCluster());
						sharedData.getRenderPlane3D().clusterChanged(wire.getConnectorB().getCluster());
						//Add the wire to the mesh sources
						board.getWiresToRender().add(wire);
						wireRayCaster.addWire(wire);
						worldMesh.addComponent(wire, board.getSimulation());
					}
					
					worldRenderer.toolDisable();
				});
			});
		});
		return true;
	}
	
	@Override
	public boolean mouseRightDown(Hitpoint hitpoint)
	{
		//The second stage of MWP is starting now:
		if(hitpoint.getHitPart() instanceof Connector)
		{
			gpuTasks.add((worldRenderer) -> {
				this.firstConnector = (Connector) hitpoint.getHitPart();
			});
			return true;
		}
		return false;
	}
	
	private void disable()
	{
		gpuTasks.add((worldRenderer) -> {
			worldRenderer.toolDisable();
		});
	}
	
	@Override
	public void renderWorld(float[] view)
	{
		//Draw wire to be placed:
		for(WireToBeDrawn wire : frameData.getWires())
		{
			Vector3 position = wire.getPosition();
			Quaternion alignment = wire.getAlignment();
			double length = wire.getLength();
			
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
			invisibleCubeShader.setUniformV4(3, wireColor);
			GenericVAO invisibleCube = shaderStorage.getInvisibleCube();
			invisibleCube.use();
			invisibleCube.draw();
		}
	}
	
	@Override
	public void renderOverlay(float[] view)
	{
		if(unArmed)
		{
			return;
		}
		
		if(frameData.isMWP())
		{
			if(frameData.getGroupA() != null) //This one should not be null, but better have it invisible than risk a crash.
			{
				drawOverlay(view, group1, frameData.getGroupA());
			}
			else
			{
				System.out.println("[ERROR] The MWP groupA is null while drawing overlay.");
			}
			if(frameData.getGroupB() != null)
			{
				drawOverlay(view, group2, frameData.getGroupB());
			}
		}
		else //SWP:
		{
			if(firstConnector == null)
			{
				//Currently not drawing a wire.
				return;
			}
			LinkedList<Connector> list = new LinkedList<>();
			list.add(firstConnector);
			drawOverlay(view, group1, list);
			if(secondConnector != null)
			{
				list.clear();
				list.add(secondConnector);
				drawOverlay(view, group2, list);
			}
		}
	}
	
	//### Graphical helpers: ###
	
	private void drawOverlay(float[] view, float[] color, List<Connector> partsToRender)
	{
		//TODO: Eventually optimize the call to these drawing calls:
		
		//Enable drawing to stencil buffer
		GL30.glStencilMask(0xFF);
		
		ShaderProgram invisibleCubeShader = shaderStorage.getInvisibleCubeShader();
		GenericVAO invisibleCube = shaderStorage.getInvisibleCube();
		
		invisibleCubeShader.use();
		invisibleCubeShader.setUniformM4(1, view);
		invisibleCubeShader.setUniformV4(3, new float[]{0, 0, 0, 0});
		
		for(Connector part : partsToRender)
		{
			World3DHelper.drawCubeFull(invisibleCubeShader, invisibleCube, part.getModel(), part, part.getParent().getModelHolder().getPlacementOffset(), new Matrix());
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
	
	//### Classes: ###
	
	//Container for data persistent for one frame.
	private static class FrameData
	{
		private final List<WireToBeDrawn> wires;
		private final boolean isMWP;
		private final List<Connector> groupA;
		private final List<Connector> groupB;
		
		public FrameData(List<WireToBeDrawn> wires, boolean isMWP, List<Connector> connectorsGroupA, List<Connector> connectorsGroupB)
		{
			this.wires = wires;
			this.isMWP = isMWP;
			this.groupA = connectorsGroupA;
			this.groupB = connectorsGroupB;
		}
		
		public boolean shouldEndTool()
		{
			return !isMWP || groupB != null;
		}
		
		public List<WireToBeDrawn> getWires()
		{
			return wires;
		}
		
		public boolean isMWP()
		{
			return isMWP;
		}
		
		public boolean isMWPDone()
		{
			return groupB != null;
		}
		
		public List<Connector> getGroupA()
		{
			return groupA;
		}
		
		public List<Connector> getGroupB()
		{
			return groupB;
		}
	}
	
	private static class WireToBeDrawn
	{
		private final Connector a;
		private final Connector b;
		
		private final Vector3 position;
		private final Quaternion alignment;
		private final double length;
		
		public static WireToBeDrawn construct(Connector a, Connector b)
		{
			if(a == b)
			{
				return null;
			}
			if(a instanceof Blot && b instanceof Blot)
			{
				return null;
			}
			
			Vector3 aPos = a.getConnectionPoint();
			Vector3 bPos = b.getConnectionPoint();
			
			Vector3 direction = bPos.subtract(aPos).divide(2);
			Quaternion alignment = MathHelper.rotationFromVectors(Vector3.zp, direction.normalize());
			if(Double.isNaN(alignment.getA()))
			{
				System.out.println("[WARNING] Cannot place wire, cause start- and end-point are the same... Please try to not abuse OpenTUNG. Ignore stacktrace above and do not report it.");
				return null;
			}
			double length = direction.length();
			Vector3 position = aPos.add(direction);
			
			return new WireToBeDrawn(a, b, position, alignment, length);
		}
		
		public WireToBeDrawn(Connector a, Connector b, Vector3 position, Quaternion alignment, double length)
		{
			this.a = a;
			this.b = b;
			this.position = position;
			this.alignment = alignment;
			this.length = length;
		}
		
		public Connector getA()
		{
			return a;
		}
		
		public Connector getB()
		{
			return b;
		}
		
		public Vector3 getPosition()
		{
			return position;
		}
		
		public Quaternion getAlignment()
		{
			return alignment;
		}
		
		public double getLength()
		{
			return length;
		}
	}
}

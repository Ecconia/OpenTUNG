package de.ecconia.java.opentung.core.tools;

import de.ecconia.java.opentung.components.conductor.Blot;
import de.ecconia.java.opentung.components.conductor.CompWireRaw;
import de.ecconia.java.opentung.components.conductor.Connector;
import de.ecconia.java.opentung.components.meta.Part;
import de.ecconia.java.opentung.core.BoardUniverse;
import de.ecconia.java.opentung.core.data.Hitpoint;
import de.ecconia.java.opentung.core.data.ShaderStorage;
import de.ecconia.java.opentung.core.data.SharedData;
import de.ecconia.java.opentung.core.helper.World3DHelper;
import de.ecconia.java.opentung.core.structs.GPUTask;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import org.lwjgl.opengl.GL30;

public class DrawWire implements Tool
{
	private final SharedData sharedData;
	private final BlockingQueue<GPUTask> gpuTasks;
	private final SimulationManager simulation;
	private final ShaderStorage shaderStorage;
	private final MeshBagContainer worldMesh;
	private final WireRayCaster wireRayCaster;
	
	private Hitpoint hitpoint;
	
	private Connector wireStartPoint;
	private BoardUniverse board;
	
	private boolean unArmed;
	
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
	public Boolean activateMouseDown(Hitpoint hitpoint, int buttonCode, boolean control)
	{
		if(buttonCode != InputProcessor.MOUSE_RIGHT)
		{
			return null;
		}
		
		if(hitpoint.getHitPart() instanceof Connector)
		{
			this.unArmed = true;
			this.hitpoint = hitpoint; //Just to never have hitpoint null.
			this.wireStartPoint = (Connector) hitpoint.getHitPart();
			gpuTasks.add((worldRenderer) -> {
				worldRenderer.toolReady();
			});
			
			//TODO: Accepting if looking at a connector.
			//TODO: remember the first component, and only enable this tool internally, once looking at some other component.
			
			return true;
		}
		return null;
	}
	
	@Override
	public Hitpoint adjustHitpoint(Hitpoint hitpoint)
	{
		this.hitpoint = hitpoint;
		
		if(unArmed)
		{
			if(hitpoint.getHitPart() != wireStartPoint)
			{
				unArmed = false;
			}
		}
		
		if(!hitpoint.isEmpty() && hitpoint.getHitPart() != wireStartPoint)
		{
			Part lookingAt = hitpoint.getHitPart();
			if(lookingAt instanceof Connector)
			{
				Vector3 toPos = ((Connector) lookingAt).getConnectionPoint();
				
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
		}
		else
		{
			hitpoint.setWireData(null, null, 0);
		}
		
		return hitpoint;
	}
	
	@Override
	public boolean mouseRightUp()
	{
		//The tool stops here:
		sharedData.getRenderPlane3D().toolStopInputs();
		
		Hitpoint hitpoint = this.hitpoint; //We are on input thread now, this gets changed by the render thread, thus copy ref.
		Connector from = this.wireStartPoint;
		
		Vector3 position = hitpoint.getWireCenterPosition();
		if(position == null)
		{
			//Released on non-connector. Or human too fast.
			disable();
			return !unArmed; //Return the armed state. Cause the camera might have never looked at something else.
		}
		//Now we have a position, so the camera must have moved of some form.
		
		Connector to = (Connector) hitpoint.getHitPart();
		Quaternion alignment = hitpoint.getWireAlignment();
		double length = hitpoint.getWireDistance();
		
		if(from instanceof Blot && to instanceof Blot)
		{
			System.out.println("Blot-Blot connections are not allowed, cause pointless.");
			disable();
			return true;
		}
		
		for(Wire wire : from.getWires())
		{
			if(wire.getOtherSide(from) == to)
			{
				System.out.println("Already connected.");
				disable();
				return true;
			}
		}
		
		sharedData.getRenderPlane3D().clusterChanged(from.getCluster());
		sharedData.getRenderPlane3D().clusterChanged(to.getCluster());
		
		//Add wire:
		CompWireRaw newWire = new CompWireRaw(board.getPlaceboWireParent());
		newWire.setRotation(alignment);
		newWire.setPosition(position);
		newWire.setLength((float) length * 2f);
		
		simulation.updateJobNextTickThreadSafe((simulation) -> {
			Map<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> updates = new HashMap<>();
			//Places the wires and updates clusters as needed. Also finishes the wire linking.
			ClusterHelper.placeWire(simulation, board, from, to, newWire, updates);
			
			//Once it is fully prepared by simulation thread, cause the graphic thread to draw it.
			gpuTasks.add((worldRenderer) -> {
				System.out.println("[ClusterUpdateDebug] Updating " + updates.size() + " conductor mesh bags.");
				for(Map.Entry<ConductorMeshBag, List<ConductorMeshBag.ConductorMBUpdate>> entry : updates.entrySet())
				{
					entry.getKey().handleUpdates(entry.getValue(), board.getSimulation());
				}
				//Add the wire to the mesh sources
				board.getWiresToRender().add(newWire);
				wireRayCaster.addWire(newWire);
				worldMesh.addComponent(newWire, board.getSimulation());
				
				worldRenderer.toolDisable();
			});
		});
		
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
	
	@Override
	public void renderOverlay(float[] view)
	{
		if(unArmed)
		{
			return;
		}
		
		//Enable drawing to stencil buffer
		GL30.glStencilMask(0xFF);
		
		ShaderProgram invisibleCubeShader = shaderStorage.getInvisibleCubeShader();
		GenericVAO invisibleCube = shaderStorage.getInvisibleCube();
		float[] color = {
				1.0f,
				0.5f,
				0.0f,
				0.7f
		};
		
		invisibleCubeShader.use();
		invisibleCubeShader.setUniformM4(1, view);
		invisibleCubeShader.setUniformV4(3, new float[]{0, 0, 0, 0});
		World3DHelper.drawCubeFull(invisibleCubeShader, invisibleCube, wireStartPoint.getModel(), wireStartPoint, wireStartPoint.getParent().getModelHolder().getPlacementOffset(), new Matrix());
		
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
}

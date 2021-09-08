package de.ecconia.java.opentung.core.tools;

import de.ecconia.java.opentung.components.CompBlotter;
import de.ecconia.java.opentung.components.CompInverter;
import de.ecconia.java.opentung.components.conductor.Blot;
import de.ecconia.java.opentung.components.conductor.CompWireRaw;
import de.ecconia.java.opentung.components.conductor.Peg;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.LogicComponent;
import de.ecconia.java.opentung.components.meta.Part;
import de.ecconia.java.opentung.core.data.Hitpoint;
import de.ecconia.java.opentung.core.data.SharedData;
import de.ecconia.java.opentung.core.structs.GPUTask;
import de.ecconia.java.opentung.inputs.InputProcessor;
import de.ecconia.java.opentung.meshing.MeshBagContainer;
import de.ecconia.java.opentung.raycast.WireRayCaster;
import de.ecconia.java.opentung.simulation.Cluster;
import de.ecconia.java.opentung.simulation.InheritingCluster;
import de.ecconia.java.opentung.simulation.Powerable;
import de.ecconia.java.opentung.simulation.SimulationManager;
import de.ecconia.java.opentung.simulation.SourceCluster;
import de.ecconia.java.opentung.simulation.Wire;
import de.ecconia.java.opentung.util.math.MathHelper;
import de.ecconia.java.opentung.util.math.Quaternion;
import de.ecconia.java.opentung.util.math.Vector3;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class DecoderSwapHelper implements Tool
{
	private final SharedData sharedData;
	private final SimulationManager simulation;
	private final BlockingQueue<GPUTask> gpuTasks;
	
	private final MeshBagContainer worldMesh;
	private final WireRayCaster wireRayCaster;
	
	public DecoderSwapHelper(SharedData sharedData)
	{
		this.sharedData = sharedData;
		
		simulation = sharedData.getBoardUniverse().getSimulation();
		gpuTasks = sharedData.getGpuTasks();
		
		worldMesh = sharedData.getRenderPlane3D().getWorldMesh();
		wireRayCaster = sharedData.getRenderPlane3D().getWireRayCaster();
	}
	
	@Override
	public Boolean activateMouseDown(Hitpoint hitpoint, int buttonCode, boolean control)
	{
		if(buttonCode != InputProcessor.MOUSE_LEFT)
		{
			return null;
		}
		Part lookingAt = hitpoint.getHitPart();
		if(lookingAt == null)
		{
			return null;
		}
//		if(lookingAt instanceof Connector)
//		{
//			lookingAt = lookingAt.getParent();
//		}
		
		if(lookingAt instanceof CompBlotter)
		{
			swap((LogicComponent) lookingAt, true);
			return false;
		}
		else if(lookingAt instanceof CompInverter)
		{
			swap((LogicComponent) lookingAt, false);
			return false;
		}
		else
		{
			return null;
		}
	}
	
	private void swap(LogicComponent oldComponent, boolean toInverter)
	{
		CompContainer parent = (CompContainer) oldComponent.getParent();
		oldComponent.setParent(null);
		
		//New component:
		LogicComponent newComponent = toInverter ? new CompInverter(parent) : new CompBlotter(parent);
		newComponent.setPositionGlobal(oldComponent.getPositionGlobal());
		newComponent.setAlignmentGlobal(oldComponent.getAlignmentGlobal());
//		newComponent.init(); //Not used.
//		newComponent.initClusters(); //Do not initialize clusters, these will be taken over from the old component.
		
		simulation.updateJobNextTickThreadSafe((simulation) -> {
			//Prime other component:
			
			InheritingCluster fakeCluster = new InheritingCluster();
			
			Peg oldInput = oldComponent.getPegs().get(0);
			Blot oldOutput = oldComponent.getBlots().get(0);
			Peg newInput = newComponent.getPegs().get(0);
			Blot newOutput = newComponent.getBlots().get(0);
			
			((SourceCluster) oldOutput.getCluster()).overwriteSource(newOutput);
			
			((Powerable) newComponent).setPowered(0, oldOutput.getCluster().isActive());
			simulation.updateNextTick(newComponent); //Always
			
			List<Wire> wiresToUpdate = new ArrayList<>();
			//Input handling:
			{
				//Transfer the cluster over / Change connector in cluster:
				oldInput.getCluster().remove(oldInput);
				newInput.setCluster(oldInput.getCluster());
				newInput.getCluster().addConnector(newInput);
				oldInput.setCluster(fakeCluster); //In case it will be updated in future.
				//Change the connector for each wire:
				for(Wire wire : oldInput.getWires())
				{
					if(wire.getConnectorA() == oldInput)
					{
						wire.setConnectorA(newInput);
					}
					else
					{
						wire.setConnectorB(newInput);
					}
					newInput.addWire(wire);
					wiresToUpdate.add(wire);
				}
				oldInput.getWires().clear(); //Remove them all.
			}
			//Output handling:
			{
				//Transfer the cluster over / Change connector in cluster:
				oldOutput.getCluster().remove(oldOutput);
				newOutput.setCluster(oldOutput.getCluster());
				newOutput.getCluster().addConnector(newOutput);
				oldOutput.setCluster(fakeCluster);
				//Change the connector for each wire:
				for(Wire wire : oldOutput.getWires())
				{
					if(wire.getConnectorA() == oldOutput)
					{
						wire.setConnectorA(newOutput);
					}
					else
					{
						wire.setConnectorB(newOutput);
					}
					newOutput.addWire(wire);
					wiresToUpdate.add(wire);
				}
				oldOutput.getWires().clear(); //Remove them all.
			}
			
			List<Cluster> modifiedClusters = new ArrayList<>(oldComponent.getConnectors().size());
			modifiedClusters.add(newOutput.getCluster());
			modifiedClusters.add(newInput.getCluster());
			sharedData.getRenderPlane3D().clustersChanged(modifiedClusters);
			
			gpuTasks.add((worldRenderer) -> {
				for(Wire wire : wiresToUpdate)
				{
					CompWireRaw cWire = (CompWireRaw) wire;
					wireRayCaster.removeWire(cWire);
					worldMesh.removeComponent(cWire, simulation);
					
					//Update position and alignment:
					Vector3 thisPos = wire.getConnectorA().getConnectionPoint();
					Vector3 thatPos = wire.getConnectorB().getConnectionPoint();
					
					Vector3 direction = thisPos.subtract(thatPos).divide(2);
					double distance = direction.length();
					Quaternion rotation = MathHelper.rotationFromVectors(Vector3.zp, direction.normalize());
					Vector3 position = thatPos.add(direction);
					
					cWire.setPositionGlobal(position);
					cWire.setAlignmentGlobal(rotation);
					cWire.setLength((float) distance * 2f);
					//End of update.
					
					wireRayCaster.addWire(cWire);
					worldMesh.addComponent(cWire, simulation);
				}
				
				parent.remove(oldComponent);
				parent.addChild(newComponent);
				parent.updateBounds();
				
				worldMesh.removeComponent(oldComponent, simulation);
				worldMesh.addComponent(newComponent, simulation);
				
				worldRenderer.toolDisable();
			});
		});
	}
}

package de.ecconia.java.opentung.core.systems;

import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.CompPeg;
import de.ecconia.java.opentung.components.CompSnappingPeg;
import de.ecconia.java.opentung.components.CompThroughPeg;
import de.ecconia.java.opentung.components.conductor.Blot;
import de.ecconia.java.opentung.components.conductor.CompWireRaw;
import de.ecconia.java.opentung.components.conductor.Connector;
import de.ecconia.java.opentung.components.meta.ConnectedComponent;
import de.ecconia.java.opentung.components.meta.Part;
import de.ecconia.java.opentung.core.data.ShaderStorage;
import de.ecconia.java.opentung.core.data.SharedData;
import de.ecconia.java.opentung.core.helper.World3DHelper;
import de.ecconia.java.opentung.core.structs.GPUTask;
import de.ecconia.java.opentung.libwrap.Matrix;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;
import de.ecconia.java.opentung.settings.Settings;
import de.ecconia.java.opentung.simulation.Cluster;
import de.ecconia.java.opentung.simulation.Clusterable;
import de.ecconia.java.opentung.simulation.HiddenWire;
import de.ecconia.java.opentung.simulation.InheritingCluster;
import de.ecconia.java.opentung.simulation.SimulationManager;
import de.ecconia.java.opentung.simulation.SourceCluster;
import de.ecconia.java.opentung.simulation.Wire;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import org.lwjgl.opengl.GL30;

public class ClusterHighlighter
{
	private final ShaderStorage shaderStorage;
	private final BlockingQueue<GPUTask> gpuTasks;
	private final SimulationManager simulation;
	private final CompBoard rootBoard;
	
	//Internal state:
	private boolean inVisible = false;
	private Clusterable startingPoint;
	private final HashSet<Cluster> affectedClusters = new HashSet<>();
	
	//Collection:
	private List<CompWireRaw> primarySelectionWires;
	private List<Connector> primarySelectionConnectors;
	private List<CompWireRaw> secondarySelectionWires;
	private List<Connector> secondarySelectionConnectors;
	
	public ClusterHighlighter(SharedData sharedData)
	{
		this.shaderStorage = sharedData.getShaderStorage();
		this.gpuTasks = sharedData.getGpuTasks();
		this.simulation = sharedData.getBoardUniverse().getSimulation();
		this.rootBoard = sharedData.getBoardUniverse().getRootBoard();
	}
	
	//Input thread.
	public void componentRightClicked(Part part)
	{
		Clusterable clusterable;
		if(part instanceof CompWireRaw)
		{
			clusterable = (Clusterable) part;
		}
		else if(part instanceof CompThroughPeg || part instanceof CompPeg || part instanceof CompSnappingPeg)
		{
			clusterable = ((ConnectedComponent) part).getPegs().get(0);
		}
		else if(part instanceof Connector)
		{
			clusterable = (Clusterable) part;
		}
		else
		{
			clusterable = null;
		}
		
		if(clusterable != null)
		{
			simulation.updateJobNextTickThreadSafe((simulation) -> {
				if(startingPoint != null && startingPoint.getCluster() == clusterable.getCluster())
				{
					affectedClusters.clear();
					gpuTasks.add(worldRenderer -> {
						cleanup();
					});
				}
				else
				{
					startImport(clusterable);
				}
			});
		}
	}
	
	private void startImport(Clusterable clusterable)
	{
		affectedClusters.clear();
		{
			Part part = (Part) clusterable;
			if(part instanceof CompWireRaw)
			{
				if(part.getParent() == null)
				{
					//Wire deleted.
					gpuTasks.add(worldRenderer -> {
						cleanup();
					});
					return;
				}
			}
			else
			{
				while(part.getParent() != null)
				{
					part = part.getParent();
				}
				if(part != rootBoard)
				{
					//Connector deleted.
					gpuTasks.add(worldRenderer -> {
						cleanup();
					});
					return;
				}
			}
		}
		
		//Collect all wires and connectors of the primary cluster:
		Cluster primaryCluster = clusterable.getCluster();
		affectedClusters.add(primaryCluster);
		List<CompWireRaw> primarySelectionWires = new ArrayList<>(primaryCluster.getWires().size());
		for(Wire wire : primaryCluster.getWires())
		{
			if(wire instanceof HiddenWire)
			{
				continue;
			}
			primarySelectionWires.add((CompWireRaw) wire);
		}
		List<Connector> primarySelectionConnectors = new ArrayList<>(primaryCluster.getConnectors());
		
		
		//Collect from secondary clusters:
		List<CompWireRaw> secondarySelectionWires = new ArrayList<>();
		List<Connector> secondarySelectionConnectors = new ArrayList<>();
		if(primaryCluster instanceof SourceCluster)
		{
			//The secondary group only consists of inheriting clusters.
			// -> Add them fully.
			for(InheritingCluster cluster : ((SourceCluster) primaryCluster).getDrains())
			{
				if(affectedClusters.contains(cluster))
				{
					continue; //Cluster already handled.
				}
				affectedClusters.add(cluster);
				for(Wire wire : cluster.getWires())
				{
					if(wire instanceof HiddenWire)
					{
						continue;
					}
					secondarySelectionWires.add((CompWireRaw) wire);
				}
				secondarySelectionConnectors.addAll(cluster.getConnectors());
			}
		}
		else
		{
			//The secondary group only consists of source clusters.
			// -> Only add the wires for each. (But only once).
			for(SourceCluster cluster : ((InheritingCluster) primaryCluster).getSources())
			{
				if(affectedClusters.contains(cluster))
				{
					continue; //Cluster already handled.
				}
				affectedClusters.add(cluster);
				Blot source = cluster.getSource();
				secondarySelectionConnectors.add(source);
				for(Wire wire : source.getWires())
				{
					if(wire.getOtherSide(source).getCluster() != primaryCluster)
					{
						continue;
					}
					//Not sure if there will ever be a hidden wire connected to a blot though...
					if(wire instanceof HiddenWire)
					{
						continue;
					}
					secondarySelectionWires.add((CompWireRaw) wire);
				}
			}
		}
		
		gpuTasks.add(worldRenderer -> {
			//Cleanup:
			if(startingPoint != null)
			{
				cleanup();
			}
			startingPoint = clusterable;
			this.primarySelectionWires = primarySelectionWires;
			this.primarySelectionConnectors = primarySelectionConnectors;
			this.secondarySelectionWires = secondarySelectionWires;
			this.secondarySelectionConnectors = secondarySelectionConnectors;
		});
	}
	
	//Simulation thread.
	public void clustersChanged(List<Cluster> clusters)
	{
		if(startingPoint == null)
		{
			return; //There is currently nothing highlighted.
		}
		//If null, regenerate cluster-highlighter.
		//Else, check if affected and regenerate if so.
		boolean regenerate = true;
		if(clusters != null)
		{
			regenerate = false;
			for(Cluster cluster : clusters)
			{
				if(affectedClusters.contains(cluster))
				{
					regenerate = true;
					break;
				}
			}
		}
		if(regenerate)
		{
			startImport(startingPoint);
		}
	}
	
	//Simulation thread.
	public void clustersOutOfPlace(List<Cluster> clusters)
	{
		//If null, make cluster-highlighter hidden.
		//Else, check if affected and hide if so.
		boolean hide = true;
		if(clusters != null)
		{
			hide = false;
			for(Cluster cluster : clusters)
			{
				if(affectedClusters.contains(cluster))
				{
					hide = true;
					break;
				}
			}
		}
		if(hide)
		{
			gpuTasks.add((worldRenderer) -> {
				inVisible = true;
			});
		}
	}
	
	//Render thread.
	public void clustersBackInPlace()
	{
		//Set cluster highlighter to visible.
		inVisible = false;
	}
	
	//Input thread.
	public void abortHighlighting()
	{
		simulation.updateJobNextTickThreadSafe((simulation) -> {
			affectedClusters.clear();
			gpuTasks.add(worldRenderer -> {
				cleanup();
			});
		});
	}
	
	//Render thread.
	private void cleanup()
	{
		startingPoint = null;
		//TBI: Does it help the GC to do this, or is it just pointless overhead? Should I reuse the list, although they might just block memory?
		primarySelectionWires.clear();
		primarySelectionConnectors.clear();
		secondarySelectionWires.clear();
		secondarySelectionConnectors.clear();
		primarySelectionWires = null;
		primarySelectionConnectors = null;
		secondarySelectionWires = null;
		secondarySelectionConnectors = null;
	}
	
	public void highlightCluster(float[] view)
	{
		if(startingPoint == null)
		{
			return;
		}
		if(inVisible)
		{
			return;
		}
		
		//TODO: Optimize to not have to enable the overlay modes freshly all the time.
		hightlight(view, primarySelectionConnectors, primarySelectionWires, new float[]{
				Settings.highlightClusterColorR,
				Settings.highlightClusterColorG,
				Settings.highlightClusterColorB,
				Settings.highlightClusterColorA
		});
		hightlight(view, secondarySelectionConnectors, secondarySelectionWires, new float[]{
				0.0f,
				0.6f,
				1.0f,
				0.6f
		});
	}
	
	private void hightlight(float[] view, List<Connector> connectors, List<CompWireRaw> wires, float[] color)
	{
		//Enable drawing to stencil buffer
		GL30.glStencilMask(0xFF);
		
		ShaderProgram invisibleCubeShader = shaderStorage.getInvisibleCubeShader();
		GenericVAO invisibleCube = shaderStorage.getInvisibleCube();
		for(CompWireRaw wire : wires)
		{
			World3DHelper.drawStencilComponent(invisibleCubeShader, invisibleCube, wire, view);
		}
		invisibleCubeShader.use();
		invisibleCubeShader.setUniformM4(1, view);
		invisibleCubeShader.setUniformV4(3, new float[]{0, 0, 0, 0});
		Matrix matrix = new Matrix();
		for(Connector connector : connectors)
		{
			World3DHelper.drawCubeFull(invisibleCubeShader, invisibleCube, connector.getModel(), connector.getParent(), connector.getParent().getModelHolder().getPlacementOffset(), matrix);
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
}

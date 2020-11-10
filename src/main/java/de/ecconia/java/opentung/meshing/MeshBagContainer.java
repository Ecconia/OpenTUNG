package de.ecconia.java.opentung.meshing;

import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.CompSnappingPeg;
import de.ecconia.java.opentung.components.CompSnappingWire;
import de.ecconia.java.opentung.components.conductor.CompWireRaw;
import de.ecconia.java.opentung.components.conductor.Peg;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.core.BoardUniverse;
import de.ecconia.java.opentung.core.ShaderStorage;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.settings.Settings;
import de.ecconia.java.opentung.simulation.SimulationManager;
import de.ecconia.java.opentung.simulation.Wire;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MeshBagContainer
{
	//Max 97536 for colorables per mesh.
	private static final int MAX_VERTICES_PER_BAG = 10000;
	
	private final ShaderStorage shaderStorage;
	private final Set<MeshBag> dirtyMeshBags = new HashSet<>();
	private final Set<ConductorMeshBag> dirtyConductorMeshBags = new HashSet<>();
	
	private final List<MeshBag> boardMeshes = new ArrayList<>();
	private final List<MeshBag> solidMeshes = new ArrayList<>();
	private final List<ColorMeshBag> colorMeshes = new ArrayList<>();
	private final List<ConductorMeshBag> conductorMeshes = new ArrayList<>();
	
	public MeshBagContainer(ShaderStorage shaderStorage)
	{
		this.shaderStorage = shaderStorage;
	}
	
	private MeshBag getFreeBoardMesh(int additionalVertices)
	{
		int limit = MAX_VERTICES_PER_BAG - additionalVertices;
		for(MeshBag bag : boardMeshes)
		{
			if(bag.getVerticesAmount() < limit)
			{
				return bag;
			}
		}
		
		MeshBag newMeshBag = new BoardMeshBag(this);
		boardMeshes.add(newMeshBag);
		return newMeshBag;
	}
	
	private MeshBag getFreeSolidMesh(int additionalVertices)
	{
		int limit = MAX_VERTICES_PER_BAG - additionalVertices;
		for(MeshBag bag : solidMeshes)
		{
			if(bag.getVerticesAmount() < limit)
			{
				return bag;
			}
		}
		
		MeshBag newMeshBag = new SolidMeshBag(this);
		solidMeshes.add(newMeshBag);
		return newMeshBag;
	}
	
	private ColorMeshBag getFreeColorMesh(int additionalVertices)
	{
		int limit = MAX_VERTICES_PER_BAG - additionalVertices;
		for(ColorMeshBag bag : colorMeshes)
		{
			if(bag.getVerticesAmount() < limit)
			{
				return bag;
			}
		}
		
		ColorMeshBag newMeshBag = new ColorMeshBag(this);
		colorMeshes.add(newMeshBag);
		return newMeshBag;
	}
	
	private ConductorMeshBag getFreeConductorMesh(int additionalVertices)
	{
		int limit = MAX_VERTICES_PER_BAG - additionalVertices;
		for(ConductorMeshBag bag : conductorMeshes)
		{
			if(bag.getVerticesAmount() < limit)
			{
				return bag;
			}
		}
		
		ConductorMeshBag newMeshBag = new ConductorMeshBag(this);
		conductorMeshes.add(newMeshBag);
		return newMeshBag;
	}
	
	public void addComponent(Component component, SimulationManager simulation)
	{
		if(component instanceof CompBoard)
		{
			int vertexCount = component.getModelHolder().getSolidVerticesAmount();
			MeshBag bag = getFreeBoardMesh(vertexCount);
			bag.addComponent(component, vertexCount);
			component.setSolidMeshBag(bag);
		}
		else
		{
			//Solid:
			int vertexCount = component.getModelHolder().getSolidVerticesAmount();
			if(vertexCount != 0)
			{
				MeshBag bag = getFreeSolidMesh(vertexCount);
				bag.addComponent(component, vertexCount);
				component.setSolidMeshBag(bag);
			}
			
			//Color:
			vertexCount = component.getModelHolder().getColorVerticesAmount();
			if(vertexCount != 0)
			{
				ColorMeshBag bag = getFreeColorMesh(vertexCount);
				bag.addComponent(component, vertexCount, simulation);
				component.setColorMeshBag(bag);
			}
			
			//Conductor:
			vertexCount = component.getModelHolder().getConductorVerticesAmount();
			if(vertexCount != 0)
			{
				ConductorMeshBag bag = getFreeConductorMesh(vertexCount);
				bag.addComponent(component, vertexCount, simulation);
				component.setConductorMeshBag(bag);
			}
		}
	}
	
	public void removeComponent(Component component, SimulationManager simulation)
	{
		if(component instanceof CompBoard)
		{
			component.getSolidMeshBag().removeComponent(component, component.getModelHolder().getSolidVerticesAmount());
			component.setSolidMeshBag(null);
		}
		else
		{
			//Solid:
			int vertexCount = component.getModelHolder().getSolidVerticesAmount();
			if(vertexCount != 0)
			{
				component.getSolidMeshBag().removeComponent(component, vertexCount);
				component.setSolidMeshBag(null);
			}
			
			//Color:
			vertexCount = component.getModelHolder().getColorVerticesAmount();
			if(vertexCount != 0)
			{
				component.getColorMeshBag().removeComponent(component, vertexCount);
				component.setColorMeshBag(null);
			}
			
			//Conductor:
			vertexCount = component.getModelHolder().getConductorVerticesAmount();
			if(vertexCount != 0)
			{
				component.getConductorMeshBag().removeComponent(component, vertexCount, simulation);
				component.setConductorMeshBag(null);
			}
		}
	}
	
	public void draw(float[] view)
	{
		if(Settings.drawBoards)
		{
			//BoardMeshes:
			shaderStorage.getBoardTexture().activate();
			ShaderProgram boardShader = shaderStorage.getMeshBoardShader();
			boardShader.use();
			boardShader.setUniformM4(1, view);
			boardShader.setUniformM4(2, view);
			for(MeshBag bag : boardMeshes)
			{
				bag.draw();
			}
		}
		
		if(Settings.drawMaterial)
		{
			//SolidMeshes:
			ShaderProgram solidShader = shaderStorage.getMeshSolidShader();
			solidShader.use();
			solidShader.setUniformM4(1, view);
			solidShader.setUniformM4(2, view);
			for(MeshBag bag : solidMeshes)
			{
				bag.draw();
			}
		}
		
		//ColorMeshes:
		ShaderProgram colorShader = shaderStorage.getMeshColorShader();
		colorShader.use();
		colorShader.setUniformM4(1, view);
		colorShader.setUniformM4(3, view);
		for(ColorMeshBag meshBag : colorMeshes)
		{
			colorShader.setUniformArray(2, meshBag.getDataArray());
			meshBag.draw();
		}
		
		//ConductorMeshes:
		ShaderProgram conductorShader = shaderStorage.getMeshConductorShader();
		conductorShader.use();
		conductorShader.setUniformM4(1, view);
		conductorShader.setUniformM4(3, view);
		for(ConductorMeshBag meshBag : conductorMeshes)
		{
			colorShader.setUniformArray(2, meshBag.getDataArray());
			meshBag.draw();
		}
	}
	
	public void rebuildConductorMeshes(SimulationManager simulation)
	{
		System.out.println("Reloading conductor meshes.");
		for(ConductorMeshBag cmb : conductorMeshes)
		{
			cmb.fixInitialLoading(simulation);
		}
		System.out.println("Done.");
	}
	
	public void setup(BoardUniverse board, List<CompWireRaw> wires, SimulationManager simulation)
	{
		addRecursive(board.getRootBoard(), simulation);
		for(CompWireRaw wire : wires)
		{
			addComponent(wire, simulation);
		}
	}
	
	public void addRecursive(Component component, SimulationManager simulation)
	{
		addComponent(component, simulation);
		if(component instanceof CompContainer)
		{
			for(Component child : ((CompContainer) component).getChildren())
			{
				addRecursive(child, simulation);
			}
		}
		else if(component instanceof CompSnappingPeg)
		{
			Peg mainPeg = component.getPegs().get(0);
			for(Wire rWire : mainPeg.getWires())
			{
				if(rWire instanceof CompSnappingWire)
				{
					//Ensure that the wire is only added once (by the snapping peg at side A).
					if(rWire.getConnectorA().equals(mainPeg))
					{
						addComponent((CompSnappingWire) rWire, simulation);
//						MeshBag bag = getFreeSolidMesh(16);
//						bag.addComponent((CompSnappingWire) rWire, 16);
//						((CompWireRaw) rWire).setSolidMeshBag(bag);
					}
					break;
				}
			}
		}
	}
	
	protected void setDirty(MeshBag bag)
	{
		dirtyMeshBags.add(bag);
	}
	
	public void rebuildDirty(SimulationManager simulationManager)
	{
		for(MeshBag bag : dirtyMeshBags)
		{
			bag.rebuild();
		}
		dirtyMeshBags.clear();
		//TODO: No don't check every tick - why would I?
		//Check if simulation needs to trigger updates:
		for(ColorMeshBag bag : colorMeshes)
		{
			bag.refresh(simulationManager);
		}
	}
}

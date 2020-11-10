package de.ecconia.java.opentung.meshing;

import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.CompSnappingPeg;
import de.ecconia.java.opentung.components.CompSnappingWire;
import de.ecconia.java.opentung.components.conductor.CompWireRaw;
import de.ecconia.java.opentung.components.conductor.Peg;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.Meshable;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;
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
			//TODO: Getter for vertex amount on ModelHolder.
			MeshBag bag = getFreeBoardMesh(4 * 6);
			bag.addComponent(component, 4 * 6);
			component.setSolidMeshBag(bag);
		}
		else
		{
			//Solid:
			List<Meshable> meshables = new ArrayList<>();
			ModelHolder modelHolder = component.getModelHolder();
			meshables.addAll(modelHolder.getSolid());
			//Snapping pegs have a colored peg -> thus its solid.
			if(component instanceof CompSnappingPeg)
			{
				meshables.add(component.getModelHolder().getPegModels().get(0));
				//TBI: Is this an okay way to hack in snapping wires?
				{
					Peg mainPeg = component.getPegs().get(0);
					for(Wire rWire : mainPeg.getWires())
					{
						if(rWire instanceof CompSnappingWire)
						{
							//Ensure that the wire is only added once (by the snapping peg at side A).
							if(rWire.getConnectorA().equals(mainPeg))
							{
								MeshBag bag = getFreeSolidMesh(16);
								bag.addComponent((CompSnappingWire) rWire, 16);
								((CompWireRaw) rWire).setSolidMeshBag(bag);
							}
							break;
						}
					}
				}
			}
			if(!meshables.isEmpty())
			{
				int verticesCount = 0;
				//TODO: Improve this crap:
				for(Meshable meshable : meshables)
				{
					verticesCount += ((CubeFull) meshable).getFacesCount() * 4;
				}
				MeshBag bag = getFreeSolidMesh(verticesCount);
				bag.addComponent(component, verticesCount);
				component.setSolidMeshBag(bag);
			}
			
			//Color:
			int colorableAmount = component.getModelHolder().getColorables().size();
			if(colorableAmount != 0)
			{
				int verticesCount = 0;
				for(Meshable meshable : component.getModelHolder().getColorables())
				{
					verticesCount += ((CubeFull) meshable).getFacesCount() * 4;
				}
				ColorMeshBag bag = getFreeColorMesh(verticesCount);
				bag.addComponent(component, verticesCount, simulation);
				component.setColorMeshBag(bag);
			}
			
			//Snapping pegs, may have a peg, but it is classified as solid.
			if(component instanceof CompSnappingPeg)
			{
				return;
			}
			
			//Conductor:
			meshables.clear();
			meshables.addAll(component.getModelHolder().getConductors());
			meshables.addAll(component.getModelHolder().getPegModels());
			meshables.addAll(component.getModelHolder().getBlotModels());
			if(!meshables.isEmpty())
			{
				int verticesCount = 0;
				for(Meshable meshable : meshables)
				{
					verticesCount += ((CubeFull) meshable).getFacesCount() * 4;
				}
				ConductorMeshBag bag = getFreeConductorMesh(verticesCount);
				bag.addComponent(component, verticesCount, simulation);
				component.setConductorMeshBag(bag);
			}
		}
	}
	
	public void removeComponent(Component component, SimulationManager simulation)
	{
		if(component instanceof CompBoard)
		{
			MeshBag associatedMB = component.getSolidMeshBag();
			associatedMB.removeComponent(component, 4 * 6);
		}
		else
		{
			//TODO: OMG!!!! COUNT THE VERTICES ONCE!
			//Solid:
			MeshBag solidMeshBag = component.getSolidMeshBag();
			if(solidMeshBag != null)
			{
				List<Meshable> meshables = new ArrayList<>();
				ModelHolder modelHolder = component.getModelHolder();
				meshables.addAll(modelHolder.getSolid());
				//Snapping pegs have a colored peg -> thus its solid.
				if(component instanceof CompSnappingPeg)
				{
					meshables.add(component.getModelHolder().getPegModels().get(0));
				}
				if(!meshables.isEmpty())
				{
					int verticesCount = 0;
					//TODO: Improve this crap:
					for(Meshable meshable : meshables)
					{
						verticesCount += ((CubeFull) meshable).getFacesCount() * 4;
					}
					component.getSolidMeshBag().removeComponent(component, verticesCount);
					component.setSolidMeshBag(null);
				}
			}
			
			//Color:
			ColorMeshBag colorMeshBag = component.getColorMeshBag();
			if(colorMeshBag != null)
			{
				int verticesCount = 0;
				for(Meshable meshable : component.getModelHolder().getColorables())
				{
					verticesCount += ((CubeFull) meshable).getFacesCount() * 4;
				}
				colorMeshBag.removeComponent(component, verticesCount);
				component.setColorMeshBag(null);
			}
			
			//Conductor:
			ConductorMeshBag conductorMeshBag = component.getConductorMeshBag();
			if(conductorMeshBag != null)
			{
				List<Meshable> meshables = new ArrayList<>();
				meshables.addAll(component.getModelHolder().getConductors());
				meshables.addAll(component.getModelHolder().getPegModels());
				meshables.addAll(component.getModelHolder().getBlotModels());
				if(!meshables.isEmpty())
				{
					int verticesCount = 0;
					for(Meshable meshable : meshables)
					{
						verticesCount += ((CubeFull) meshable).getFacesCount() * 4;
					}
					conductorMeshBag.removeComponent(component, verticesCount, simulation);
					component.setConductorMeshBag(null);
				}
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

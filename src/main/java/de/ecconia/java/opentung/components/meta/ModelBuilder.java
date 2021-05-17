package de.ecconia.java.opentung.components.meta;

import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.CubeTunnel;
import de.ecconia.java.opentung.components.fragments.Meshable;
import de.ecconia.java.opentung.components.fragments.TexturedFace;
import de.ecconia.java.opentung.util.math.Vector3;

public class ModelBuilder
{
	private final ModelHolder modelHolder;
	
	public ModelBuilder()
	{
		modelHolder = new ModelHolder();
	}
	
	public ModelHolder build()
	{
		//No purpose yet - but now there is a wrapper which hides methods.
		return modelHolder;
	}
	
	public ModelBuilder setPlacementOffset(Vector3 placementOffset)
	{
		modelHolder.setPlacementOffset(placementOffset);
		return this;
	}
	
	public ModelBuilder addSolid(Meshable solidMeshable)
	{
		modelHolder.addSolid(solidMeshable);
		return this;
	}
	
	public ModelBuilder addPeg(CubeFull pegModel)
	{
		modelHolder.addPegModel(pegModel);
		return this;
	}
	
	public ModelBuilder addBlot(CubeFull blotModel)
	{
		modelHolder.addBlotModel(blotModel);
		return this;
	}
	
	public ModelBuilder addColorable(Meshable colorableMeshable)
	{
		modelHolder.addColorable(colorableMeshable);
		return this;
	}
	
	public ModelBuilder addTexture(TexturedFace texturedFace)
	{
		modelHolder.addTexture(texturedFace);
		return this;
	}
	
	public ModelBuilder addConductor(CubeTunnel conductorMeshable)
	{
		modelHolder.addConductor(conductorMeshable);
		return this;
	}
	
	public ModelBuilder addColoredPegModel(CubeFull coloredPegModel)
	{
		modelHolder.addColoredPegModel(coloredPegModel);
		return this;
	}
	
	//Placement settings:
	
	public ModelBuilder setMountPlaceable(boolean canBePlacedOnMounts)
	{
		modelHolder.setCanBePlacedOnMounts(canBePlacedOnMounts);
		return this;
	}
	
	public ModelBuilder setBoardPlacementOption(PlacementSettingBoardSquare placementSettingBoardSquare)
	{
		modelHolder.setPlacementSettingBoardSquare(placementSettingBoardSquare);
		return this;
	}
	
	public ModelBuilder setBoardSidePlacementOption(PlacementSettingBoardSide placementSettingBoardSide)
	{
		modelHolder.setPlacementSettingBoardSide(placementSettingBoardSide);
		return this;
	}
}

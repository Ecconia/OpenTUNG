package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.ConnectedComponent;
import de.ecconia.java.opentung.components.meta.ModelBuilder;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.components.meta.PlaceableInfo;
import de.ecconia.java.opentung.components.meta.PlacementSettingBoardSide;
import de.ecconia.java.opentung.components.meta.PlacementSettingBoardSquare;
import de.ecconia.java.opentung.util.math.Vector3;

public class CompPeg extends ConnectedComponent
{
	public static final ModelHolder modelHolder = new ModelBuilder()
			.setPlacementOffset(new Vector3(0.0, +0.15 + 0.075, 0.0))
			.addPeg(new CubeFull(new Vector3(0.0, 0.0, 0.0), new Vector3(0.09, 0.3, 0.09), Color.circuitOFF))
			.setMountPlaceable(true)
			.setBoardSidePlacementOption(PlacementSettingBoardSide.All)
			.setBoardPlacementOption(PlacementSettingBoardSquare.All)
			.build();
	public static final PlaceableInfo info = new PlaceableInfo(modelHolder, "TUNG-Peg", "0.2.6", CompPeg.class, CompPeg::new);
	
	@Override
	public ModelHolder getModelHolder()
	{
		return modelHolder;
	}
	
	@Override
	public PlaceableInfo getInfo()
	{
		return info;
	}
	
	//### Non-Static ###
	
	public CompPeg(CompContainer parent)
	{
		super(parent);
	}
}

package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.conductor.Blot;
import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.CubeOpen;
import de.ecconia.java.opentung.components.fragments.Direction;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Holdable;
import de.ecconia.java.opentung.components.meta.LogicComponent;
import de.ecconia.java.opentung.components.meta.ModelBuilder;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.components.meta.PlaceableInfo;
import de.ecconia.java.opentung.components.meta.PlacementSettingBoardSide;
import de.ecconia.java.opentung.components.meta.PlacementSettingBoardSquare;
import de.ecconia.java.opentung.simulation.Powerable;
import de.ecconia.java.opentung.simulation.SimulationManager;
import de.ecconia.java.opentung.util.math.Vector3;

public class CompButton extends LogicComponent implements Powerable, Holdable
{
	public static final ModelHolder modelHolder = new ModelBuilder()
			.setPlacementOffset(new Vector3(0.0, 0.15f + 0.075f, 0.0))
			.addSolid(new CubeFull(new Vector3(0.0, 0.0, 0.0), new Vector3(0.3, 0.3, 0.3), Color.material))
			.addBlot(new CubeOpen(new Vector3(0.0, 0.0, -0.15 - 0.06), new Vector3(0.15, 0.15, 0.12), Direction.ZPos, Color.circuitOFF))
			//TODO: Open bottom
			.addSolid(new CubeFull(new Vector3(0.0, 0.15 + 0.03, 0.0), new Vector3(0.18, 0.06, 0.18), Color.interactable))
			.setMountPlaceable(true)
			.setBoardSidePlacementOption(PlacementSettingBoardSide.None)
			.setBoardPlacementOption(PlacementSettingBoardSquare.Middle)
			.build();
	public static final PlaceableInfo info = new PlaceableInfo(modelHolder, "TUNG-Button", "0.2.6", CompButton.class, CompButton::new);
	
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
	
	private final Blot output;
	
	public CompButton(CompContainer parent)
	{
		super(parent);
		output = blots.get(0);
	}
	
	private boolean powered;
	
	@Override
	public void setPowered(int index, boolean powered)
	{
		this.powered = powered;
	}
	
	@Override
	public boolean isPowered(int index)
	{
		return powered;
	}
	
	@Override
	public void forceUpdateOutput()
	{
		//The button may not be ON at the time this method gets called.
	}
	
	@Override
	public void update(SimulationManager simulation)
	{
		output.getCluster().update(simulation);
	}
	
	@Override
	public void setHold(boolean hold, SimulationManager simulation)
	{
		powered = hold;
		simulation.updateNextTickThreadSafe(this);
	}
}

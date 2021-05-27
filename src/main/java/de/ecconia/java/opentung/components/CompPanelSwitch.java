package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.conductor.Blot;
import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.CubeOpen;
import de.ecconia.java.opentung.components.fragments.Direction;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.LogicComponent;
import de.ecconia.java.opentung.components.meta.ModelBuilder;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.components.meta.PlaceableInfo;
import de.ecconia.java.opentung.components.meta.PlacementSettingBoardSide;
import de.ecconia.java.opentung.components.meta.PlacementSettingBoardSquare;
import de.ecconia.java.opentung.simulation.Powerable;
import de.ecconia.java.opentung.simulation.SimulationManager;
import de.ecconia.java.opentung.util.math.Vector3;

public class CompPanelSwitch extends LogicComponent implements Powerable
{
	public static final ModelHolder modelHolder = new ModelBuilder()
			.setPlacementOffset(new Vector3(0.0, 0.075, 0.0))
			.addSolid(new CubeFull(new Vector3(0.0, 0.2, 0.0), new Vector3(0.15, 0.207, 0.06), Color.interactable))
			.addSolid(new CubeFull(new Vector3(0.0, 0.05, 0.0), new Vector3(0.3, 0.1, 0.3), Color.material))
			.addSolid(new CubeOpen(new Vector3(0.0, -0.125, 0.0), new Vector3(0.2, 0.25, 0.2), Direction.YPos, Color.material))
			.addBlot(new CubeOpen(new Vector3(0.0, -0.31, 0.0), new Vector3(0.15, 0.12, 0.15), Direction.YPos, Color.circuitOFF))
			.setMountPlaceable(false)
			.setBoardSidePlacementOption(PlacementSettingBoardSide.None)
			.setBoardPlacementOption(PlacementSettingBoardSquare.Middle)
			.build();
	public static final PlaceableInfo info = new PlaceableInfo(modelHolder, "TUNG-PanelSwitch", "0.2.6", CompPanelSwitch.class, CompPanelSwitch::new);
	
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
	
	public CompPanelSwitch(CompContainer parent)
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
		//Default state is off. Only update on ON.
		if(powered)
		{
			output.forceUpdateON();
		}
	}
	
	@Override
	public void update(SimulationManager simulation)
	{
		output.getCluster().update(simulation);
	}
	
	@Override
	public void leftClicked(SimulationManager simulation)
	{
		powered = !powered;
		simulation.updateNextTickThreadSafe(this);
	}
}

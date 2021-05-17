package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.conductor.Blot;
import de.ecconia.java.opentung.components.conductor.Peg;
import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.CubeOpen;
import de.ecconia.java.opentung.components.fragments.Direction;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelBuilder;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.components.meta.PlaceableInfo;
import de.ecconia.java.opentung.components.meta.PlacementSettingBoardSide;
import de.ecconia.java.opentung.components.meta.PlacementSettingBoardSquare;
import de.ecconia.java.opentung.simulation.Powerable;
import de.ecconia.java.opentung.simulation.SimulationManager;
import de.ecconia.java.opentung.simulation.Updateable;
import de.ecconia.java.opentung.util.math.Vector3;

public class CompThroughBlotter extends Component implements Powerable, Updateable
{
	public static final ModelHolder modelHolder = new ModelBuilder()
			.setPlacementOffset(new Vector3(0.0, 0.0, 0.0))
			.addBlot(new CubeOpen(new Vector3(0.0, 0.1625 + 0.06, 0.0), new Vector3(0.15, 0.12, 0.15), Direction.YNeg, Color.circuitOFF))
			.addSolid(new CubeFull(new Vector3(0.0, 0.0, 0.0), new Vector3(0.2, 0.325, 0.2), Color.material))
			.addPeg(new CubeOpen(new Vector3(0.0, -0.1625 - 0.15, 0.0), new Vector3(0.1, 0.3, 0.1), Direction.YPos, Color.circuitOFF))
			.setMountPlaceable(false)
			.setBoardSidePlacementOption(PlacementSettingBoardSide.None)
			.setBoardPlacementOption(PlacementSettingBoardSquare.Middle)
			.build();
	public static final PlaceableInfo info = new PlaceableInfo(modelHolder, "TUNG-ThroughBlotter", "0.2.6", CompThroughBlotter.class, CompThroughBlotter::new);
	
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
	
	private final Peg input;
	private final Blot output;
	
	public CompThroughBlotter(CompContainer parent)
	{
		super(parent);
		input = pegs.get(0);
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
		boolean input = this.input.getCluster().isActive();
		if(powered != input)
		{
			powered = input;
			simulation.updateNextStage(output.getCluster());
		}
	}
}

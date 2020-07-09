package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.PlaceableInfo;
import de.ecconia.java.opentung.components.conductor.Blot;
import de.ecconia.java.opentung.components.conductor.Peg;
import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.CubeOpen;
import de.ecconia.java.opentung.components.fragments.Direction;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.math.Vector3;
import de.ecconia.java.opentung.simulation.Powerable;
import de.ecconia.java.opentung.simulation.SimulationManager;
import de.ecconia.java.opentung.simulation.Updateable;

public class CompThroughBlotter extends Component implements Powerable, Updateable
{
	public static final ModelHolder modelHolder = new ModelHolder();
	public static final PlaceableInfo info = new PlaceableInfo(modelHolder, CompThroughBlotter::new);
	
	static
	{
		modelHolder.setPlacementOffset(new Vector3(0.0, 0.0, 0.0));
		modelHolder.addBlot(new CubeOpen(new Vector3(0.0, 0.1625 + 0.06, 0.0), new Vector3(0.15, 0.12, 0.15), Direction.YNeg));
		modelHolder.addSolid(new CubeFull(new Vector3(0.0, 0.0, 0.0), new Vector3(0.2, 0.325, 0.2), Color.material));
		modelHolder.addPeg(new CubeOpen(new Vector3(0.0, -0.1625 - 0.15, 0.0), new Vector3(0.1, 0.3, 0.1), Direction.YPos));
	}
	
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
	public void setPowered(boolean powered)
	{
		this.powered = powered;
	}
	
	@Override
	public boolean isPowered()
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

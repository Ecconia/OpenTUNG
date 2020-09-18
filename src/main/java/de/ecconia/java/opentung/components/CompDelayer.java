package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.PlaceableInfo;
import de.ecconia.java.opentung.components.conductor.Blot;
import de.ecconia.java.opentung.components.conductor.Peg;
import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.CubeOpenRotated;
import de.ecconia.java.opentung.components.fragments.Direction;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.CustomData;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.math.Quaternion;
import de.ecconia.java.opentung.math.Vector3;
import de.ecconia.java.opentung.util.io.ByteLevelHelper;
import de.ecconia.java.opentung.simulation.Powerable;
import de.ecconia.java.opentung.simulation.SimulationManager;
import de.ecconia.java.opentung.simulation.Updateable;

public class CompDelayer extends Component implements Powerable, Updateable, CustomData
{
	public static final ModelHolder modelHolder = new ModelHolder();
	public static final PlaceableInfo info = new PlaceableInfo(modelHolder, "TUNG-Delayer", "0.2.6", CompDelayer.class, CompDelayer::new);
	
	static
	{
		modelHolder.setPlacementOffset(new Vector3(0.0, 0.15 + 0.075f, 0.0));
		modelHolder.addSolid(new CubeFull(new Vector3(0.0, 0.0, 0.0), new Vector3(0.3, 0.3, 0.3), Color.material));
		//TODO: Positions of the rotated components is more or less wrong.
		modelHolder.addPeg(new CubeOpenRotated(Quaternion.angleAxis(45, Vector3.xp), new Vector3(0.0, 0.285, 0.0), new Vector3(0.09, 0.24, 0.09), Direction.YNeg, Color.circuitOFF));
		modelHolder.addBlot(new CubeOpenRotated(Quaternion.angleAxis(45, Vector3.xn), new Vector3(0.0, 0.195, 0.0), new Vector3(0.15, 0.12, 0.15), Direction.YNeg, Color.circuitOFF));
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
	
	public CompDelayer(CompContainer parent)
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
	
	private int delayCount;
	
	public void setDelayCount(int delayCount)
	{
		this.delayCount = delayCount;
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
		if(delayCount == 9)
		{
			changeOutputState(simulation, true);
		}
		else if(delayCount == 0)
		{
			changeOutputState(simulation, false);
		}
		
		if(input == powered)
		{
			delayCount = input ? 9 : 0;
		}
		else //input != output
		{
			delayCount += input ? 1 : -1;
			simulation.updateNextTick(this);
		}
	}
	
	private void changeOutputState(SimulationManager simulation, boolean state)
	{
		if(powered != state)
		{
			powered = state;
			simulation.updateNextStage(output.getCluster());
		}
	}
	
	//### Save/Load ###
	
	@Override
	public byte[] getCustomData()
	{
		byte[] bytes = new byte[ByteLevelHelper.sizeOfUnsignedInt(delayCount)];
		ByteLevelHelper.writeUnsignedInt(delayCount, bytes, 0);
		return bytes;
	}
}

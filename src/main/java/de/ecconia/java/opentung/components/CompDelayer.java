package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.conductor.Blot;
import de.ecconia.java.opentung.components.conductor.Peg;
import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.CubeOpenRotated;
import de.ecconia.java.opentung.components.fragments.Direction;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.math.Quaternion;
import de.ecconia.java.opentung.math.Vector3;
import de.ecconia.java.opentung.simulation.Powerable;
import de.ecconia.java.opentung.simulation.SimulationManager;
import de.ecconia.java.opentung.simulation.Updateable;

public class CompDelayer extends Component implements Powerable, Updateable
{
	public static final ModelHolder modelHolder = new ModelHolder();
	
	static
	{
		modelHolder.setPlacementOffset(new Vector3(0.0, 0.15 + 0.075f, 0.0));
		modelHolder.addSolid(new CubeFull(new Vector3(0.0, 0.0, 0.0), new Vector3(0.3, 0.3, 0.3), Color.material));
		modelHolder.addPeg(new CubeOpenRotated(Quaternion.angleAxis(45, Vector3.xp), new Vector3(0.0, 0.3, 0.0), new Vector3(0.09, 0.24, 0.09), Direction.YNeg));
		modelHolder.addBlot(new CubeOpenRotated(Quaternion.angleAxis(45, Vector3.xn), new Vector3(0.0, 0.15 + 0.06, 0.0), new Vector3(0.15, 0.12, 0.15), Direction.YNeg));
	}
	
	@Override
	public ModelHolder getModelHolder()
	{
		return modelHolder;
	}
	
	//### Non-Static ###
	
	public CompDelayer(CompContainer parent)
	{
		super(parent);
		inputPeg = pegs.get(0);
		outputBlot = blots.get(0);
	}

	private Peg inputPeg;
	private Blot outputBlot;
	
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
			outputBlot.forceUpdateON();
		}
	}
	
	@Override
	public void update(SimulationManager simulation)
	{
		boolean input = inputPeg.getCluster().isActive();
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
			simulation.mightHaveChanged(outputBlot.getCluster());
		}
	}
}

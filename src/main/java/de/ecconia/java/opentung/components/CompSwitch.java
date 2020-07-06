package de.ecconia.java.opentung.components;

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

public class CompSwitch extends Component implements Powerable, Updateable
{
	public static final ModelHolder modelHolder = new ModelHolder();
	
	static
	{
		modelHolder.setPlacementOffset(new Vector3(0.0, 0.15f + 0.075f, 0.0));
		modelHolder.addSolid(new CubeFull(new Vector3(0.0, 0.0, 0.0), new Vector3(0.3, 0.3, 0.3), Color.material));
		modelHolder.addBlot(new CubeOpen(new Vector3(0.0, 0.0, -0.15 -0.06), new Vector3(0.15, 0.15, 0.12), Direction.ZPos));
		modelHolder.addSolid(new CubeFull(new Vector3(0.0, 0.2, 0.0), new Vector3(0.15, 0.207, 0.06), Color.interactable));
	}
	
	@Override
	public ModelHolder getModelHolder()
	{
		return modelHolder;
	}
	
	//### Non-Static ###
	
	public CompSwitch(CompContainer parent)
	{
		super(parent);
		outputBlot = blots.get(0);
	}

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
		outputBlot.getCluster().update(simulation); //Just forward the problem to the cluster.
	}
	
	@Override
	public void leftClicked(SimulationManager simulation)
	{
		//Flip the internal state.
		powered = !powered; //This can be changed, since the essential check is happening in the cluster.
		//Also tell the simulation unit, that it has to do its job:
		simulation.updateNextTickThreadSafe(this);
	}
}

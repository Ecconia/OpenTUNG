package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.conductor.Blot;
import de.ecconia.java.opentung.components.conductor.Peg;
import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.CubeOpen;
import de.ecconia.java.opentung.components.fragments.Direction;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.Holdable;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.math.Vector3;
import de.ecconia.java.opentung.simulation.Powerable;
import de.ecconia.java.opentung.simulation.SimulationManager;
import de.ecconia.java.opentung.simulation.Updateable;

public class CompPanelButton extends Component implements Powerable, Updateable, Holdable
{
	private static final ModelHolder modelHolder = new ModelHolder();
	
	static
	{
		modelHolder.setPlacementOffset(new Vector3(0.0, 0.0, 0.0));
		modelHolder.addSolid(new CubeOpen(new Vector3(0.0, 0.075 + 0.1 + 0.03, 0.0), new Vector3(0.18, 0.06, 0.18), Direction.YNeg, Color.interactable));
		modelHolder.addSolid(new CubeFull(new Vector3(0.0, 0.075 + 0.05, 0.0), new Vector3(0.3, 0.1, 0.3), Color.material));
		modelHolder.addSolid(new CubeFull(new Vector3(0.0, 0.075 - 0.125, 0.0), new Vector3(0.2, 0.25, 0.2), Color.material));
		modelHolder.addBlot(new CubeOpen(new Vector3(0.0, -0.075 - 0.1 - 0.06, 0.0), new Vector3(0.15, 0.12, 0.15), Direction.YPos));
	}
	
	@Override
	public ModelHolder getModelHolder()
	{
		return modelHolder;
	}
	
	//### Non-Static ###
	
	public CompPanelButton(CompContainer parent)
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
		outputBlot.getCluster().update(simulation);
	}
	
	@Override
	public void setHold(boolean hold, SimulationManager simulation)
	{
		powered = hold;
		simulation.updateNextTickThreadSafe(this);
	}
}

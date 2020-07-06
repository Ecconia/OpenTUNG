package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.conductor.Blot;
import de.ecconia.java.opentung.components.conductor.Peg;
import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.CubeOpen;
import de.ecconia.java.opentung.components.fragments.Direction;
import de.ecconia.java.opentung.components.meta.Colorable;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.math.Vector3;
import de.ecconia.java.opentung.simulation.SimulationManager;
import de.ecconia.java.opentung.simulation.Updateable;

public class CompNoisemaker extends Component implements Updateable, Colorable
{
	public static final ModelHolder modelHolder = new ModelHolder();
	
	static
	{
		modelHolder.setPlacementOffset(new Vector3(0.0, 0.075, 0.0));
		modelHolder.addColorable(new CubeFull(new Vector3(0.0, 0.36 + 0.3, 0.0), new Vector3(0.24, 0.6, 0.24), Color.noisemakerOFF));
		modelHolder.addPeg(new CubeOpen(new Vector3(0.0, 0.18, 0.0), new Vector3(0.1, 0.36, 0.1), Direction.YPos));
	}
	
	@Override
	public ModelHolder getModelHolder()
	{
		return modelHolder;
	}
	
	//### Non-Static ###
	
	public CompNoisemaker(CompContainer parent)
	{
		super(parent);
		inputPeg = pegs.get(0);
	}

	private Peg inputPeg;
	
	public void setFrequency(float frequency)
	{
	}
	
	@Override
	public void update(SimulationManager simulation)
	{
		boolean on = inputPeg.getCluster().isActive();
		simulation.setColor(colorID, on ? Color.noisemakerON : Color.noisemakerOFF);
	}
	
	private int colorID;
	
	@Override
	public void setColorID(int id, int colorID)
	{
		this.colorID = colorID;
	}
}

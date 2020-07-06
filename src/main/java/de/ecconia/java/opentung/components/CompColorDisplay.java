package de.ecconia.java.opentung.components;

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

public class CompColorDisplay extends Component implements Updateable, Colorable
{
	public static final ModelHolder modelHolder = new ModelHolder();
	
	static
	{
		modelHolder.setPlacementOffset(new Vector3(0.0, 0.075, 0.0));
		modelHolder.addColorable(new CubeFull(new Vector3(0.0, 0.48 + 0.15, 0.0), new Vector3(0.3, 0.3, 0.3), Color.displayOff));
		modelHolder.addPeg(new CubeOpen(new Vector3(0.0, 0.48 - 0.09, 0.1), new Vector3(0.1, 0.18, 0.1), Direction.YPos));
		modelHolder.addPeg(new CubeOpen(new Vector3(0.0, 0.48 - 0.15, 0.0), new Vector3(0.1, 0.3, 0.1), Direction.YPos));
		modelHolder.addPeg(new CubeOpen(new Vector3(0.0, 0.24, -0.1), new Vector3(0.1, 0.48, 0.1), Direction.YPos));
	}
	
	@Override
	public ModelHolder getModelHolder()
	{
		return modelHolder;
	}
	
	//### Non-Static ###
	
	private final Peg inputSmall;
	private final Peg inputMedium;
	private final Peg inputLong;
	
	public CompColorDisplay(CompContainer parent)
	{
		super(parent);
		inputSmall = pegs.get(0);
		inputMedium = pegs.get(1);
		inputLong = pegs.get(2);
	}
	
	@Override
	public void update(SimulationManager simulation)
	{
		int colorIndex = 0;
		if(inputSmall.getCluster().isActive())
		{
			colorIndex |= 1;
		}
		if(inputMedium.getCluster().isActive())
		{
			colorIndex |= 2;
		}
		if(inputLong.getCluster().isActive())
		{
			colorIndex |= 4;
		}
		
		Color color = Color.byColorDisplayIndex(colorIndex);
		simulation.setColor(colorID, color);
	}
	
	private int colorID;
	
	@Override
	public void setColorID(int id, int colorID)
	{
		this.colorID = colorID;
	}
}

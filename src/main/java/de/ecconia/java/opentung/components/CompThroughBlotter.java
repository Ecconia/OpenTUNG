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

public class CompThroughBlotter extends Component implements Powerable
{
	private static final ModelHolder modelHolder = new ModelHolder();
	
	static
	{
		modelHolder.setPlacementOffset(new Vector3(0.0, 0.0, 0.0));
		modelHolder.addBlot(new CubeOpen(new Vector3(0.0, 0.1625 + 0.06, 0.0), new Vector3(0.15, 0.12, 0.15), Direction.YNeg));
		modelHolder.addSolid(new CubeFull(new Vector3(0.0, 0.0, 0.0), new Vector3(0.2, 0.325, 0.2), Color.material));
		modelHolder.addPeg(new CubeOpen(new Vector3(0.0, -0.1625 - 0.15, 0.0), new Vector3(0.1, 0.3, 0.1), Direction.YPos));
	}
	
	public static void initGL()
	{
		modelHolder.generateTestModel(ModelHolder.TestModelType.Simple);
	}
	
	@Override
	public ModelHolder getModelHolder()
	{
		return modelHolder;
	}
	
	//### Non-Static ###
	
	public CompThroughBlotter(CompContainer parent)
	{
		super(parent);
		pegs.add(new Peg(this, getModelHolder().getPegModels().get(0)));
		blots.add(new Blot(this, getModelHolder().getBlotModels().get(0)));
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
			Blot blot = blots.get(0);
			blot.forceUpdateON();
		}
	}
}
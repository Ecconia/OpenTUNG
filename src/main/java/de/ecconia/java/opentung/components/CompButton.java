package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.conductor.Blot;
import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.CubeOpen;
import de.ecconia.java.opentung.components.fragments.Direction;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.math.Vector3;
import de.ecconia.java.opentung.simulation.Powerable;

public class CompButton extends Component implements Powerable
{
	public static final ModelHolder modelHolder = new ModelHolder();
	
	static
	{
		modelHolder.setPlacementOffset(new Vector3(0.0, 0.15f + 0.075f, 0.0));
		modelHolder.addSolid(new CubeFull(new Vector3(0.0, 0.0, 0.0), new Vector3(0.3, 0.3, 0.3), Color.material));
		modelHolder.addBlot(new CubeOpen(new Vector3(0.0, 0.0, -0.15 -0.06), new Vector3(0.15, 0.15, 0.12), Direction.ZPos));
		//TODO: Open bottom
		modelHolder.addSolid(new CubeFull(new Vector3(0.0, 0.15 + 0.03, 0.0), new Vector3(0.18, 0.06, 0.18), Color.interactable));
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
	
	public CompButton(CompContainer parent)
	{
		super(parent);
		for(CubeFull cube : getModelHolder().getBlotModels())
		{
			blots.add(new Blot(this, cube));
		}
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
		//The button may not be ON at the time this method gets called.
	}
}

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

public class CompSwitch extends Component
{
	public static final ModelHolder modelHolder = new ModelHolder();
	
	static
	{
		modelHolder.setPlacementOffset(new Vector3(0.0, 0.15, 0.0));
		modelHolder.addSolid(new CubeFull(new Vector3(0.0, 0.0, 0.0), new Vector3(0.3, 0.3, 0.3), Color.material));
		modelHolder.addBlot(new CubeOpen(new Vector3(0.0, 0.0, -0.15 -0.06), new Vector3(0.15, 0.15, 0.12), Direction.ZPos));
		modelHolder.addSolid(new CubeFull(new Vector3(0.0, 0.2, 0.0), new Vector3(0.15, 0.207, 0.06), Color.interactable));
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
	
	public CompSwitch(CompContainer parent)
	{
		super(parent);
		for(CubeFull cube : getModelHolder().getBlotModels())
		{
			blots.add(new Blot(this, cube));
		}
	}
}

package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.conductor.Peg;
import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.CubeOpen;
import de.ecconia.java.opentung.components.fragments.Direction;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.math.Vector3;
import de.ecconia.java.opentung.simulation.SimulationManager;
import de.ecconia.java.opentung.simulation.Updateable;

public class CompPanelColorDisplay extends Component implements Updateable
{
	public static final Color offColor = Color.rgb(32, 32, 32);
	
	public static final ModelHolder modelHolder = new ModelHolder();
	
	static
	{
		modelHolder.setPlacementOffset(new Vector3(0.0, 0.0, 0.0));
		modelHolder.addSolid(new CubeFull(new Vector3(0.0, 0.075 + 0.05, 0.0), new Vector3(0.3, 0.1, 0.3), offColor));
		//Lets cheat a bit, to prevent z-Buffer-Fighting:
		modelHolder.addSolid(new CubeOpen(new Vector3(0.0, 0.075 - 0.125, 0.0), new Vector3(0.2, 0.1 + 0.15, 0.299), Direction.YPos, Color.material));
		modelHolder.addPeg(new CubeOpen(new Vector3(0.0, -0.075 - 0.1 - 0.06, 0.1), new Vector3(0.1, 0.12, 0.1), Direction.YPos));
		modelHolder.addPeg(new CubeOpen(new Vector3(0.0, -0.075 - 0.1 - 0.105, 0.0), new Vector3(0.1, 0.21, 0.1), Direction.YPos));
		modelHolder.addPeg(new CubeOpen(new Vector3(0.0, -0.075 - 0.1 - 0.15, -0.1), new Vector3(0.1, 0.3, 0.1), Direction.YPos));
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
	
	public CompPanelColorDisplay(CompContainer parent)
	{
		super(parent);
		for(CubeFull cube : getModelHolder().getPegModels())
		{
			pegs.add(new Peg(this, cube));
		}
	}
	
	@Override
	public void update(SimulationManager simulation)
	{
	
	}
}

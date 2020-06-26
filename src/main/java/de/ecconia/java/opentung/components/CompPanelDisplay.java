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

public class CompPanelDisplay extends Component implements Updateable
{
	public static final Color offColor = Color.rgb(32, 32, 32);
	
	public static final ModelHolder modelHolder = new ModelHolder();
	
	static
	{
		modelHolder.setPlacementOffset(new Vector3(0.0, 0.0, 0.0));
		modelHolder.addSolid(new CubeFull(new Vector3(0.0, 0.075 + 0.05, 0.0), new Vector3(0.3, 0.1, 0.3), offColor));
		modelHolder.addSolid(new CubeOpen(new Vector3(0.0, 0.075 - 0.125, 0.0), new Vector3(0.2, 0.1 + 0.15, 0.2), Direction.YPos, Color.material));
		modelHolder.addPeg(new CubeOpen(new Vector3(0.0, -0.075 - 0.1 - 0.15, 0.0), new Vector3(0.1, 0.3, 0.1), Direction.YPos));
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
	
	private Vector3 colorRaw;
	
	public CompPanelDisplay(CompContainer parent)
	{
		super(parent);
		for(CubeFull cube : getModelHolder().getPegModels())
		{
			pegs.add(new Peg(this, cube));
		}
	}
	
	public void setColorRaw(Vector3 colorRaw)
	{
		this.colorRaw = colorRaw;
	}
	
	public Vector3 getColorRaw()
	{
		return colorRaw;
	}
	
	@Override
	public void update(SimulationManager simulation)
	{
	}
}

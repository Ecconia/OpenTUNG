package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.CubeOpen;
import de.ecconia.java.opentung.components.fragments.Direction;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.math.Vector3;

public class CompThroughPeg extends Component
{
	private static final ModelHolder modelHolder = new ModelHolder();
	
	static
	{
		modelHolder.setPlacementOffset(new Vector3(0.0, 0.0, 0.0));
		modelHolder.addSolid(new CubeFull(new Vector3(0.0, 0.0, 0.0), new Vector3(0.2, +0.15 +0.175, 0.2), Color.material));
		modelHolder.addConnector(new CubeOpen(new Vector3(0.0, +0.075 +0.0875 +0.15, 0.0), new Vector3(0.1, 0.3, 0.1), Direction.YNeg));
		modelHolder.addConnector(new CubeOpen(new Vector3(0.0, -0.075 -0.0875 -0.15, 0.0), new Vector3(0.1, 0.3, 0.1), Direction.YPos));
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
	
	public CompThroughPeg(CompContainer parent)
	{
		super(parent);
	}
}

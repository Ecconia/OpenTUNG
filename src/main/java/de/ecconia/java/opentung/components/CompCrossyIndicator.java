package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.Line;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.math.Quaternion;
import de.ecconia.java.opentung.math.Vector3;

public class CompCrossyIndicator extends Component
{
	public static final ModelHolder modelHolder = new ModelHolder();
	
	static
	{
		modelHolder.setPlacementOffset(new Vector3(0.0, 0.0, 0.0));
		Color c = new Color(0.2, 0.2, 1.0);
		modelHolder.addSolid(new Line(new Vector3(-0.3, +0.0, +0.0), new Vector3(+0.3, +0.0, +0.0), c));
		modelHolder.addSolid(new Line(new Vector3(+0.0, -0.3, +0.0), new Vector3(+0.0, +0.3, +0.0), c));
		modelHolder.addSolid(new Line(new Vector3(+0.0, +0.0, -0.3), new Vector3(+0.0, +0.0, +0.3), c));
	}
	
	public static void initGL()
	{
		modelHolder.generateTestModel(ModelHolder.TestModelType.Line);
	}
	
	@Override
	public ModelHolder getModelHolder()
	{
		return modelHolder;
	}
	
	//### Non-Static ###
	
	public CompCrossyIndicator(Vector3 position)
	{
		super(null);
		setPosition(position);
		setRotation(Quaternion.angleAxis(0, Vector3.yp));
	}
}

package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.math.Quaternion;
import de.ecconia.java.opentung.math.Vector3;
import de.ecconia.java.opentung.models.CrossyIndicatorModel;
import de.ecconia.java.opentung.models.GenericModel;

public class CompCrossyIndicator extends Component
{
	public static CrossyIndicatorModel model;
	
	public static void initGL()
	{
		model = new CrossyIndicatorModel();
	}
	
	public CompCrossyIndicator(Vector3 position)
	{
		super(null);
		setPosition(position);
		setRotation(Quaternion.angleAxis(0, Vector3.yp));
	}
	
	@Override
	public GenericModel getModel()
	{
		return model;
	}
}

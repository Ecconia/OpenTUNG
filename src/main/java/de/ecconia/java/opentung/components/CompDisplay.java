package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.math.Vector3;

public class CompDisplay extends Component
{
	public static final Vector3 offColor = new Vector3(32f / 255f, 32f / 255f, 32f / 255f);
	private Vector3 colorRaw;
	
	public CompDisplay(CompContainer parent)
	{
		super(parent);
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
	public ModelHolder getModelHolder()
	{
		throw new RuntimeException("No Model for this component yet.");
	}
}

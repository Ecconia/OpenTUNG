package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.math.Vector3;

public class CompPeg extends Component
{
	public static final ModelHolder modelHolder = new ModelHolder();
	
	static
	{
		modelHolder.setPlacementOffset(new Vector3(0.0, +0.15 + 0.075, 0.0));
		modelHolder.addPeg(new CubeFull(new Vector3(0.0, 0.0, 0.0), new Vector3(0.09, 0.3, 0.09), null));
	}
	
	@Override
	public ModelHolder getModelHolder()
	{
		return modelHolder;
	}
	
	//### Non-Static ###
	
	public CompPeg(CompContainer parent)
	{
		super(parent);
	}
}

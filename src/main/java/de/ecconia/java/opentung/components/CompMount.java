package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.ModelBuilder;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.components.meta.PlaceableInfo;
import de.ecconia.java.opentung.util.math.Vector3;

public class CompMount extends CompContainer
{
	public static final double MOUNT_HEIGHT = 0.65;
	
	public static final ModelHolder modelHolder = new ModelBuilder()
			.setPlacementOffset(new Vector3(0.0, 0.075 + 0.325, 0.0))
			.addSolid(new CubeFull(new Vector3(0.0, 0.0, 0.0), new Vector3(0.15, 0.65, 0.1), Color.material))
			.build();
	public static final PlaceableInfo info = new PlaceableInfo(modelHolder, "TUNG-Mount", "0.2.6", CompMount.class, CompMount::new);
	
	@Override
	public ModelHolder getModelHolder()
	{
		return modelHolder;
	}
	
	@Override
	public PlaceableInfo getInfo()
	{
		return info;
	}
	
	//### Non-Static ###
	
	public CompMount(CompContainer parent)
	{
		super(parent);
	}
}

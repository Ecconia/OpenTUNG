package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.meta.PlaceableInfo;
import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.Direction;
import de.ecconia.java.opentung.components.fragments.TexturedFace;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.util.math.Vector3;

public class CompPanelLabel extends CompLabel
{
	public static final ModelHolder modelHolder = new ModelHolder();
	public static final PlaceableInfo info = new PlaceableInfo(modelHolder, "TUNG-PanelLabel", "0.2.6", CompPanelLabel.class, CompPanelLabel::new);
	
	static
	{
		modelHolder.setPlacementOffset(new Vector3(0.0, 0.05 + 0.075, 0.0));
		modelHolder.addSolid(new CubeFull(new Vector3(0.0, 0.0, 0.0), new Vector3(0.3, 0.1, 0.3), Color.material));
		modelHolder.addTexture(new TexturedFace(new Vector3(0.0, 0.0, 0.0), new Vector3(0.3, 0.1, 0.3), Direction.YPos));
	}
	
	public static void initGL()
	{
		modelHolder.generateTextureVAO();
	}
	
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
	
	public CompPanelLabel(CompContainer parent)
	{
		super(parent);
	}
}

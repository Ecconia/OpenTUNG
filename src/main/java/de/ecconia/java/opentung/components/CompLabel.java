package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.PlaceableInfo;
import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.Direction;
import de.ecconia.java.opentung.components.fragments.TexturedFace;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.libwrap.LabelTextureWrapper;
import de.ecconia.java.opentung.math.Vector3;

public class CompLabel extends Component
{
	public static final ModelHolder modelHolder = new ModelHolder();
	public static final PlaceableInfo info = new PlaceableInfo(modelHolder, CompLabel::new);
	
	static
	{
		modelHolder.setPlacementOffset(new Vector3(0.0, 0.15 + 0.075, 0.0));
		modelHolder.addSolid(new CubeFull(new Vector3(0.0, 0.0, 0.0), new Vector3(0.3, 0.3, 0.3), Color.material));
		modelHolder.addTexture(new TexturedFace(new Vector3(0.0, 0.0, 0.0), new Vector3(0.3, 0.3, 0.3), Direction.YPos));
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
	
	private String text;
	private float fontSize;
	
	private LabelTextureWrapper texture;
	
	public CompLabel(CompContainer parent)
	{
		super(parent);
	}
	
	public void setText(String text)
	{
		this.text = text;
	}
	
	public void setFontSize(float fontSize)
	{
		this.fontSize = fontSize;
	}
	
	public String getText()
	{
		return text;
	}
	
	public float getFontSize()
	{
		return fontSize;
	}
	
	public void activate()
	{
		texture.activate();
	}
	
	public void setTexture(LabelTextureWrapper texture)
	{
		this.texture = texture;
	}
}

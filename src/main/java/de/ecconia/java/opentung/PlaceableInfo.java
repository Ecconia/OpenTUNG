package de.ecconia.java.opentung;

import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.libwrap.TextureWrapper;
import java.awt.image.BufferedImage;

public class PlaceableInfo
{
	private final ModelHolder model;
	private final CompGenerator generator;
	private final String name;
	//TODO: Prefer texture atlas for icons. Less CPU work.
	private TextureWrapper iconTexture;
	
	public PlaceableInfo(ModelHolder model, String name, CompGenerator generator)
	{
		this.model = model;
		this.generator = generator;
		this.name = name;
	}
	
	public String getName()
	{
		return name;
	}
	
	public void updateIconTexture(BufferedImage image)
	{
		if(iconTexture != null)
		{
			iconTexture.unload();
		}
		
		iconTexture = new TextureWrapper(image, true);
	}
	
	public TextureWrapper getIconTexture()
	{
		return iconTexture;
	}
	
	public ModelHolder getModel()
	{
		return model;
	}
	
	public Component instance(CompContainer container)
	{
		return generator.generateComponent(container);
	}
	
	public interface CompGenerator
	{
		Component generateComponent(CompContainer parent);
	}
}

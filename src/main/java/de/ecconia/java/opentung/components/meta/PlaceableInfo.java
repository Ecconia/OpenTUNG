package de.ecconia.java.opentung.components.meta;

import de.ecconia.java.opentung.libwrap.TextureWrapper;
import java.awt.image.BufferedImage;

public class PlaceableInfo
{
	private final ModelHolder model;
	private final CompGenerator generator;
	private final String name;
	private final boolean hasCustomData;
	private final String version;
	
	//TODO: Prefer texture atlas for icons. Less CPU work.
	private TextureWrapper iconTexture;
	
	public PlaceableInfo(ModelHolder model, String name, String version, Class<? extends Component> clazz, CompGenerator generator)
	{
		this.model = model;
		this.generator = generator;
		this.name = name;
		this.version = version;
		
		hasCustomData = CustomData.class.isAssignableFrom(clazz);
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getVersion()
	{
		return version;
	}
	
	public void updateIconTexture(BufferedImage image)
	{
		if(iconTexture != null)
		{
			iconTexture.unload();
		}
		
		iconTexture = TextureWrapper.createComponentIconTexture(image);
	}
	
	public boolean hasCustomData()
	{
		return hasCustomData;
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

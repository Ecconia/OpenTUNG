package de.ecconia.java.opentung.libwrap;

import java.awt.image.BufferedImage;
import org.lwjgl.opengl.GL30;

public class LabelTextureWrapper extends TextureWrapper
{
	public static TextureWrapper createLabelTexture(BufferedImage image, Integer usages)
	{
		{
			image = SDF2.start(image);
			//Scale image:
//			if(image.getWidth() != 256)
//			{
//				BufferedImage imageScaled = new BufferedImage(256, 256, image.getType());
//				Graphics g = imageScaled.getGraphics();
//				g.drawImage(image, 0, 0, 256, 256, null);
//				g.dispose();
//
//				image = imageScaled;
//			}
		}
		//Far = GL_LINEAR_MIPMAP_LINEAR <- Whatever was commented out.
		return new LabelTextureWrapper(image, usages, ColorInput.Binary, GL30.GL_REPEAT, GL30.GL_LINEAR, GL30.GL_LINEAR);
	}
	
	//If null, this texture cannot be unloaded.
	private Integer usages;
	
	private LabelTextureWrapper(BufferedImage image, Integer usages, ColorInput input, int wrapTypeGL, int nearFilterGL, int farFilterGL)
	{
		super(image, input, wrapTypeGL, nearFilterGL, farFilterGL);
		this.usages = usages;
	}
	
	@Override
	public void unload()
	{
		if(usages == null)
		{
			return; //Texture cannot be unloaded.
		}
		
		usages--;
		if(usages != 0)
		{
			return;
		}
		
		super.unload();
	}
	
	//TODO: This should be called from the render thread too... 'usages' is not thread-safe. (Or just wait, until the refactoring for text is done).
	@Override
	public TextureWrapper copy()
	{
		if(usages == null)
		{
			//Because this is the loading texture...
			System.out.println("[LABEL-COPY/WARNING] You copied a board, while a label on it was not generated yet, please wait a bit and try again.");
			return this;
		}
		usages++;
		return this;
	}
}

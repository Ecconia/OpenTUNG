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
	
	private Integer usages;
	
	private LabelTextureWrapper(BufferedImage image, Integer usages, ColorInput input, int wrapTypeGL, int nearFilterGL, int farFilterGL)
	{
		super(image, input, wrapTypeGL, nearFilterGL, farFilterGL);
		this.usages = usages;
	}
	
	@Override
	public void upload()
	{
		if(usages != null && usages == 0)
		{
			//The Labels using this texture was already deleted. Do not upload this texture.
			return;
		}
		super.upload();
		//TBI: While uploading the id is already set, so there is a small time window where it could be deleted while it is still uploading. Handle that!
	}
	
	@Override
	public void unload()
	{
		if(usages != null)
		{
			usages--;
			if(usages != 0)
			{
				return;
			}
		}
		
		if(id == 0)
		{
			System.out.println("WARNING: Deleted texture before it was fully uploaded to GPU. Might cause it to not being unloaded properly.");
			return;
		}
		GL30.glDeleteTextures(id);
	}
	
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

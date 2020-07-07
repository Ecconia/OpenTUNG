package de.ecconia.java.opentung.libwrap;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public class LabelTextureWrapper
{
	private ByteBuffer buffer;
	private final int width;
	private final int height;
	
	private int id;
	
	public LabelTextureWrapper(BufferedImage image)
	{
		{
			image = SDF1.start(image);
			
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
		
		width = image.getWidth();
		height = image.getHeight();
		int[] pixels = new int[width * height];
		image.getRGB(0, 0, width, height, pixels, 0, width);
		buffer = BufferUtils.createByteBuffer(width * height);
		for(int y = 0; y < height; y++)
		{
			for(int x = 0; x < width; x++)
			{
				int pixel = pixels[y * width + x];
				buffer.put((byte) (pixel & 0xFF));    // Alpha component
			}
		}
		buffer.flip();
	}
	
	public void activate()
	{
		GL30.glBindTexture(GL30.GL_TEXTURE_2D, id);
	}
	
	public void upload()
	{
		id = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL30.GL_REPEAT);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL30.GL_REPEAT);
//		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		//Anisotropic filtering: TODO: Check if available
//		GL11.glTexParameterf(GL11.GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, 2.5f);
//		System.out.println(GL.getCapabilities().GL_EXT_texture_filter_anisotropic);
		
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RED, width, height, 0, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, buffer);
		GL30.glGenerateMipmap(GL30.GL_TEXTURE_2D);
		
		buffer = null;
	}
}

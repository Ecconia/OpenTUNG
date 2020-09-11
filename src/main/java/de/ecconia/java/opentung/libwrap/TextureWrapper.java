package de.ecconia.java.opentung.libwrap;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL30;

public class TextureWrapper
{
	public enum ColorInput
	{
		Binary(1, GL30.GL_RED),
		Grayscale(1, GL30.GL_RED),
		RGB(3, GL30.GL_RGB),
		RGBA(4, GL30.GL_RGBA);
		
		private final int arguments;
		private final int glType;
		
		ColorInput(int arguments, int glType)
		{
			this.arguments = arguments;
			this.glType = glType;
		}
		
		public int getArguments()
		{
			return arguments;
		}
		
		public int getGlType()
		{
			return glType;
		}
	}
	
	private final int wrapTypeGL;
	private final int nearFilterGL;
	private final int farFilterGL;
	private final ColorInput input;
	private final int width;
	private final int height;
	
	protected int id;
	private ByteBuffer buffer;
	
	public static TextureWrapper createFontAtlasTexture(BufferedImage image)
	{
		TextureWrapper texture = new TextureWrapper(image, ColorInput.Grayscale, GL30.GL_CLAMP_TO_EDGE, GL30.GL_LINEAR, GL30.GL_LINEAR);
		texture.upload();
		return texture;
	}
	
	public static TextureWrapper createComponentIconTexture(BufferedImage image)
	{
		TextureWrapper texture = new TextureWrapper(image, ColorInput.RGBA, GL30.GL_CLAMP_TO_EDGE, GL30.GL_NEAREST, GL30.GL_NEAREST);
		texture.upload();
		return texture;
	}
	
	public static TextureWrapper createBoardTexture(BufferedImage image)
	{
		TextureWrapper texture = new TextureWrapper(image, ColorInput.RGB, GL30.GL_REPEAT, GL30.GL_NEAREST, GL30.GL_LINEAR_MIPMAP_LINEAR);
		texture.upload();
		return texture;
	}
	
	protected TextureWrapper(BufferedImage image, ColorInput input, int wrapTypeGL, int nearFilterGL, int farFilterGL)
	{
		this.input = input;
		this.width = image.getWidth();
		this.height = image.getHeight();
		this.wrapTypeGL = wrapTypeGL;
		this.nearFilterGL = nearFilterGL;
		this.farFilterGL = farFilterGL;
		
		this.buffer = imageToByteBuffer(image, input);
	}
	
	public void upload()
	{
		id = GL30.glGenTextures();
		GL30.glBindTexture(GL30.GL_TEXTURE_2D, id);
		GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_WRAP_S, wrapTypeGL);
		GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_WRAP_T, wrapTypeGL);
		
		GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MIN_FILTER, farFilterGL);
		GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAG_FILTER, nearFilterGL);
		
		//Anisotropic filtering: TODO: Check if available
//		GL30.glTexParameterf(GL30.GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, 2.5f);
//		System.out.println(GL.getCapabilities().GL_EXT_texture_filter_anisotropic);
		
		GL30.glTexImage2D(GL30.GL_TEXTURE_2D, 0, input.getGlType(), width, height, 0, input.getGlType(), GL30.GL_UNSIGNED_BYTE, buffer);
		buffer = null; //No longer needed.
		GL30.glGenerateMipmap(GL30.GL_TEXTURE_2D);
	}
	
	public static ByteBuffer imageToByteBuffer(BufferedImage image, ColorInput input)
	{
		int width = image.getWidth();
		int height = image.getHeight();
		int[] pixels = new int[width * height];
		image.getRGB(0, 0, width, height, pixels, 0, width);
		ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * input.getArguments());
		if(input == ColorInput.RGBA)
		{
			for(int y = 0; y < height; y++)
			{
				for(int x = 0; x < width; x++)
				{
					int pixel = pixels[y * width + x];
					buffer.put((byte) ((pixel >> 16) & 0xFF)); //Red
					buffer.put((byte) ((pixel >> 8) & 0xFF)); //Green
					buffer.put((byte) (pixel & 0xFF)); //Blue
					buffer.put((byte) ((pixel >> 24) & 0xFF)); //Alpha
				}
			}
		}
		else if(input == ColorInput.RGB)
		{
			for(int y = 0; y < height; y++)
			{
				for(int x = 0; x < width; x++)
				{
					int pixel = pixels[y * width + x];
					buffer.put((byte) ((pixel >> 16) & 0xFF)); //Red
					buffer.put((byte) ((pixel >> 8) & 0xFF)); //Green
					buffer.put((byte) (pixel & 0xFF)); //Blue
				}
			}
		}
		else if(input == ColorInput.Grayscale)
		{
			for(int y = 0; y < height; y++)
			{
				for(int x = 0; x < width; x++)
				{
					int pixel = pixels[y * width + x];
					buffer.put((byte) (pixel & 0xFF)); //Blue
				}
			}
		}
		else //Binary / null
		{
			for(int y = 0; y < height; y++)
			{
				for(int x = 0; x < width; x++)
				{
					int pixel = pixels[y * width + x];
					buffer.put((byte) (pixel & 0xFF)); //Blue
				}
			}
		}
		buffer.flip();
		return buffer;
	}
	
	public void activate()
	{
		GL30.glBindTexture(GL30.GL_TEXTURE_2D, id);
	}
	
	public void unload()
	{
		GL30.glDeleteTextures(id);
	}
}

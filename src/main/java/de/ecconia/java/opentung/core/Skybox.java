package de.ecconia.java.opentung.core;

import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.TextureWrapper;
import de.ecconia.java.opentung.settings.Settings;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import javax.imageio.ImageIO;
import org.lwjgl.opengl.GL30;

public class Skybox
{
	private static Path skyboxFolder;
	
	private final ShaderProgram skyboxShader;
	
	private TexProvider texProvider;
	
	private Integer textureID;
	private int skyboxVAO;
	
	public Skybox(ShaderStorage shaderStorage)
	{
		this.skyboxShader = shaderStorage.getSkyboxShader();
	}
	
	public void setup()
	{
		float[] skyboxVertices = {
				// positions
				-1.0f, 1.0f, -1.0f,
				-1.0f, -1.0f, -1.0f,
				1.0f, -1.0f, -1.0f,
				1.0f, -1.0f, -1.0f,
				1.0f, 1.0f, -1.0f,
				-1.0f, 1.0f, -1.0f,
				
				-1.0f, -1.0f, 1.0f,
				-1.0f, -1.0f, -1.0f,
				-1.0f, 1.0f, -1.0f,
				-1.0f, 1.0f, -1.0f,
				-1.0f, 1.0f, 1.0f,
				-1.0f, -1.0f, 1.0f,
				
				1.0f, -1.0f, -1.0f,
				1.0f, -1.0f, 1.0f,
				1.0f, 1.0f, 1.0f,
				1.0f, 1.0f, 1.0f,
				1.0f, 1.0f, -1.0f,
				1.0f, -1.0f, -1.0f,
				
				-1.0f, -1.0f, 1.0f,
				-1.0f, 1.0f, 1.0f,
				1.0f, 1.0f, 1.0f,
				1.0f, 1.0f, 1.0f,
				1.0f, -1.0f, 1.0f,
				-1.0f, -1.0f, 1.0f,
				
				-1.0f, 1.0f, -1.0f,
				1.0f, 1.0f, -1.0f,
				1.0f, 1.0f, 1.0f,
				1.0f, 1.0f, 1.0f,
				-1.0f, 1.0f, 1.0f,
				-1.0f, 1.0f, -1.0f,
				
				-1.0f, -1.0f, -1.0f,
				-1.0f, -1.0f, 1.0f,
				1.0f, -1.0f, -1.0f,
				1.0f, -1.0f, -1.0f,
				-1.0f, -1.0f, 1.0f,
				1.0f, -1.0f, 1.0f
		};
		
		skyboxVAO = GL30.glGenVertexArrays();
		int skyboxVBO = GL30.glGenBuffers();
		GL30.glBindVertexArray(skyboxVAO);
		GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, skyboxVBO);
		GL30.glBufferData(GL30.GL_ARRAY_BUFFER, skyboxVertices, GL30.GL_STATIC_DRAW);
		GL30.glEnableVertexAttribArray(0);
		GL30.glVertexAttribPointer(0, 3, GL30.GL_FLOAT, false, 3 * Float.BYTES, 0);
		
		reloadTexture();
	}
	
	public static void prepareDataFolder(Path dataFolder) throws IOException
	{
		skyboxFolder = dataFolder.resolve("skybox");
		if(!Files.exists(skyboxFolder))
		{
			System.out.println("[FilesInit] Skybox folder does not exist, creating (along with default skybox).");
			Files.createDirectory(skyboxFolder);
			
			//Fresh folder, create default.
			InputStream link = Skybox.class.getClassLoader().getResourceAsStream("skybox/atlas.png");
			Files.copy(link, skyboxFolder.resolve("atlas.png"));
		}
		else if(!Files.isDirectory(skyboxFolder))
		{
			System.out.println("[FilesInit] [ERROR] Skybox folder is a file, thus cannot be used. Please remove skybox file: '" + skyboxFolder + "'.");
			System.exit(1);
		}
	}
	
	public void reloadTexture()
	{
		try
		{
			if(texProvider == null)
			{
				texProvider = AtlasResolver.create();
				if(texProvider == null)
				{
					texProvider = SingleResolver.create();
					if(texProvider == null)
					{
						//Cannot find any texture.
						System.out.println("[SKYBOX] No skybox texture found.");
						return;
					}
				}
			}
			else if(!texProvider.isChanged())
			{
				//Current texture has not changed.
				return;
			}
			
			System.out.println("[SKYBOX] Skybox file has changed.");
			
			BufferedImage[] images = texProvider.getImages();
			if(images == null)
			{
				System.out.println("[SKYBOX] Could not load skybox texture.");
				return;
			}
			
			int textureID = GL30.glGenTextures();
			GL30.glBindTexture(GL30.GL_TEXTURE_CUBE_MAP, textureID);
			
			for(int i = 0; i < 6; i++)
			{
				BufferedImage image = images[i];
				ByteBuffer buffer = TextureWrapper.imageToByteBuffer(image, TextureWrapper.ColorInput.RGB);
				GL30.glTexImage2D(GL30.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL30.GL_RGB, image.getWidth(), image.getHeight(), 0, GL30.GL_RGB, GL30.GL_UNSIGNED_BYTE, buffer);
			}
			
			GL30.glTexParameteri(GL30.GL_TEXTURE_CUBE_MAP, GL30.GL_TEXTURE_MIN_FILTER, GL30.GL_LINEAR);
			GL30.glTexParameteri(GL30.GL_TEXTURE_CUBE_MAP, GL30.GL_TEXTURE_MAG_FILTER, GL30.GL_LINEAR);
			GL30.glTexParameteri(GL30.GL_TEXTURE_CUBE_MAP, GL30.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_EDGE);
			GL30.glTexParameteri(GL30.GL_TEXTURE_CUBE_MAP, GL30.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_EDGE);
			GL30.glTexParameteri(GL30.GL_TEXTURE_CUBE_MAP, GL30.GL_TEXTURE_WRAP_R, GL30.GL_CLAMP_TO_EDGE);
			
			//Replace current texture:
			if(this.textureID != null && this.textureID != 0)
			{
				GL30.glDeleteTextures(this.textureID);
			}
			this.textureID = textureID;
		}
		catch(IOException e)
		{
			System.out.println("[SKYBOX] Exception while loading skybox: " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}
	
	public void render(float[] view)
	{
		if(Settings.skyboxHotReloading)
		{
			reloadTexture();
		}
		
		if(textureID == null)
		{
			return;
		}
		
		//Destructive operation, if Skybox is not the last thing to render, it should create a copy.
		view[12] = 0;
		view[13] = 0;
		view[14] = 0;
		view[15] = 0;
		
		GL30.glDisable(GL30.GL_CULL_FACE);
		GL30.glDepthFunc(GL30.GL_LEQUAL);
		
		skyboxShader.use();
		skyboxShader.setUniformM4(1, view);
		
		//Bind VAO
		GL30.glBindVertexArray(skyboxVAO);
		//Bind Texture
		GL30.glBindTexture(GL30.GL_TEXTURE_CUBE_MAP, textureID);
		//Draw arrays
		GL30.glDrawArrays(GL30.GL_TRIANGLES, 0, 36);
		
		GL30.glDepthFunc(GL30.GL_LESS);
		GL30.glEnable(GL30.GL_CULL_FACE);
	}
	
	private interface TexProvider
	{
		BufferedImage[] getImages() throws IOException;
		
		boolean isChanged() throws IOException;
	}
	
	private static class SingleResolver implements TexProvider
	{
		private final FileHolder[] files;
		
		public static SingleResolver create() throws IOException
		{
			Path[] paths = {
					skyboxFolder.resolve("right"),
					skyboxFolder.resolve("left"),
					skyboxFolder.resolve("top"),
					skyboxFolder.resolve("bottom"),
					skyboxFolder.resolve("front"),
					skyboxFolder.resolve("back"),
			};
			
			for(Path path : paths)
			{
				if(!Files.exists(path))
				{
					return null;
				}
			}
			
			FileHolder[] files = {
					new FileHolder(paths[0]),
					new FileHolder(paths[1]),
					new FileHolder(paths[2]),
					new FileHolder(paths[3]),
					new FileHolder(paths[4]),
					new FileHolder(paths[5])
			};
			
			return new SingleResolver(files);
		}
		
		private SingleResolver(FileHolder[] files)
		{
			this.files = files;
		}
		
		@Override
		public BufferedImage[] getImages() throws IOException
		{
			for(FileHolder file : files)
			{
				if(file.isInvalid())
				{
					System.out.println("[SKYBOX] WARNING: File does not exist anymore.");
					return null;
				}
			}
			
			BufferedImage[] images = new BufferedImage[6];
			for(int i = 0; i < 6; i++)
			{
				images[i] = files[i].loadImage();
				if(images[i] == null)
				{
					System.out.println("[SKYBOX] WARNING: Not able to load skybox.");
					return null;
				}
			}
			int width = images[0].getWidth();
			int height = images[0].getHeight();
			if(width != height)
			{
				System.out.println("[SKYBOX] Height and width of each side is not the same.");
				return null;
			}
			for(int i = 1; i < 6; i++)
			{
				if(images[i].getWidth() != width)
				{
					System.out.println("[SKYBOX] Not all sides have the same width.");
					return null;
				}
				if(images[i].getHeight() != height)
				{
					System.out.println("[SKYBOX] Not all sides have the same height.");
					return null;
				}
			}
			
			return images;
		}
		
		@Override
		public boolean isChanged() throws IOException
		{
			for(FileHolder file : files)
			{
				if(file.isChanged())
				{
					return true;
				}
			}
			return false;
		}
	}
	
	private static class AtlasResolver implements TexProvider
	{
		private final FileHolder file;
		
		public static AtlasResolver create() throws IOException
		{
			Path path = skyboxFolder.resolve("atlas.jpg");
			if(!Files.exists(path))
			{
				path = skyboxFolder.resolve("atlas.png");
				if(!Files.exists(path))
				{
					path = null;
				}
			}
			if(path == null)
			{
				return null;
			}
			return new AtlasResolver(new FileHolder(path));
		}
		
		private AtlasResolver(FileHolder file)
		{
			this.file = file;
		}
		
		@Override
		public BufferedImage[] getImages() throws IOException
		{
			if(file.isInvalid())
			{
				System.out.println("[SKYBOX] WARNING: File does not exist anymore.");
				return null;
			}
			
			//Load and validate bounds of file:
			BufferedImage atlas = file.loadImage();
			if(atlas == null)
			{
				System.out.println("[SKYBOX] WARNING: Not able to load skybox.");
				return null;
			}
			int width = atlas.getWidth();
			if(width % 4 != 0)
			{
				System.out.println("[SKYBOX] WARNING: To loaded image has width which cannot be divided by 4: " + width);
				return null;
			}
			int height = atlas.getHeight();
			if(height % 3 != 0)
			{
				System.out.println("[SKYBOX] WARNING: To loaded image has height which cannot be divided by 3: " + height);
				return null;
			}
			int a = width / 4;
			if(a != (height / 3))
			{
				System.out.println("[SKYBOX] Height and width of each side is not the same.");
				return null;
			}
			//Split the images:
			BufferedImage left = atlas.getSubimage(0, a, a, a);
			BufferedImage front = atlas.getSubimage(a, a, a, a);
			BufferedImage right = atlas.getSubimage(2 * a, a, a, a);
			BufferedImage back = atlas.getSubimage(3 * a, a, a, a);
			BufferedImage top = atlas.getSubimage(a, 0, a, a);
			BufferedImage bottom = atlas.getSubimage(a, 2 * a, a, a);
			
			BufferedImage[] sides = {
					right,
					left,
					top,
					bottom,
					front,
					back
			};
			
			return sides;
		}
		
		@Override
		public boolean isChanged() throws IOException
		{
			return file.isChanged();
		}
	}
	
	private static class FileHolder
	{
		private final Path path;
		private FileTime modifiedTime;
		
		public FileHolder(Path path) throws IOException
		{
			this.path = path;
			modifiedTime = Files.getLastModifiedTime(path);
		}
		
		public boolean isChanged() throws IOException
		{
			FileTime newTime = Files.getLastModifiedTime(path);
			if(!newTime.equals(modifiedTime))
			{
				modifiedTime = newTime;
				return true;
			}
			return false;
		}
		
		public boolean isInvalid()
		{
			return !Files.exists(path);
		}
		
		public BufferedImage loadImage() throws IOException
		{
			return ImageIO.read(path.toFile());
		}
	}
}

package de.ecconia.java.opentung.libwrap;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.atomic.AtomicReference;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public class SWindowWrapper
{
	private final int oWidth, oHeight;
	private final long windowID;
	
	private AtomicReference<Dimension> dim = new AtomicReference<>();
	
	public SWindowWrapper(int width, int height, String title)
	{
		this.oWidth = width;
		this.oHeight = height;
		GLFWErrorCallback.createPrint(System.err).set();
		
		if(!GLFW.glfwInit())
		{
			throw new IllegalStateException("Unable to initialize GLFW");
		}
		
		GLFW.glfwDefaultWindowHints();
		GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
		GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
		
		GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
		GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
		GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
		
		windowID = GLFW.glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL);
		if(windowID == MemoryUtil.NULL)
		{
			throw new RuntimeException("Failed to create the GLFW window");
		}
		
		try
		{
			try(MemoryStack stack = MemoryStack.stackPush())
			{
				int size = 1024;
				ByteBuffer pixels = imageResourcesToBuffer("Logo" + size + ".png", size * size);
				System.out.println(pixels.limit());
				
				IntBuffer w = stack.mallocInt(1);
				IntBuffer h = stack.mallocInt(1);
				IntBuffer comp = stack.mallocInt(1);
				ByteBuffer icon = STBImage.stbi_load_from_memory(pixels, w, h, comp, 4);
				GLFW.glfwSetWindowIcon(windowID, GLFWImage.mallocStack(1, stack)
						.width(w.get(0))
						.height(h.get(0))
						.pixels(icon)
				);
				STBImage.stbi_image_free(icon);
			}
		}
		catch(Exception e)
		{
			System.out.println("Could not set window icon:");
			e.printStackTrace(System.out);
		}
		
		GLFW.glfwSetWindowSizeCallback(windowID, (window, width2, height2) -> {
			dim.set(new Dimension(width2, height2));
		});
	}
	
	public static ByteBuffer imageResourcesToBuffer(String resource, int bufferSize) throws IOException
	{
		try(InputStream source = SWindowWrapper.class.getClassLoader().getResourceAsStream(resource);
		    ReadableByteChannel rbc = Channels.newChannel(source))
		{
			//Max size:
			ByteBuffer buffer = BufferUtils.createByteBuffer(bufferSize * 4);
			while(true)
			{
				int amountRead = rbc.read(buffer);
				if(amountRead == -1)
				{
					break;
				}
			}
			
			buffer.flip();
			return buffer;
		}
	}
	
	public void setVsync(boolean state)
	{
		GLFW.glfwSwapInterval(state ? 1 : 0);
	}
	
	public void place()
	{
//		try(MemoryStack stack = MemoryStack.stackPush())
//		{
		//IntBuffer pWidth = stack.mallocInt(1);
		//IntBuffer pHeight = stack.mallocInt(1);
		//GLFW.glfwGetWindowSize(window, pWidth, pHeight);
		GLFWVidMode vidmode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
		//GLFW.glfwSetWindowPos(window, (vidmode.width() - width.get(0)) / 2, (vidmode.height() - pHeight.get(0)) / 2
		GLFW.glfwSetWindowPos(windowID, (vidmode.width() - oWidth) / 2, (vidmode.height() - oHeight) / 2
		);
//		}
		
		GLFW.glfwShowWindow(windowID);
	}
	
	public void grabContext()
	{
		GLFW.glfwMakeContextCurrent(windowID);
	}
	
	public long getID()
	{
		return windowID;
	}
	
	public boolean shouldClose()
	{
		return GLFW.glfwWindowShouldClose(windowID);
	}
	
	public void setTitle(String title)
	{
		GLFW.glfwSetWindowTitle(windowID, title);
	}
	
	public void update()
	{
		GLFW.glfwSwapBuffers(windowID);
	}
	
	public Dimension getNewDimension()
	{
		return dim.getAndSet(null);
	}
}

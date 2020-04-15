package de.ecconia.java.opentung.libwrap;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.util.concurrent.atomic.AtomicReference;

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
		
		GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
		GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 5);
		GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
		
		windowID = GLFW.glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL);
		if(windowID == MemoryUtil.NULL)
		{
			throw new RuntimeException("Failed to create the GLFW window");
		}
		
		GLFW.glfwSetWindowSizeCallback(windowID, (window, width2, height2) -> {
			dim.set(new Dimension(width2, height2));
		});
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
		
		GLFW.glfwMakeContextCurrent(windowID);
		setVsync(true);
		GLFW.glfwShowWindow(windowID);
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

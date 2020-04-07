package de.ecconia.java.opentung.libwrap;

import org.lwjgl.glfw.GLFW;

public class KeyboardHandler
{
	private final Thread keyboardInputThread;
	
	public KeyboardHandler(long windowID)
	{
		keyboardInputThread = new Thread(() -> {
			
			while(!Thread.currentThread().isInterrupted())
			{
				GLFW.glfwPollEvents();
				
				try
				{
					Thread.sleep(100);
				}
				catch(InterruptedException e)
				{
					break;
				}
			}
			
			System.out.println("Keybaord thread shutted down.");
		}, "KeybaordThread");
		
		GLFW.glfwSetKeyCallback(windowID, (window1, key, scancode, action, mods) -> {
			System.out.println(action + " " + key);
			
			if(key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_RELEASE)
			{
				System.out.println("Should quit...");
				GLFW.glfwSetWindowShouldClose(window1, true);
			}
		});
		
		keyboardInputThread.start();
	}
	
	public void stop()
	{
		keyboardInputThread.interrupt();
	}
}

package de.ecconia.java.opentung.inputs;

import org.lwjgl.glfw.GLFW;

public class InputReceiver
{
	private final InputProcessor processor;
	private final Thread inputThread;
	
	public InputReceiver(InputProcessor processor, long windowID)
	{
		this.processor = processor;
		
		GLFW.glfwSetCursorPosCallback(windowID, (windowIDC, x, y) -> {
			processor.updateCursorPosition((int) x, (int) y);
		});
		
		GLFW.glfwSetMouseButtonCallback(windowID, (windowIDC, button, action, mods) -> {
			if(action == GLFW.GLFW_RELEASE)
			{
				processor.cursorReleased(button);
			}
			else if(action == GLFW.GLFW_PRESS)
			{
				processor.cursorPressed(button);
			}
		});
		
		GLFW.glfwSetKeyCallback(windowID, (windowIDC, key, scancode, action, mods) -> {
			if(action == GLFW.GLFW_PRESS)
			{
				processor.keyPressed(key, scancode, mods);
			}
			else if(action == GLFW.GLFW_RELEASE)
			{
				processor.keyReleased(key, scancode, mods);
			}
		});
		
		GLFW.glfwSetWindowFocusCallback(windowID, (windowIDC, state) -> {
			processor.focusChanged(state);
		});
		
		inputThread = Thread.currentThread();
	}
	
	public void stop()
	{
		inputThread.interrupt();
	}
	
	public void eventPollEntry()
	{
		long time = System.currentTimeMillis();
		while(!Thread.currentThread().isInterrupted())
		{
			GLFW.glfwPollEvents();
			
			processor.postEvents();
			
			try
			{
				long cTime = System.currentTimeMillis();
				long wTime = cTime - time;
				time = cTime;
				if(wTime > 0)
				{
					Thread.sleep(10);
				}
			}
			catch(InterruptedException e)
			{
				break;
			}
		}
	}
}

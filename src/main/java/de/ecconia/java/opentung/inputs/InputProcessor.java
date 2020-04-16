package de.ecconia.java.opentung.inputs;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class InputProcessor
{
	private int latestX;
	private int latestY;
	private int mouseXChange;
	private int mouseYChange;
	
	private final long windowID;
	private final InputReceiver receiver;
	private final List<InputConsumer> inputConsumers = new ArrayList<>();
	
	private InputConsumer blockingConsumer = null;
	private InputConsumer soonConsumer = null; //Wait a cycle before applying this.
	private boolean applyNewConsumer = false;
	
	public void registerClickConsumer(InputConsumer inputConsumer)
	{
		this.inputConsumers.add(inputConsumer);
	}
	
	public InputProcessor(long windowID)
	{
		this.windowID = windowID;
		
		receiver = new InputReceiver(this, windowID);
	}
	
	//#########################
	// Receiver methods:
	//#########################
	
	public void updateCursorPosition(int x, int y)
	{
		mouseXChange += latestX - x;
		mouseYChange += latestY - y;
		latestX = x;
		latestY = y;
	}
	
	public void cursorPressed(int button)
	{
		for(InputConsumer consumer : inputConsumers)
		{
			if(consumer.down(button, latestX, latestY))
			{
				break;
			}
		}
	}
	
	public void cursorReleased(int button)
	{
		for(InputConsumer consumer : inputConsumers)
		{
			if(consumer.up(button, latestX, latestY))
			{
				break;
			}
		}
	}
	
	public void keyPressed(int key, int scancode, int mods)
	{
		//TODO: Forward to consumer.
	}
	
	public void keyReleased(int key, int scancode, int mods)
	{
		if(key == GLFW.GLFW_KEY_ESCAPE)
		{
			skip:
			{
				for(InputConsumer consumer : inputConsumers)
				{
					if(consumer.escapeIssued())
					{
						break skip;
					}
				}
				
				//No layer used the ESC, close game.
				//TODO: Add settings toggle later on.
				GLFW.glfwSetWindowShouldClose(windowID, true);
			}
			
			return;
		}
		
		//TODO: Forward to consumer.
//		if(blockingConsumer != null)
//		{
//			for(InputConsumer consumer : inputConsumers)
//			{
//			}
//		}
	}
	
	public void postEvents()
	{
		if(blockingConsumer != null)
		{
			boolean isA = GLFW.glfwGetKey(windowID, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS;
			boolean isS = GLFW.glfwGetKey(windowID, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS;
			boolean isD = GLFW.glfwGetKey(windowID, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS;
			boolean isW = GLFW.glfwGetKey(windowID, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS;
			boolean isSp = GLFW.glfwGetKey(windowID, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;
			boolean isSh = GLFW.glfwGetKey(windowID, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS;
			
			blockingConsumer.movement(mouseXChange, mouseYChange, isA, isD, isW, isS, isSp, isSh);
		}
		
		//Who knows who calls the blocking thing when, first lets finish all handling, then update this.
		if(this.applyNewConsumer)
		{
			this.blockingConsumer = this.soonConsumer;
			this.applyNewConsumer = false;
		}
		//Reset mouse movement, it should have been processed by now.
		mouseXChange = 0;
		mouseYChange = 0;
	}
	
	public void stop()
	{
		receiver.stop();
	}
	
	public void captureMode(InputConsumer consumer)
	{
		this.soonConsumer = consumer;
		applyNewConsumer = true;
		if(consumer == null)
		{
			GLFW.glfwSetInputMode(windowID, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
		}
		else
		{
			GLFW.glfwSetInputMode(windowID, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
		}
	}
	
	public boolean isCaptured(InputConsumer consumer)
	{
		return blockingConsumer == consumer;
	}
	
	public boolean isCaptured()
	{
		return blockingConsumer != null;
	}
	
	public void focusChanged(boolean state)
	{
		//TBI: May fight with the setting by click or?
		if(!state)
		{
			captureMode(null);
		}
	}
}

package de.ecconia.java.opentung.inputs;

import org.lwjgl.glfw.GLFW;

public class InputProcessor implements Controller
{
	//TODO: Add fancy stuff, for example that one can walk - while a menu is open - as long as no text field is in focus.
	
	public static final int MOUSE_RIGHT = GLFW.GLFW_MOUSE_BUTTON_RIGHT;
	public static final int MOUSE_LEFT = GLFW.GLFW_MOUSE_BUTTON_LEFT;
	public static final int MOUSE_MIDDLE = GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
	
	private int latestX;
	private int latestY;
	private int mouseXChange;
	private int mouseYChange;
	
	private final long windowID;
	private final InputReceiver receiver;
	
	private Controller3D controller3D;
	private Controller2D controller2D;
	private Controller activeController = this;
	
	private Long escapeDown;
	
	public InputProcessor(long windowID)
	{
		this.windowID = windowID;
		
		receiver = new InputReceiver(this, windowID);
	}
	
	public void eventPollEntry()
	{
		//Switch to InputReceiver to receive all events.
		receiver.eventPollEntry();
	}
	
	public void stop()
	{
		receiver.stop();
	}
	
	public void setController(Controller controller)
	{
		if(controller instanceof Controller2D)
		{
			controller2D = (Controller2D) controller;
		}
		else
		{
			controller3D = (Controller3D) controller;
		}
		controller.setInputThread(this);
		
		if(controller3D != null && controller2D != null)
		{
			activeController = controller2D;
		}
	}
	
	public long getWindowID()
	{
		return windowID;
	}
	
	public int getMouseXChange()
	{
		return mouseXChange;
	}
	
	public int getMouseYChange()
	{
		return mouseYChange;
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
		
		activeController.mouseMove(latestX, latestY, mouseXChange, mouseYChange);
	}
	
	public void cursorPressed(int button)
	{
		activeController.mouseDown(button, latestX, latestY);
	}
	
	public void cursorReleased(int button)
	{
		activeController.mouseUp(button, latestX, latestY);
	}
	
	public void keyPressed(int key, int scancode, int mods)
	{
		if(key == GLFW.GLFW_KEY_ESCAPE)
		{
			escapeDown = System.currentTimeMillis();
		}
		activeController.keyDown(key, scancode, mods);
	}
	
	public void keyReleased(int key, int scancode, int mods)
	{
		if(key == GLFW.GLFW_KEY_ESCAPE)
		{
			//I cannot guarantee random updates by OS, thus this test.
			if((System.currentTimeMillis() - escapeDown) > 1000)
			{
				issueShutdown();
				return;
			}
			escapeDown = null;
		}
		activeController.keyUp(key, scancode, mods);
	}
	
	public void mouseScrolled(double xScroll, double yScroll)
	{
		activeController.scrolled(xScroll, yScroll);
	}
	
	public void focusChanged(boolean state)
	{
		if(!state)
		{
			activeController.unfocus();
		}
	}
	
	public void postEvents()
	{
		if(escapeDown != null && (System.currentTimeMillis() - escapeDown) > 1000)
		{
			issueShutdown();
			return;
		}
		activeController.inputInterval();
		
		//Reset mouse movement, it should have been processed by now.
		mouseXChange = 0;
		mouseYChange = 0;
	}
	
	//Switching modes:
	
	public void switchTo3D()
	{
		//Disable cursor.
		GLFW.glfwSetInputMode(windowID, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
		activeController = controller3D;
		receiver.setIntervalMode(true);
	}
	
	public void switchTo2D()
	{
		//Rearm cursor
		GLFW.glfwSetInputMode(windowID, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
		activeController = controller2D;
		receiver.setIntervalMode(false);
	}
	
	public Controller2D get2DController()
	{
		return controller2D;
	}
	
	//Controller commands:
	
	public void issueShutdown()
	{
		GLFW.glfwSetWindowShouldClose(windowID, true);
	}
	
	//Startup Controller, handles what can't be handled yet - boot process:
	
	@Override
	public void setInputThread(InputProcessor inputProcessor)
	{
	}
	
	@Override
	public void keyUp(int keyIndex, int scancode, int mods)
	{
		if(keyIndex == GLFW.GLFW_KEY_ESCAPE)
		{
			//TODO: Add setting for this behavior:
			issueShutdown();
		}
	}
	
	public void updatePauseMenu()
	{
		controller2D.updatePauseMenu();
	}
}

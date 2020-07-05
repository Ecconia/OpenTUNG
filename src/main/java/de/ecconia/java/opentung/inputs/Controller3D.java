package de.ecconia.java.opentung.inputs;

import de.ecconia.java.opentung.Camera;
import de.ecconia.java.opentung.RenderPlane3D;
import de.ecconia.java.opentung.Settings;
import de.ecconia.java.opentung.components.conductor.Connector;
import de.ecconia.java.opentung.components.meta.Holdable;
import de.ecconia.java.opentung.components.meta.Part;
import org.lwjgl.glfw.GLFW;

public class Controller3D implements Controller
{
	private final RenderPlane3D renderPlane3D;
	private final Camera camera;
	
	private InputProcessor inputProcessor;
	
	public Controller3D(RenderPlane3D renderPlane3D)
	{
		this.renderPlane3D = renderPlane3D;
		camera = renderPlane3D.getCamera();
	}
	
	@Override
	public void setInputThread(InputProcessor inputProcessor)
	{
		this.inputProcessor = inputProcessor;
	}
	
	@Override
	public void inputInterval()
	{
		long windowID = inputProcessor.getWindowID();
		boolean isA = GLFW.glfwGetKey(windowID, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS;
		boolean isS = GLFW.glfwGetKey(windowID, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS;
		boolean isD = GLFW.glfwGetKey(windowID, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS;
		boolean isW = GLFW.glfwGetKey(windowID, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS;
		boolean isSp = GLFW.glfwGetKey(windowID, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;
		boolean isSh = GLFW.glfwGetKey(windowID, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS;
		boolean isControl = GLFW.glfwGetKey(windowID, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS;
		
		camera.movement(inputProcessor.getMouseXChange(), inputProcessor.getMouseYChange(), isA, isD, isW, isS, isSp, isSh, isControl);
	}
	
	@Override
	public void mouseUp(int type, int x, int y)
	{
		if(type == InputProcessor.MOUSE_LEFT)
		{
			checkMouseLeft(false);
		}
		else if(type == InputProcessor.MOUSE_RIGHT)
		{
			checkMouseRight(false);
		}
	}
	
	@Override
	public void mouseDown(int type, int x, int y)
	{
		if(type == InputProcessor.MOUSE_LEFT)
		{
			checkMouseLeft(true);
		}
		else if(type == InputProcessor.MOUSE_RIGHT)
		{
			checkMouseRight(true);
		}
	}
	
	@Override
	public void keyUp(int keyIndex, int scancode, int mods)
	{
		if(keyIndex == GLFW.GLFW_KEY_ESCAPE)
		{
			switchToInterface();
		}
	}
	
	@Override
	public void unfocus()
	{
		switchToInterface();
	}
	
	//Stuff:
	
	private boolean mouseDownLeft;
	private boolean mouseDownRight;
	
	private void switchToInterface()
	{
		inputProcessor.switchTo2D();
		checkMouseLeft(false);
		checkMouseRight(false);
	}
	
	private void checkMouseLeft(boolean shouldBePressed)
	{
		if(shouldBePressed)
		{
			if(mouseDownLeft)
			{
				System.out.println("Left click already marked down 3D-Pane, but got downed again.");
			}
			else
			{
				mouseDownLeft = true;
				mouseLeftDown();
			}
		}
		else
		{
			if(mouseDownLeft)
			{
				mouseDownLeft = false;
				mouseLeftUp();
			}
		}
	}
	
	private void checkMouseRight(boolean shouldBePressed)
	{
		if(shouldBePressed)
		{
			if(mouseDownRight)
			{
				System.out.println("Right click already marked down 3D-Pane, but got downed again.");
			}
			else
			{
				mouseDownRight = true;
				mouseRightDown();
			}
		}
		else
		{
			if(mouseDownRight)
			{
				mouseDownRight = false;
				mouseRightUp();
			}
		}
	}
	
	//State awareness / actual handling / non framework:
	
	public void doFrameCycle()
	{
		doLeftHoldableCheck();
	}
	
	//Left:
	
	private long mouseLeftDown;
	private Part mouseLeftDownOn;
	private Holdable mouseLeftHoldable;
	
	private void mouseLeftDown()
	{
		mouseLeftDown = System.currentTimeMillis();
		mouseLeftDownOn = renderPlane3D.getCursorObject();
	}
	
	private void mouseLeftUp()
	{
		Part mouseLeftDownOn = renderPlane3D.getCursorObject();
		if(mouseLeftDownOn != null)
		{
			long clickDuration = (System.currentTimeMillis() - mouseLeftDown);
			//If the click was longer than a second, validate that its the intended component...
			if(clickDuration > Settings.longMousePressDuration)
			{
				if(this.mouseLeftDownOn == mouseLeftDownOn)
				{
					renderPlane3D.componentLeftClicked(mouseLeftDownOn);
				}
			}
			else
			{
				renderPlane3D.componentLeftClicked(mouseLeftDownOn);
			}
		}
		mouseLeftDown = 0;
	}
	
	private void doLeftHoldableCheck()
	{
		if(mouseLeftDown != 0)
		{
			Part part = renderPlane3D.getCursorObject();
			if(part != null)
			{
				if(part instanceof Holdable)
				{
					Holdable currentlyHold = (Holdable) part;
					if(currentlyHold != mouseLeftHoldable)
					{
						if(mouseLeftHoldable != null)
						{
							//If mouse over something else.
							renderPlane3D.componentLeftUnHold(mouseLeftHoldable);
						}
						//If something new is hold:
						mouseLeftHoldable = currentlyHold;
						renderPlane3D.componentLeftHold(mouseLeftHoldable);
					}
				}
				else
				{
					if(mouseLeftHoldable != null)
					{
						//If mouse over something non-holdable.
						renderPlane3D.componentLeftUnHold(mouseLeftHoldable);
						mouseLeftHoldable = null;
					}
				}
			}
			else
			{
				if(mouseLeftHoldable != null)
				{
					//If mouse no longer over a component.
					renderPlane3D.componentLeftUnHold(mouseLeftHoldable);
					mouseLeftHoldable = null;
				}
			}
		}
		else if(mouseLeftHoldable != null)
		{
			//If mouse has been lifted.
			renderPlane3D.componentLeftUnHold(mouseLeftHoldable);
			mouseLeftHoldable = null;
		}
	}
	
	//Right:
	
	private long mouseRightDown;
	private Part mouseRightDownOn;
	private boolean mouseRightDownOnConnector;
	
	private void mouseRightDown()
	{
		mouseRightDown = System.currentTimeMillis();
		mouseRightDownOn = renderPlane3D.getCursorObject();
		if(mouseRightDownOn instanceof Connector)
		{
			mouseRightDownOnConnector = true;
			renderPlane3D.rightDragOnConnector((Connector) mouseRightDownOn);
		}
	}
	
	private void mouseRightUp()
	{
		Part mouseRightDownOn = renderPlane3D.getCursorObject();
		if(mouseRightDownOnConnector)
		{
			renderPlane3D.rightDragOnConnector(null);
		}
		else
		{
			if(mouseRightDownOn != null)
			{
				long clickDuration = (System.currentTimeMillis() - mouseRightDown);
				//If the click was longer than a second, validate that its the intended component...
				if(clickDuration > Settings.longMousePressDuration)
				{
					if(this.mouseRightDownOn == mouseRightDownOn)
					{
						renderPlane3D.componentRightClicked(mouseRightDownOn);
					}
				}
				else
				{
					renderPlane3D.componentRightClicked(mouseRightDownOn);
				}
			}
		}
		mouseRightDown = 0;
		mouseRightDownOnConnector = false;
	}
}

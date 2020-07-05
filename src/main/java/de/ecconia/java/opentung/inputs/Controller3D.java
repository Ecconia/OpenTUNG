package de.ecconia.java.opentung.inputs;

import de.ecconia.java.opentung.Camera;
import de.ecconia.java.opentung.RenderPlane3D;
import org.lwjgl.glfw.GLFW;

public class Controller3D implements Controller
{
	private final RightClickReceiver mouseClickReceiver;
	private final Camera camera;
	
	private InputProcessor inputProcessor;
	
	private boolean mousedown;
	
	public Controller3D(RenderPlane3D renderPlane3D)
	{
		mouseClickReceiver = renderPlane3D;
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
			checkMouse(false);
		}
		else if(type == InputProcessor.MOUSE_RIGHT)
		{
			mouseClickReceiver.mouseRightClick();
		}
	}
	
	@Override
	public void mouseDown(int type, int x, int y)
	{
		if(type == InputProcessor.MOUSE_LEFT)
		{
			checkMouse(true);
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
	
	private void switchToInterface()
	{
		inputProcessor.switchTo2D();
		checkMouse(false);
	}
	
	private void checkMouse(boolean shouldBePressed)
	{
		if(shouldBePressed)
		{
			if(mousedown)
			{
				System.out.println("Right click already marked down 3D-Pane, but got downed again.");
			}
			else
			{
				mousedown = true;
				mouseClickReceiver.mouseLeftDown();
			}
		}
		else
		{
			if(mousedown)
			{
				mousedown = false;
				mouseClickReceiver.mouseLeftUp();
			}
		}
	}
	
	public interface RightClickReceiver
	{
		void mouseLeftUp();
		
		void mouseLeftDown();
		
		void mouseRightClick();
	}
}

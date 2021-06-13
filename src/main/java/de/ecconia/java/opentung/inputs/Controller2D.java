package de.ecconia.java.opentung.inputs;

import de.ecconia.java.opentung.components.meta.PlaceableInfo;
import de.ecconia.java.opentung.interfaces.RenderPlane2D;
import de.ecconia.java.opentung.settings.keybinds.Keybindings;
import org.lwjgl.glfw.GLFW;

public class Controller2D implements Controller
{
	private final RenderPlane2D renderPlane2D;
	
	private InputProcessor inputProcessor;
	
	private boolean mouseOnGUI;
	private boolean leftMouseDown;
	
	public Controller2D(RenderPlane2D renderPlane2D)
	{
		this.renderPlane2D = renderPlane2D;
	}
	
	@Override
	public void setInputThread(InputProcessor inputProcessor)
	{
		this.inputProcessor = inputProcessor;
	}
	
	@Override
	public void mouseMove(int xAbs, int yAbs, int xRel, int yRel)
	{
		renderPlane2D.mouseMoved(xAbs, yAbs, leftMouseDown);
	}
	
	@Override
	public void mouseDown(int mouseIndex, int x, int y)
	{
		if(mouseIndex == InputProcessor.MOUSE_LEFT)
		{
			leftMouseDown = true;
			if(renderPlane2D.leftMouseDown(x, y))
			{
				mouseOnGUI = true;
			}
		}
	}
	
	@Override
	public void mouseUp(int mouseIndex, int x, int y)
	{
		if(mouseIndex == InputProcessor.MOUSE_LEFT)
		{
			leftMouseDown = false;
			if(!mouseOnGUI)
			{
				if(!renderPlane2D.leftMouseUp(x, y))
				{
					if(renderPlane2D.hasWindowOpen())
					{
						renderPlane2D.closeWindows();
					}
					inputProcessor.switchTo3D();
				}
			}
			else
			{
				renderPlane2D.leftMouseUp(x, y);
			}
		}
		else if(mouseIndex == InputProcessor.MOUSE_MIDDLE)
		{
			renderPlane2D.middleMouse(x, y);
		}
		
		mouseOnGUI = false;
	}
	
	@Override
	public void keyUp(int keyIndex, int scancode, int mods)
	{
		if(keyIndex == GLFW.GLFW_KEY_ESCAPE)
		{
			if(renderPlane2D.hasWindowOpen())
			{
				renderPlane2D.closeWindows();
				mouseOnGUI = false; //Reset regardless.
				inputProcessor.switchTo3D(); //If closed just go here.
			}
			else
			{
				openPauseMenu();
			}
		}
		else if(scancode == Keybindings.KeyToggleComponentsList)
		{
			if(!renderPlane2D.toggleComponentList())
			{
				if(!renderPlane2D.hasWindowOpen())
				{
					mouseOnGUI = false; //Reset regardless.
					inputProcessor.switchTo3D(); //If closed just go here.
				}
			}
		}
		else if(scancode == Keybindings.KeyUnlockMouseCursor)
		{
			//The cursor is already unlocked, thus here it just closes the window...
			renderPlane2D.closeWindows();
		}
	}
	
	public void forwardScrollingToHotbar(int val)
	{
		renderPlane2D.getHotbar().scrollInput(val);
	}
	
	public void forwardNumberIndexToHotbar(int index)
	{
		renderPlane2D.getHotbar().numberInput(index);
	}
	
	public void forwardInfoToHotbar(PlaceableInfo info)
	{
		renderPlane2D.getHotbar().selectOrAdd(info);
	}
	
	public void openComponentList()
	{
		inputProcessor.switchTo2D();
		renderPlane2D.openComponentList();
	}
	
	public void openPauseMenu()
	{
		inputProcessor.switchTo2D();
		renderPlane2D.openPauseMenu();
	}
	
	public void updatePauseMenu()
	{
		renderPlane2D.updatePauseMenu();
	}
	
	public void dropHotbarEntry()
	{
		renderPlane2D.getHotbar().dropHotbarEntry();
	}
}

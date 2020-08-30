package de.ecconia.java.opentung.inputs;

import de.ecconia.java.opentung.PlaceableInfo;
import de.ecconia.java.opentung.interfaces.RenderPlane2D;
import org.lwjgl.glfw.GLFW;

public class Controller2D implements Controller
{
	private final RenderPlane2D renderPlane2D;
	
	private InputProcessor inputProcessor;
	
	private boolean mouseOnGUI;
	
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
	
	}
	
	@Override
	public void mouseDown(int mouseIndex, int x, int y)
	{
		if(mouseIndex == InputProcessor.MOUSE_LEFT)
		{
			if(renderPlane2D.leftMouseDownIn(x, y))
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
			if(!mouseOnGUI)
			{
				if(renderPlane2D.hasWindowOpen())
				{
					renderPlane2D.closeWindow();
				}
				inputProcessor.switchTo3D();
			}
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
				renderPlane2D.closeWindow();
				mouseOnGUI = false; //Reset regardless.
				inputProcessor.switchTo3D(); //If closed just go here.
			}
			else
			{
				//TODO: Add setting for this behavior:
				inputProcessor.issueShutdown();
			}
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
		renderPlane2D.getHotbar().setInfo(info);
	}
	
	public void openComponentList()
	{
		inputProcessor.switchTo2D();
		renderPlane2D.openComponentList();
	}
}

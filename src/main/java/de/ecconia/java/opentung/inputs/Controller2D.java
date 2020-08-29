package de.ecconia.java.opentung.inputs;

import de.ecconia.java.opentung.PlaceableInfo;
import de.ecconia.java.opentung.interfaces.RenderPlane2D;
import org.lwjgl.glfw.GLFW;

public class Controller2D implements Controller
{
	private final RenderPlane2D renderPlane2D;
	
	private InputProcessor inputProcessor;
	
	public Controller2D(RenderPlane2D renderPlane2D)
	{
		this.renderPlane2D = renderPlane2D;
	}
	
	@Override
	public void setInputThread(InputProcessor inputProcessor)
	{
		this.inputProcessor = inputProcessor;
	}
	
	//Currently there is no UI element, thus just give focus away on a click and exit on ESC.
	
	@Override
	public void mouseUp(int mouseIndex, int x, int y)
	{
		if(mouseIndex == InputProcessor.MOUSE_LEFT)
		{
			inputProcessor.switchTo3D();
		}
	}
	
	@Override
	public void keyUp(int keyIndex, int scancode, int mods)
	{
		if(keyIndex == GLFW.GLFW_KEY_ESCAPE)
		{
			//TODO: Add setting for this behavior:
			inputProcessor.issueShutdown();
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
}

package de.ecconia.java.opentung.interfaces;

import de.ecconia.java.opentung.core.data.ShaderStorage;

public abstract class Window
{
	protected boolean isVisible; //Input thread controlled.
	private boolean renderVisibility; //Render thread "controlled".
	
	//Render cycle visibility:
	
	public void storeRenderVisibility()
	{
		renderVisibility = isVisible;
	}
	
	public boolean isRenderVisibilitySet()
	{
		return renderVisibility;
	}
	
	//General stuff:
	
	public void setup()
	{
	}
	
	public boolean isVisible()
	{
		return isVisible;
	}
	
	public void close()
	{
		isVisible = false;
	}
	
	//Rendering:
	
	//Although misleading, this is for drawing things with NVG library.
	public void renderFrame()
	{
	}
	
	//This is for every thing, OpenTUNG can render by itself. Currently textures and font.
	public void renderDecor(ShaderStorage shaderStorage)
	{
	}
	
	//Input:
	
	public boolean keyUp(int scancode)
	{
		return false;
	}
	
	public boolean leftMouseDown(int x, int y)
	{
		return false;
	}
	
	public boolean leftMouseUp(int x, int y)
	{
		return false;
	}
	
	public boolean mouseMoved(int x, int y, boolean leftDown)
	{
		return false;
	}
	
	public boolean middleMouse(int x, int y)
	{
		return false;
	}
}

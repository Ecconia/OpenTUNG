package de.ecconia.java.opentung.core.tools;

import de.ecconia.java.opentung.core.data.Hitpoint;

public interface Tool
{
	//Activate?
	
	/**
	 * @return boolean null if not using, the value is true if the tool is also activated
	 */
	default Boolean activateKeyUp(Hitpoint hitpoint, int scancode, boolean control)
	{
		return null;
	}
	
	default Boolean activateMouseDown(Hitpoint hitpoint, int buttonCode, boolean control)
	{
		return null;
	}
	
	//Keybindings:
	
	/**
	 * Called when Escape is pressed.
	 *
	 * @return true if consumed
	 */
	default boolean abort()
	{
		return false;
	}
	
	/**
	 * Called when a key is released.
	 *
	 * @return true if consumed
	 */
	default boolean keyUp(int scancode, boolean control)
	{
		return false;
	}
	
	default boolean scroll(int val, boolean control, boolean alt)
	{
		return false;
	}
	
	default boolean mouseLeftUp()
	{
		return false;
	}
	
	default boolean mouseRightUp()
	{
		return false;
	}
	
	default boolean mouseRightDown(Hitpoint hitpoint)
	{
		return false;
	}
	
	//Render:
	
	/**
	 * Called in the render routine, while the world is loaded.
	 *
	 * @param view
	 */
	default void renderWorld(float[] view)
	{
	}
	
	/**
	 * Called after the world has been rendered, used to overlay transparent highlighting surfaces.
	 *
	 * @param view
	 */
	default void renderOverlay(float[] view)
	{
	}
	
	default Hitpoint adjustHitpoint(Hitpoint hitpoint)
	{
		return hitpoint;
	}
}

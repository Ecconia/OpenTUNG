package de.ecconia.java.opentung.inputs;

public interface Controller
{
	void setInputThread(InputProcessor inputProcessor);
	
	//Mouse:
	
	default void mouseDown(int mouseIndex, int x, int y)
	{
	}
	
	default void mouseUp(int mouseIndex, int x, int y)
	{
	}
	
	default void mouseMove(int xAbs, int yAbs, int xRel, int yRel)
	{
	}
	
	//Keyboard:
	
	default void keyDown(int keyIndex, int scancode, int mods)
	{
	}
	
	default void keyUp(int keyIndex, int scancode, int mods)
	{
	}
	
	//Special:
	
	default void scrolled(double xoffset, double yoffset)
	{
	}
	
	default void unfocus()
	{
	}
	
	default void inputInterval()
	{
	}
}

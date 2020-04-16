package de.ecconia.java.opentung.inputs;

public interface InputConsumer
{
	default boolean down(int type, int x, int y)
	{
		return false;
	}
	
	default boolean move(int xAbs, int yAbs, int xRel, int yRel)
	{
		return false;
	}
	
	default boolean up(int type, int x, int y)
	{
		return false;
	}
	
	//No return bool, since this is only called when capture mode.
	default void movement(float mx, float my, boolean l, boolean r, boolean f, boolean b, boolean u, boolean d)
	{
	}
	
	default boolean escapeIssued()
	{
		return false;
	}
}

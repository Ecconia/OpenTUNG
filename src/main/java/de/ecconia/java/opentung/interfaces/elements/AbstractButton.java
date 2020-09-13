package de.ecconia.java.opentung.interfaces.elements;

import de.ecconia.java.opentung.interfaces.GUIColors;
import de.ecconia.java.opentung.interfaces.Shapes;

public abstract class AbstractButton
{
	protected final float width, height;
	protected final float relX, relY;
	
	private boolean hovered;
	
	public AbstractButton(float relX, float relY, int width, int height)
	{
		this.width = width;
		this.height = height;
		this.relX = relX;
		this.relY = relY;
	}
	
	public void renderFrame(long nvg, float x, float y)
	{
		Shapes.drawBox(nvg, x + relX, y + relY, width, height, GUIColors.background, hovered ? GUIColors.outlineHighlighted : GUIColors.outline);
	}
	
	public boolean inside(float x, float y)
	{
		float halfW = width / 2f;
		float halfH = height / 2f;
		x -= relX;
		y -= relY;
		return x > -halfW && x < halfW && y > -halfH && y < halfH;
	}
	
	public void resetHover()
	{
		hovered = false;
	}
	
	public void testHover(float x, float y)
	{
		hovered = inside(x, y);
	}
}

package de.ecconia.java.opentung.interfaces.elements;

import de.ecconia.java.opentung.interfaces.GUIColors;
import de.ecconia.java.opentung.interfaces.Shapes;

public abstract class AbstractButton
{
	protected final float width, height;
	protected final float relX, relY;
	
	protected boolean disabled;
	
	private boolean hovered;
	
	public AbstractButton(float relX, float relY, float width, float height)
	{
		this.width = width;
		this.height = height;
		this.relX = relX;
		this.relY = relY;
	}
	
	public void renderFrame(long nvg, float x, float y)
	{
		Shapes.drawBox(nvg, x + relX, y + relY, width, height, disabled ? GUIColors.backgroundDisabled : GUIColors.background, hovered && !disabled ? GUIColors.outlineHighlighted : GUIColors.outline);
	}
	
	public boolean inside(float x, float y)
	{
		if(disabled)
		{
			return false;
		}
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
	
	public void setDisabled(boolean disabled)
	{
		this.disabled = disabled;
	}
}

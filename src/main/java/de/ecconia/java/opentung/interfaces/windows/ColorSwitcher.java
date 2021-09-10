package de.ecconia.java.opentung.interfaces.windows;

import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.meta.CustomColor;
import de.ecconia.java.opentung.core.tools.EditWindow;
import de.ecconia.java.opentung.interfaces.GUIColors;
import de.ecconia.java.opentung.interfaces.RenderPlane2D;
import de.ecconia.java.opentung.interfaces.Shapes;
import de.ecconia.java.opentung.interfaces.Window;
import de.ecconia.java.opentung.settings.Settings;
import de.ecconia.java.opentung.settings.keybinds.Keybindings;
import de.ecconia.java.opentung.util.math.Vector3;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NanoVG;

public class ColorSwitcher extends Window
{
	private final RenderPlane2D interfaceRenderer;
	
	private static final float side = 100;
	private static final float padding = 10;
	private static final Color[] colors = {
			//Display colors, in order:
			new Color(178, 0, 0),
			new Color(244, 217, 2),
			new Color(0, 48, 191),
			new Color(19, 143, 0),
			new Color(244, 91, 0),
			new Color(136, 17, 244),
			new Color(191, 191, 201),
			//Board colors:
			new Color(195, 195, 195),
			new Color(110, 110, 110),
			new Color(51, 51, 51),
			new Color(84, 213, 34),
			new Color(84, 145, 34),
			new Color(14, 97, 0),
			new Color(0, 205, 167),
			new Color(0, 105, 160),
			new Color(0, 42, 167),
			new Color(234, 0, 196),
			new Color(161, 2, 168),
			new Color(83, 0, 95),
			new Color(236, 50, 60),
			new Color(196, 16, 13),
			new Color(138, 4, 4),
			new Color(255, 225, 84),
			new Color(245, 202, 0),
			new Color(253, 160, 52),
			new Color(255, 141, 7),
			new Color(185, 86, 0),
			new Color(100, 46, 0),
			new Color(52, 24, 0),
	};
	
	private final float windowWidth;
	private final float windowHeight;
	
	private EditWindow editWindow;
	private CustomColor component;
	
	private final List<ColorSlot> colorSlots = new ArrayList<>();
	
	public ColorSwitcher(RenderPlane2D interfaceRenderer)
	{
		this.interfaceRenderer = interfaceRenderer;
		
		int amount = colors.length;
		int columns = (int) Math.ceil(Math.sqrt(amount));
		int rows = (int) Math.ceil((float) amount / (float) columns);
		
		windowWidth = columns * (side + padding) + padding;
		windowHeight = rows * (side + padding) + padding;
		
		int i = 0;
		float offsetX = -windowWidth / 2f + padding + side / 2f;
		float currentY = -windowHeight / 2f + padding + side / 2f;
		for(int y = 0; y < rows; y++)
		{
			float currentX = offsetX;
			for(int x = 0; x < columns; x++)
			{
				if(i >= amount)
				{
					return;
				}
				Color color = colors[i];
				colorSlots.add(new ColorSlot(currentX, currentY, side, side, color));
				currentX += side + padding;
				i++;
			}
			currentY += side + padding;
		}
	}
	
	public void activate(EditWindow editWindow, CustomColor component)
	{
		interfaceRenderer.getInputHandler().switchTo2D(); //TBI: Should be more generic?
		setComponent(editWindow, component);
		isVisible = true;
	}
	
	private void setComponent(EditWindow editWindow, CustomColor component)
	{
		this.editWindow = editWindow;
		this.component = component;
	}
	
	@Override
	public void close()
	{
		super.close();
		
		component = null;
		editWindow.guiClosed();
		//Also clear this data:
		lastSlot = null;
		startColor = null;
	}
	
	private float middleX;
	private float middleY;
	
	@Override
	public void renderFrame()
	{
		float scale = Settings.guiScale;
		long nvg = interfaceRenderer.vg;
		
		middleX = interfaceRenderer.realWidth(scale) / 2f;
		middleY = interfaceRenderer.realHeight(scale) / 2f;
		Shapes.drawBox(nvg, middleX, middleY, windowWidth, windowHeight, GUIColors.background, GUIColors.outline);
		
		for(ColorSlot component : colorSlots)
		{
			component.draw(nvg, middleX, middleY);
		}
	}
	
	//### Input handling: ###
	
	private Color startColor; //Also serves as "isMouseDown" bool.
	private ColorSlot lastSlot;
	
	@Override
	public boolean keyUp(int scancode)
	{
		if(scancode == Keybindings.KeyEditComponent)
		{
			close();
			interfaceRenderer.getInputHandler().switchTo3D();
			return true;
		}
		return false;
	}
	
	@Override
	public boolean mouseMoved(int x, int y, boolean leftDown)
	{
		if(startColor == null)
		{
			return true;
		}
		float scale = Settings.guiScale;
		float sx = (float) x / scale;
		float sy = (float) y / scale;
		
		if(downInside(sx, sy))
		{
			doMovementUpdate(sx, sy);
		}
		return true;
	}
	
	@Override
	public boolean leftMouseDown(int x, int y)
	{
		startColor = component.getColor(); //Mouse down.
		
		float scale = Settings.guiScale;
		float sx = (float) x / scale;
		float sy = (float) y / scale;
		
		if(downInside(sx, sy))
		{
			doMovementUpdate(sx, sy);
			return true;
		}
		return false;
	}
	
	private void doMovementUpdate(float sx, float sy)
	{
		float xx = sx - middleX;
		float yy = sy - middleY;
		for(ColorSlot slot : colorSlots)
		{
			if(slot.inside(xx, yy))
			{
				component.setColor(slot.getColor());
				lastSlot = slot;
				break;
			}
		}
	}
	
	@Override
	public boolean leftMouseUp(int x, int y)
	{
		float scale = Settings.guiScale;
		float sx = (float) x / scale;
		float sy = (float) y / scale;
		
		boolean downInside = downInside(sx, sy);
		float xx = sx - middleX;
		float yy = sy - middleY;
		
		if(!downInside || lastSlot == null || !lastSlot.inside(xx, yy))
		{
			//Restore color:
			component.setColor(startColor);
		}
		else
		{
			//Apply/Keep color:
			//TBI: Is the manual handling an okay-ish solution?
			close();
			interfaceRenderer.getInputHandler().switchTo3D();
		}
		
		lastSlot = null;
		startColor = null;
		return downInside;
	}
	
	//### Changer: ###
	
	private boolean downInside(float x, float y)
	{
		float windowStartX = middleX - windowWidth / 2f;
		float windowStartY = middleY - windowHeight / 2f;
		return windowStartX < x && x < (windowStartX + windowWidth) && windowStartY < y && y < (windowStartY + windowHeight);
	}
	
	//### Classes: ###
	
	private static class ColorSlot
	{
		private final float width, height;
		private final float relX, relY;
		private final NVGColor nvgColor;
		private final Color color;
		
		public ColorSlot(float relX, float relY, float width, float height, Color color)
		{
			this.width = width;
			this.height = height;
			this.relX = relX;
			this.relY = relY;
			this.color = color;
			
			Vector3 vColor = color.asVector();
			nvgColor = NanoVG.nvgRGBAf(
					(float) vColor.getX(),
					(float) vColor.getY(),
					(float) vColor.getZ(),
					1.0f, NVGColor.create());
		}
		
		public void draw(long nvg, float x, float y)
		{
			Shapes.drawBox(nvg, x + relX, y + relY, width, height, nvgColor, GUIColors.outline);
		}
		
		public boolean inside(float x, float y)
		{
			float halfW = width / 2f;
			float halfH = height / 2f;
			x -= relX;
			y -= relY;
			return x > -halfW && x < halfW && y > -halfH && y < halfH;
		}
		
		public Color getColor()
		{
			return color;
		}
	}
}

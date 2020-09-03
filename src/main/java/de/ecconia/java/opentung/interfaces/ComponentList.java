package de.ecconia.java.opentung.interfaces;

import de.ecconia.java.opentung.PlaceableInfo;
import de.ecconia.java.opentung.components.CompBlotter;
import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.CompButton;
import de.ecconia.java.opentung.components.CompColorDisplay;
import de.ecconia.java.opentung.components.CompDelayer;
import de.ecconia.java.opentung.components.CompDisplay;
import de.ecconia.java.opentung.components.CompInverter;
import de.ecconia.java.opentung.components.CompLabel;
import de.ecconia.java.opentung.components.CompMount;
import de.ecconia.java.opentung.components.CompNoisemaker;
import de.ecconia.java.opentung.components.CompPanelButton;
import de.ecconia.java.opentung.components.CompPanelColorDisplay;
import de.ecconia.java.opentung.components.CompPanelDisplay;
import de.ecconia.java.opentung.components.CompPanelLabel;
import de.ecconia.java.opentung.components.CompPanelSwitch;
import de.ecconia.java.opentung.components.CompPeg;
import de.ecconia.java.opentung.components.CompSnappingPeg;
import de.ecconia.java.opentung.components.CompSwitch;
import de.ecconia.java.opentung.components.CompThroughBlotter;
import de.ecconia.java.opentung.components.CompThroughPeg;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;
import de.ecconia.java.opentung.settings.Settings;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NanoVG;

public class ComponentList
{
	//TODO: Automatically create this list.
	private static final PlaceableInfo[] placeableInfos = new PlaceableInfo[]{
			CompBoard.info,
			CompPeg.info,
			CompInverter.info,
			CompBlotter.info,
			CompDelayer.info,
			CompSwitch.info,
			CompButton.info,
			CompPanelSwitch.info,
			CompPanelButton.info,
			CompNoisemaker.info,
			CompMount.info,
			CompSnappingPeg.info,
			CompThroughBlotter.info,
			CompThroughPeg.info,
			CompDisplay.info,
			CompPanelDisplay.info,
			CompColorDisplay.info,
			CompPanelColorDisplay.info,
			CompLabel.info,
			CompPanelLabel.info,
	};
	private final RenderPlane2D renderPlane2D;
	private final Hotbar hotbar;
	
	public ComponentList(RenderPlane2D renderPlane2D, Hotbar hotbar)
	{
		this.hotbar = hotbar;
		this.renderPlane2D = renderPlane2D;
	}
	
	private static final float side = 100;
	private static final float padding = 10;
	
	private float[] offsetsX;
	private float[] offsetsY;
	
	private float windowStartX;
	private float windowStartY;
	private float windowWidth;
	private float windowHeight;
	
	private PlaceableInfo draggedElement;
	private float mousePosX;
	private float mousePosY;
	
	private boolean guiOperationOverwrite;
	private Integer insertedAt;
	private Integer extractedIsActive;
	
	public void draw()
	{
		int amount = placeableInfos.length;
		offsetsX = new float[amount];
		offsetsY = new float[amount];
		
		float scale = Settings.guiScale;
		
		int columns = (int) Math.ceil(Math.sqrt(amount));
		int rows = (int) Math.ceil((float) amount / (float) columns);
		
		long nvg = renderPlane2D.vg;
		
		NVGColor background = NanoVG.nvgRGBAf(0.5f, 0.5f, 0.5f, 1.0f, NVGColor.create());
		NVGColor hotbarBG = NanoVG.nvgRGBAf(0.8f, 0.8f, 0.8f, 0.3f, NVGColor.create());
		NVGColor hotbarOutline = NanoVG.nvgRGBf(0.2f, 0.2f, 0.2f, NVGColor.create());
		
		NanoVG.nvgBeginPath(nvg);
		
		windowWidth = columns * (side + padding) + padding;
		windowHeight = rows * (side + padding) + padding;
		windowStartX = (renderPlane2D.realWidth(scale) - windowWidth) / 2f;
		windowStartY = (renderPlane2D.realHeight(scale) - windowHeight) / 2f;
		
		Shapes.drawBox(nvg, windowStartX + windowWidth / 2f, windowStartY + windowHeight / 2f, windowWidth, windowHeight, background, hotbarOutline);
		
		int i = 0;
		float offsetX = windowStartX + padding + side / 2f;
		float currentY = windowStartY + padding + side / 2f;
		for(int y = 0; y < rows; y++)
		{
			float currentX = offsetX;
			for(int x = 0; x < columns; x++)
			{
				if(i >= amount)
				{
					return;
				}
				offsetsX[i] = currentX;
				offsetsY[i++] = currentY;
				Shapes.drawBox(nvg, currentX, currentY, side, side, hotbarBG, hotbarOutline);
				currentX += side + padding;
			}
			currentY += side + padding;
		}
	}
	
	public void drawIcons(ShaderProgram iconShader, GenericVAO iconPlane)
	{
		float scale = Settings.guiScale;
		iconShader.use();
		iconShader.setUniformV2(1, new float[]{(side / 2f - 5f) * scale, (side / 2f - 5f) * scale});
		iconPlane.use();
		for(int i = 0; i < placeableInfos.length; i++)
		{
			PlaceableInfo info = placeableInfos[i];
			info.getIconTexture().activate();
			float x = offsetsX[i];
			float y = offsetsY[i];
			iconShader.setUniformV2(2, new float[]{x * scale, y * scale});
			iconPlane.draw();
		}
		
		if(draggedElement != null)
		{
			draggedElement.getIconTexture().activate();
			iconShader.setUniformV2(2, new float[]{mousePosX * scale, mousePosY * scale});
			iconPlane.draw();
		}
	}
	
	public boolean leftMouseDown(int x, int y)
	{
		float scale = Settings.guiScale;
		float sx = (float) x / scale;
		float sy = (float) y / scale;
		if(downInside(sx, sy))
		{
			float half = side / 2f;
			Integer match = null;
			for(int i = 0; i < placeableInfos.length; i++)
			{
				float xx = offsetsX[i];
				float yy = offsetsY[i];
				
				float startX = xx - half;
				float endX = xx + half;
				float startY = yy - half;
				float endY = yy + half;
				
				if(startX > sx || endX < sx || startY > sy || endY < sy)
				{
					continue;
				}
				
				match = i;
				break;
			}
			
			if(match != null)
			{
				draggedElement = placeableInfos[match];
				mousePosX = sx;
				mousePosY = sy;
			}
			
			return true;
		}
		else
		{
			Integer downOn = hotbar.downOnEntry(sx, sy);
			if(downOn != null)
			{
				insertedAt = downOn;
				int active = hotbar.getActive();
				extractedIsActive = active == insertedAt ? active : null;
				return true;
			}
			else
			{
				return false;
			}
		}
	}
	
	private boolean downInside(float x, float y)
	{
		return windowStartX < x && x < (windowStartX + windowWidth) && windowStartY < y && y < (windowStartY + windowHeight);
	}
	
	public void abort()
	{
		if(insertedAt == null && extractedIsActive != null)
		{
			//Bad, now we have to fix.
			hotbar.setActive(extractedIsActive);
		}
		
		//Abort any component movement.
		draggedElement = null;
		insertedAt = null;
		extractedIsActive = null;
		guiOperationOverwrite = false;
	}
	
	public boolean leftMouseUp(int x, int y)
	{
		float scale = Settings.guiScale;
		float sx = (float) x / scale;
		float sy = (float) y / scale;
		
		if(guiOperationOverwrite)
		{
			abort();
			return true;
		}
		
		abort();
		return downInside(sx, sy);
	}
	
	public void mouseDragged(int x, int y)
	{
		float scale = Settings.guiScale;
		mousePosX = (float) x / scale;
		mousePosY = (float) y / scale;
		if(hotbar.onHotbar(mousePosY))
		{
			if(insertedAt != null)
			{
				//Probe if it got moved:
				int newIndex = hotbar.neighbourIndexOf(mousePosX, insertedAt);
				if(newIndex != insertedAt)
				{
					PlaceableInfo thing = hotbar.remove(insertedAt);
					hotbar.insert(newIndex, thing);
					insertedAt = newIndex;
					if(extractedIsActive != null)
					{
						hotbar.setActive(insertedAt);
					}
				}
			}
			else
			{
				Integer hotbarIndex = hotbar.indexOf(mousePosX);
				if(hotbarIndex != null)
				{
					//The cursor is still on the hotbar (Y-Level).
					if(draggedElement != null)
					{
						//Something just entered the hotbar, add it.
						hotbar.insert(hotbarIndex, draggedElement);
						insertedAt = hotbarIndex;
						if(extractedIsActive != null)
						{
							hotbar.setActive(insertedAt);
						}
						draggedElement = null;
					}
					else
					{
						guiOperationOverwrite = true; //Don't close the gui!
						//Its air, add it!
						hotbar.insert(hotbarIndex, null);
						insertedAt = hotbarIndex;
						if(extractedIsActive != null)
						{
							hotbar.setActive(insertedAt);
						}
					}
				}
				//else - hotbar full.
			}
		}
		else
		{
			//Mouse is not on hotbar.
			if(insertedAt != null)
			{
				//If there is something temporary inserted though:
				draggedElement = hotbar.remove(insertedAt); //Take it out again.
				//TODO: Maybe don't repeat this step over and over (its cheap though)
				if(draggedElement == null && hotbar.hasNoAir())
				{
					//Uff grabbed last air, put right back!
					hotbar.insert(insertedAt, null);
					if(extractedIsActive != null)
					{
						hotbar.setActive(insertedAt);
					}
					return;
				}
				insertedAt = null;
			}
		}
	}
}
package de.ecconia.java.opentung.interfaces.windows;

import de.ecconia.java.opentung.PlaceableInfo;
import de.ecconia.java.opentung.ShaderStorage;
import de.ecconia.java.opentung.SharedData;
import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.CompButton;
import de.ecconia.java.opentung.components.CompDisplay;
import de.ecconia.java.opentung.components.CompInverter;
import de.ecconia.java.opentung.components.CompPeg;
import de.ecconia.java.opentung.interfaces.GUIColors;
import de.ecconia.java.opentung.interfaces.RenderPlane2D;
import de.ecconia.java.opentung.interfaces.Shapes;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;
import de.ecconia.java.opentung.settings.Settings;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NanoVG;

public class Hotbar
{
	private final RenderPlane2D plane;
	private final SharedData sharedData;
	
	private static final NVGColor hotbarBG = GUIColors.background;
	private static final NVGColor hotbarOutline = GUIColors.outline;
	private static final NVGColor hotbarOutlineActive = GUIColors.outlineHighlighted;
	
	private static final float side = 100;
	private static final float padding = 20f;
	
	//Graphic thread data:
	private final PlaceableInfo[] r_slots = new PlaceableInfo[20];
	private int r_count = 1; //How many slots are in use.
	private int r_active; //Which slot is selected.
	
	//The Offsets can be shared on both threads, it just acts as a flag and even if there is an issue, it would only be visual for 1 frame.
	//Not for these values to change significantly, the window has to be resized, the scale adjusted. Unlikely to happen while dragging stuff.
	private float yOffset;
	private float xOffset;
	
	public Hotbar(RenderPlane2D plane, SharedData sharedData)
	{
		this.plane = plane;
		this.sharedData = sharedData;
		
		//Default items, currently quite random:
		i_slots[1] = CompBoard.info;
		i_slots[2] = CompPeg.info;
		i_slots[3] = CompInverter.info;
		i_slots[4] = CompButton.info;
		i_slots[5] = CompDisplay.info;
		i_count = 6;
		
		//TODO: Load save hotbar, including active slot.
		sharedData.setCurrentPlaceable(i_slots[i_active]); //Run this on start, the hotbar will eventually be custom.
	}
	
	private void recalcOffsets()
	{
		float scale = Settings.guiScale;
		yOffset = plane.realHeight(scale) - 60; //Magic value offset.
		
		if(r_count != 0) //Should never happen. But well.
		{
			float middle = plane.realWidth(scale) / 2f;
			//Count of fields by padding and side.
			//Sub one padding, cause its too much at the outside.
			//Sub one time side, cause one half of it is too much at both outsides.
			float fullWidthFromCenterToCenter = (float) r_count * (side + padding) - padding - side;
			//Subtract half of it, off the middle.
			xOffset = middle - fullWidthFromCenterToCenter / 2f;
		}
	}
	
	public void draw()
	{
		copyInputDataToRenderThread();
		recalcOffsets();
		
		if(r_count != 0)
		{
			NanoVG.nvgStrokeWidth(plane.vg, 3);
			
			float x = xOffset;
			Shapes.drawBox(plane.vg, x, yOffset, side, side, hotbarBG, r_active == 0 ? hotbarOutlineActive : hotbarOutline);
			for(int i = 1; i < r_count; i++)
			{
				x += padding + side; //Use unscaled width.
				Shapes.drawBox(plane.vg, x, yOffset, side, side, hotbarBG, r_active == i ? hotbarOutlineActive : hotbarOutline);
			}
		}
	}
	
	public void drawIcons(ShaderStorage shaderStorage)
	{
		if(i_count == 0)
		{
			return;
		}
		
		ShaderProgram textureShader = shaderStorage.getFlatTextureShader();
		textureShader.use();
		//Size:
		float scale = Settings.guiScale;
		textureShader.setUniformV2(1, new float[]{(side / 2f - 5f) * scale, (side / 2f - 5f) * scale});
		
		GenericVAO texturePlane = shaderStorage.getFlatTexturePlane();
		texturePlane.use();
		
		float width = (side + padding) * scale;
		float x = xOffset * scale; //Scale to actual position.
		for(int i = 0; i < r_count; i++)
		{
			PlaceableInfo info = r_slots[i];
			if(info != null)
			{
				info.getIconTexture().activate();
				
				//Offset:
				textureShader.setUniformV2(2, new float[]{x, yOffset * scale});
				texturePlane.draw();
			}
			
			x += width;
		}
	}
	
	// ### Above is RENDER Thread ###
	
	private final PlaceableInfo[] i_slots = new PlaceableInfo[r_slots.length];
	private int i_count = 1; //How many slots are in use.
	private int i_active; //Which slot is selected.
	
	private void copyInputDataToRenderThread()
	{
		System.arraycopy(i_slots, 0, r_slots, 0, r_slots.length);
		r_count = i_count;
		r_active = i_active;
	}
	
	// ### Below is INPUT Thread ###
	
	public void scrollInput(int val)
	{
		if(Settings.horizontalSwapped)
		{
			val = -val;
		}
		int max = i_count - 1;
		int newActive = i_active;
		newActive += val;
		
		while(newActive < 0)
		{
			newActive += max + 1;
		}
		while(newActive > max)
		{
			newActive -= max + 1;
		}
		
		i_active = newActive;
		
		activeUpdated();
	}
	
	public void numberInput(int index)
	{
		if(index >= i_count)
		{
			index = i_count - 1;
		}
		i_active = index;
		activeUpdated();
	}
	
	public void selectOrAdd(PlaceableInfo info)
	{
		for(int i = 0; i < i_count; i++)
		{
			if(i_slots[i] == info)
			{
				i_active = i;
				activeUpdated();
				return;
			}
		}
		justAdd(info);
	}
	
	public void justAdd(PlaceableInfo info)
	{
		//Append at end:
		if(i_count == i_slots.length)
		{
			//Hotbar full.
			return;
		}
		i_slots[i_count] = info;
		i_count++;
		i_active = i_count - 1;
		activeUpdated();
	}
	
	public void dropHotbarEntry()
	{
		//Special case, cause we never want the bar empty.
		if(i_count == 1)
		{
			return;
		}
		
		PlaceableInfo removed = i_slots[i_active];
		i_count--;
		for(int i = i_active; i < i_count; i++)
		{
			i_slots[i] = i_slots[i + 1];
		}
		
		if(removed == null)
		{
			//check if there is one more...
			for(int i = 0; i < i_count; i++)
			{
				if(i_slots[i] == null)
				{
					if(i_active >= i_count)
					{
						i_active = i_count - 1;
					}
					activeUpdated();
					return;
				}
			}
			
			//TBI: Alternatively add the empty slot at the end again. Middle mouse click would have done that anyway.
			//Add back, there should be at least 1 empty slot. For reasons.
			for(int i = i_count - 1; i >= i_active; i--)
			{
				i_slots[i + 1] = i_slots[i];
			}
			i_count++;
			i_slots[i_active] = null;
		}
		else
		{
			if(i_active >= i_count)
			{
				i_active = i_count - 1;
			}
			activeUpdated();
		}
	}
	
	private void activeUpdated()
	{
		if(i_active >= i_count)
		{
			sharedData.setCurrentPlaceable(null);
		}
		else
		{
			sharedData.setCurrentPlaceable(i_slots[i_active]);
		}
	}
	
	public boolean onHotbar(float y)
	{
		return (yOffset - side / 2f) < y;
	}
	
	public Integer indexOf(float x)
	{
		if(i_count == i_slots.length)
		{
			return null; //Is full, don't claim a free spot.
		}
		
		float offset = xOffset;
		for(int i = 0; i < i_count; i++)
		{
			if(x < offset)
			{
				return i;
			}
			offset += padding + side;
		}
		
		return i_count;
	}
	
	public int neighbourIndexOf(float x, int current)
	{
		float center = xOffset + current * (padding + side);
		if(x > center)
		{
			//Subtract until at the exact middle between tiles, else it flickers when moving.
			float distance = x - center - (side + padding) / 2f;
			if(distance < 0)
			{
				//Still on the tile.
				return current;
			}
			int steps = (int) (distance / (padding + side)) + 1;
			int index = current + steps;
			if(index >= i_count)
			{
				index = i_count - 1;
			}
			return index;
		}
		else
		{
			//Subtract until at the exact middle between tiles, else it flickers when moving.
			float distance = center - x - (side + padding) / 2f;
			if(distance < 0)
			{
				//Still on the tile.
				return current;
			}
			int steps = (int) (distance / (padding + side)) + 1;
			int index = current - steps;
			if(index < 0)
			{
				index = 0;
			}
			return index;
		}
	}
	
	public void insert(Integer hotbarIndex, PlaceableInfo placeableInfo)
	{
		if(hotbarIndex == i_count)
		{
			i_slots[i_count] = placeableInfo;
			i_count++;
		}
		else
		{
			for(int i = i_count; i > hotbarIndex; i--)
			{
				i_slots[i] = i_slots[i - 1];
			}
			i_slots[hotbarIndex] = placeableInfo;
			i_count++;
			if(i_active >= hotbarIndex)
			{
				i_active++;
			}
		}
	}
	
	public PlaceableInfo remove(int removeIndex)
	{
		PlaceableInfo removed = i_slots[removeIndex];
		i_count--;
		for(int i = removeIndex; i < i_count; i++)
		{
			i_slots[i] = i_slots[i + 1];
		}
		if(removeIndex == i_active)
		{
			i_active = 20; //Remove active element, ComponentList knows what it does.
			activeUpdated();
		}
		else if(i_active > removeIndex)
		{
			i_active--;
		}
		return removed;
	}
	
	public Integer downOnEntry(float sx, float sy)
	{
		if((yOffset - side / 2f) < sy)
		{
			//In correct Y level.
			float offset = xOffset - side / 2f;
			if(sx < offset)
			{
				return null;
			}
			for(int i = 0; i < i_count; i++)
			{
				offset += side;
				if(sx < offset)
				{
					return i;
				}
				offset += padding;
				if(sx < offset)
				{
					return null;
				}
			}
		}
		return null;
	}
	
	public boolean hasNoAir()
	{
		for(int i = 0; i < i_count; i++)
		{
			if(i_slots[i] == null)
			{
				return false;
			}
		}
		return true;
	}
	
	public void setActive(int index)
	{
		if(index >= i_count)
		{
			index = i_count - 1;
		}
		i_active = index;
		activeUpdated();
	}
	
	public int getActive()
	{
		return i_active;
	}
}

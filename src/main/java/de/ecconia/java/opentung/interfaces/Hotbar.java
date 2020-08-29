package de.ecconia.java.opentung.interfaces;

import de.ecconia.java.opentung.PlaceableInfo;
import de.ecconia.java.opentung.SharedData;
import de.ecconia.java.opentung.components.CompBlotter;
import de.ecconia.java.opentung.components.CompDelayer;
import de.ecconia.java.opentung.components.CompInverter;
import de.ecconia.java.opentung.components.CompLabel;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;
import de.ecconia.java.opentung.settings.Settings;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NanoVG;

public class Hotbar
{
	private final RenderPlane2D plane;
	
	private final SharedData sharedData;
	private final List<PlaceableInfo> slots = new ArrayList<>();
	private int active = 0;
	
	private static final float side = 100;
	private static final float padding = 20f;
	
	//Tmp data:
	private float[] xOffsets;
	private float yOffset;
	
	public Hotbar(RenderPlane2D plane, SharedData sharedData)
	{
		this.plane = plane;
		this.sharedData = sharedData;
		slots.add(null);
		
		slots.add(CompInverter.info);
		slots.add(CompBlotter.info);
		slots.add(CompDelayer.info);
		slots.add(CompInverter.info);
		slots.add(CompLabel.info);
	}
	
	public void draw()
	{
		int slotCount = slots.size();
		if(slotCount != 0)
		{
			float scale = Settings.guiScale;
			NVGColor hotbarBG = NanoVG.nvgRGBAf(0.8f, 0.8f, 0.8f, 0.3f, NVGColor.create());
			NVGColor hotbarOutline = NanoVG.nvgRGBf(0.2f, 0.2f, 0.2f, NVGColor.create());
			NVGColor hotbarOutlineActive = NanoVG.nvgRGBf(1.0f, 1.0f, 1.0f, NVGColor.create());
			NanoVG.nvgStrokeWidth(plane.vg, 3);
			float middle = plane.realWidth(scale) / 2f;
			yOffset = plane.realHeight(scale) - 60; //Magic value offset.
			
			xOffsets = new float[slotCount];
			xOffsets[0] = middle
					- (float) (slotCount / 2) * side
					- (float) (slotCount / 2) * padding;
			if((slotCount & 1) == 0)
			{
				xOffsets[0] += (side + padding) / 2f;
			}
			
			Shapes.drawBox(plane.vg, xOffsets[0], yOffset, side, side, hotbarBG, active == 0 ? hotbarOutlineActive : hotbarOutline);
			for(int i = 1; i < slotCount; i++)
			{
				xOffsets[i] = xOffsets[i - 1] + side + padding;
				Shapes.drawBox(plane.vg, xOffsets[i], yOffset, side, side, hotbarBG, active == i ? hotbarOutlineActive : hotbarOutline);
			}
		}
	}
	
	public void drawIcons(ShaderProgram iconShader, GenericVAO iconPlane)
	{
		if(slots.isEmpty())
		{
			return;
		}
		
		float scale = Settings.guiScale;
		iconShader.use();
		//Size:
		iconShader.setUniformV2(1, new float[]{(side / 2f - 5f) * scale, (side / 2f - 5f) * scale});
		
		iconPlane.use();
		for(int i = 0; i < slots.size(); i++)
		{
			PlaceableInfo info = slots.get(i);
			if(info != null)
			{
				info.getIconTexture().activate();
				
				float xOffset = xOffsets[i];
				//Offset:
				iconShader.setUniformV2(2, new float[]{xOffset * scale, yOffset * scale});
				iconPlane.draw();
			}
		}
	}
	
	public void scrollInput(int val)
	{
		int max = slots.size() - 1;
		active += val;
		
		while(active < 0)
		{
			active += max + 1;
		}
		while(active > max)
		{
			active -= max + 1;
		}
		
		activeUpdated();
	}
	
	private void activeUpdated()
	{
		sharedData.setCurrentPlaceable(slots.get(active));
	}
	
	public void numberInput(int index)
	{
		if(index >= slots.size())
		{
			index = slots.size() - 1;
		}
		active = index;
		activeUpdated();
	}
	
	public void setInfo(PlaceableInfo info)
	{
		for(int i = 0; i < slots.size(); i++)
		{
			if(slots.get(i) == info)
			{
				active = i;
				activeUpdated();
				return;
			}
		}
		
		//Else append at end:
		active = slots.size();
		slots.add(info);
		activeUpdated();
	}
}

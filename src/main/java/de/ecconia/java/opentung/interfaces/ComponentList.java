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
	
	public ComponentList(RenderPlane2D renderPlane2D, Hotbar hotbar)
	{
		this.renderPlane2D = renderPlane2D;
	}
	
	private static final float side = 100;
	private static final float padding = 10;
	
	private float[] offsetsX;
	private float[] offsetsY;
	
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
		
		float width = columns * (side + padding) + padding;
		float height = rows * (side + padding) + padding;
		float startX = (renderPlane2D.realWidth(scale) - width) / 2f;
		float startY = (renderPlane2D.realHeight(scale) - height) / 2f;
		
		Shapes.drawBox(nvg, startX + width / 2f, startY + height / 2f, width, height, background, hotbarOutline);
		
		int i = 0;
		float offsetX = startX + padding + side / 2f;
		float currentY = startY + padding + side / 2f;
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
	}
}

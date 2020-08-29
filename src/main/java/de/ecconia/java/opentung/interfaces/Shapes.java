package de.ecconia.java.opentung.interfaces;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NanoVG;

public class Shapes
{
	public static void drawBox(long vg, float x, float y, float width, float height, NVGColor background, NVGColor outline)
	{
		final float extraBorder = 10f;
		float leftO = x + -width / 2f;
		float leftI = leftO + extraBorder;
		float rightO = x + width / 2f;
		float rightI = rightO - extraBorder;
		float topO = y + -height / 2f;
		float topI = topO + extraBorder;
		float bottomO = y + height / 2f;
		float bottomI = bottomO - extraBorder;
		
		NanoVG.nvgBeginPath(vg);
		NanoVG.nvgMoveTo(vg, leftI, topO);
		NanoVG.nvgLineTo(vg, rightI, topO);
		NanoVG.nvgArcTo(vg, rightO, topO,
				rightO, topI, extraBorder);
		NanoVG.nvgLineTo(vg, rightO, bottomI);
		NanoVG.nvgArcTo(vg, rightO, bottomO,
				rightI, bottomO, extraBorder);
		NanoVG.nvgLineTo(vg, leftI, bottomO);
		NanoVG.nvgArcTo(vg, leftO, bottomO,
				leftO, bottomI, extraBorder);
		NanoVG.nvgLineTo(vg, leftO, topI);
		NanoVG.nvgArcTo(vg, leftO, topO,
				leftI, topO, extraBorder);
		NanoVG.nnvgClosePath(vg);
		
		NanoVG.nvgFillColor(vg, background);
		NanoVG.nvgFill(vg);
		NanoVG.nvgStrokeColor(vg, outline);
		NanoVG.nvgStroke(vg);
	}
}

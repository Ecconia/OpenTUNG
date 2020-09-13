package de.ecconia.java.opentung.interfaces;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NanoVG;

public class GUIColors
{
	public static final NVGColor background = NanoVG.nvgRGBAf(0.5f, 0.5f, 0.5f, 1.0f, NVGColor.create());
	public static final NVGColor outline = NanoVG.nvgRGBf(0.2f, 0.2f, 0.2f, NVGColor.create());
	public static final NVGColor outlineHighlighted = NanoVG.nvgRGBf(1.0f, 1.0f, 1.0f, NVGColor.create());
}

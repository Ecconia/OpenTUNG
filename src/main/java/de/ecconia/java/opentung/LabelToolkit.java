package de.ecconia.java.opentung;

import de.ecconia.java.opentung.libwrap.LabelTextureWrapper;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LabelToolkit
{
	public static LabelTextureWrapper generateUploadTexture(String text, float textSize)
	{
		String[] lines = text.split("\n");
		
		int side = 300;
		//Generate image:
		BufferedImage image = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setColor(java.awt.Color.white);
		g.fillRect(0, 0, side, side);
		
		g.setColor(java.awt.Color.black);
		Font f = g.getFont();
		f = f.deriveFont(f.getStyle() | Font.BOLD);
		f = f.deriveFont(textSize / 11f * (float) side);
		g.setFont(f);
		
		FontMetrics m = g.getFontMetrics(f);
		int height = lines.length * m.getHeight();
		int lineHeight = m.getHeight();
		
		int offsetY = side / 2 - height / 2;
		for(int i = 0; i < lines.length; i++)
		{
			String lineText = lines[i];
			offsetY += lineHeight;
			g.drawString(lineText, 0, offsetY);
		}
		
		g.dispose();
		
		return new LabelTextureWrapper(image);
	}
	
	private final Map<LabelContainer, LabelTextureWrapper> labels = new HashMap<>();
	private LabelTextureWrapper ducky;
	
	public LabelTextureWrapper generate(String text, float fontSize)
	{
		if(ducky == null)
		{
			ducky = LabelTextureWrapper.debuggyDuck();
		}
		LabelContainer container = new LabelContainer(text, fontSize);
		
		LabelTextureWrapper texture = labels.get(container);
		if(texture != null)
		{
			return texture;
		}
		
		texture = generateUploadTexture(text, fontSize);
		labels.put(container, texture);
		return texture;
	}
	
	public int getLabelCount()
	{
		return labels.size();
	}
	
	private static class LabelContainer
	{
		private final String text;
		private final float fontSize;
		
		public LabelContainer(String text, float fontSize)
		{
			this.text = text;
			this.fontSize = fontSize;
		}
		
		@Override
		public boolean equals(Object o)
		{
			if(this == o)
			{
				return true;
			}
			if(o == null || getClass() != o.getClass())
			{
				return false;
			}
			LabelContainer that = (LabelContainer) o;
			return Float.compare(that.fontSize, fontSize) == 0 &&
					Objects.equals(text, that.text);
		}
		
		@Override
		public int hashCode()
		{
			return Objects.hash(text, fontSize);
		}
	}
}

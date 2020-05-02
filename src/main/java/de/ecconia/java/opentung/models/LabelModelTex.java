package de.ecconia.java.opentung.models;

import de.ecconia.java.opentung.libwrap.TextureWrapper;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class LabelModelTex extends GenericModel
{
	public LabelModelTex()
	{
		//Create:
		vertices = new float[]{
				-0.15f, 0.3f + 0.075f, -0.15f, 1, 0,
				+0.15f, 0.3f + 0.075f, -0.15f, 0, 0,
				+0.15f, 0.3f + 0.075f, +0.15f, 0, 1,
				-0.15f, 0.3f + 0.075f, +0.15f, 1, 1,
		};
		
		indices = new short[]{
				0, 1, 2,
				0, 3, 2,
		};
		
		upload(ShaderType.LabelShader);
	}
	
	public static TextureWrapper generateUploadTexture(String text, float textSize)
	{
		String[] lines = text.split("\n");
//		System.out.println("Lines: " + lines.length + " Size: " + textSize);
		
		int side = 300;
		//Generate image:
		BufferedImage image = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setColor(Color.white);
		g.fillRect(0, 0, side, side);
		
		g.setColor(Color.red);
		g.drawLine(0, 0, side - 1, side - 1);
		g.drawLine(side - 1, 0, 0, side - 1);
		
		g.setColor(Color.black);
		Font f = g.getFont();
		f = f.deriveFont(textSize / 10f * (float) side);
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
		
		return new TextureWrapper(image);
	}
}

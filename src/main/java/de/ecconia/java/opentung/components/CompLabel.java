package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.Direction;
import de.ecconia.java.opentung.components.fragments.TexturedFace;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.libwrap.TextureWrapper;
import de.ecconia.java.opentung.math.Vector3;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class CompLabel extends Component
{
	private static final ModelHolder modelHolder = new ModelHolder();
	
	static
	{
		modelHolder.setPlacementOffset(new Vector3(0.0, 0.15 + 0.075, 0.0));
		modelHolder.addSolid(new CubeFull(new Vector3(0.0, 0.0, 0.0), new Vector3(0.3, 0.3, 0.3), Color.material));
		modelHolder.addTexture(new TexturedFace(new Vector3(0.0, 0.0, 0.0), new Vector3(0.3, 0.3, 0.3), Direction.YPos));
	}
	
	public static void initGL()
	{
		modelHolder.generateTextureVAO();
	}
	
	@Override
	public ModelHolder getModelHolder()
	{
		return modelHolder;
	}
	
	//### Non-Static ###
	
	private String text;
	private float fontSize;
	
	private TextureWrapper texture;
	
	public CompLabel(CompContainer parent)
	{
		super(parent);
	}
	
	public void setText(String text)
	{
		this.text = text;
	}
	
	public void setFontSize(float fontSize)
	{
		this.fontSize = fontSize;
	}
	
	public String getText()
	{
		return text;
	}
	
	public float getFontSize()
	{
		return fontSize;
	}
	
	public void initialize()
	{
		texture = generateUploadTexture(text, fontSize);
	}
	
	public void activate()
	{
		texture.activate();
	}
	
	public static TextureWrapper generateUploadTexture(String text, float textSize)
	{
		String[] lines = text.split("\n");
//		System.out.println("Lines: " + lines.length + " Size: " + textSize);
		
		int side = 300;
		//Generate image:
		BufferedImage image = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setColor(java.awt.Color.white);
		g.fillRect(0, 0, side, side);
		
		g.setColor(java.awt.Color.red);
		g.drawLine(0, 0, side - 1, side - 1);
		g.drawLine(side - 1, 0, 0, side - 1);
		
		g.setColor(java.awt.Color.black);
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

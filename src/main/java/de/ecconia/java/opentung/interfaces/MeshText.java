package de.ecconia.java.opentung.interfaces;

import de.ecconia.java.opentung.libwrap.FloatShortArraysInt;
import de.ecconia.java.opentung.libwrap.TextureWrapper;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MeshText
{
	private static final int PADDING = 4;
	
	private final Font font = new Font(null, Font.PLAIN, 40);
	private final Map<Character, LetterMeta> letters = new HashMap<>();
	private final int xHeight;
	private final int nHeight;
	
	private TextureWrapper atlas;
	private boolean dirty = true;
	
	public MeshText()
	{
		addLetters("ÁÀÂxgjyqp");
		xHeight = letters.get('x').rendered.getHeight();
		System.out.println("[FontDebug] X-Height: " + xHeight);
		int max = 0;
		int min = 0;
		for(LetterMeta l : letters.values())
		{
			int lMax = l.yOffset;
			if(lMax > max)
			{
				max = lMax;
			}
			int lMin = l.rendered.getHeight() - lMax;
			if(lMin > min)
			{
				min = lMin;
			}
		}
		nHeight = min + max;
		System.out.println("[FontDebug] NormalizedHeight: " + nHeight);
		
		//Might be useful later on:
		//font.canDisplay(c); //-> Boolean
	}
	
	public void addLetters(String someText)
	{
		for(char c : someText.toCharArray())
		{
			addLetter(c);
		}
	}
	
	public void addLetter(char c)
	{
		if(!letters.containsKey(c))
		{
			letters.put(c, new LetterMeta(c));
			dirty = true;
		}
	}
	
	public void createAtlas()
	{
		if(dirty)
		{
			if(atlas != null)
			{
				atlas.unload();
			}
			List<LetterMeta> copy = letters.values().stream().filter(e -> e.rendered != null).sorted(new Comparator<LetterMeta>()
			{
				@Override
				public int compare(LetterMeta a, LetterMeta b)
				{
					return b.rendered.getHeight() - a.rendered.getHeight();
				}
			}).collect(Collectors.toList());
			
			int width = 0;
			int max = 0;
			for(LetterMeta l : copy)
			{
				width += l.rendered.getWidth() + PADDING;
				if(l.rendered.getHeight() > max)
				{
					max = l.rendered.getHeight();
				}
			}
			
			//Make width a power of 4. Needed for OpenGL (alternative change OpenGL settings).
			int rem = width % 4;
			if(rem != 0)
			{
				width += 4 - rem;
			}
			
			float texHeightPixel = 1f / (float) max;
			float texWidthPixel = 1f / (float) width;
			
			BufferedImage image = new BufferedImage(width, max, BufferedImage.TYPE_BYTE_GRAY);
			Graphics g = image.getGraphics();
			g.setColor(Color.white);
			g.fillRect(0, 0, width, max);
			int x = 0;
			for(LetterMeta l : copy)
			{
				g.drawImage(l.rendered, x, 0, null);
				l.textureBounds = new Rectangle2D.Float(
						texWidthPixel * x,
						0,
						texWidthPixel * l.rendered.getWidth(),
						texHeightPixel * l.rendered.getHeight());
				x += l.rendered.getWidth() + PADDING;
			}
			g.dispose();
			
			atlas = TextureWrapper.createFontAtlasTexture(image);
			dirty = false;
		}
	}
	
	public FloatShortArraysInt fillArray(String textToRender, int targetFontSize)
	{
		//Collect letters:
		ArrayList<LetterMeta> lettersUsed = new ArrayList<>(textToRender.length());
		int visibleAmount = 0;
		for(char c : textToRender.toCharArray())
		{
			LetterMeta meta = letters.get(c);
			lettersUsed.add(meta);
			if(meta.rendered != null)
			{
				visibleAmount++;
			}
		}
		
		short[] indices = new short[visibleAmount * 6];
		int offset = 0;
		int ii = 0;
		for(int i = 0; i < visibleAmount; i++)
		{
			int first = offset;
			indices[ii++] = (short) (first + 0);
			indices[ii++] = (short) (first + 1);
			indices[ii++] = (short) (first + 2);
			indices[ii++] = (short) (first + 1);
			indices[ii++] = (short) (first + 3);
			indices[ii++] = (short) (first + 2);
			offset += 4;
		}
		
		float scale = (float) targetFontSize / (float) nHeight;
		
		float[] vertices = new float[visibleAmount * 16];
		float xOffset = 0;
		int i = 0;
		for(LetterMeta letter : lettersUsed)
		{
			if(letter.rendered != null)
			{
				float yOffset = ((float) letter.yOffset + (float) xHeight / 2f) * scale;
				float w = (float) letter.rendered.getWidth() * scale;
				float h = (float) letter.rendered.getHeight() * scale;
				Rectangle2D r = letter.textureBounds;
				//TBI: Swapped Texture-Y, find better solution.
				//-1, -1, 0, 0, // L T
				vertices[i++] = xOffset; //PX
				vertices[i++] = yOffset; //PY
				vertices[i++] = (float) (r.getX()); //TX
				vertices[i++] = (float) (r.getY()); //TY
				//-1, +1, 0, 1, // L B
				vertices[i++] = xOffset; //PX
				vertices[i++] = yOffset + h; //PY
				vertices[i++] = (float) (r.getX()); //TX
				vertices[i++] = (float) (r.getY() + r.getHeight()); //TY
				//+1, -1, 1, 0, // R T
				vertices[i++] = xOffset + w; //PX
				vertices[i++] = yOffset; //PY
				vertices[i++] = (float) (r.getX() + r.getWidth()); //TX
				vertices[i++] = (float) (r.getY()); //TY
				//+1, +1, 1, 1, // R B
				vertices[i++] = xOffset + w; //PX
				vertices[i++] = yOffset + h; //PY
				vertices[i++] = (float) (r.getX() + r.getWidth()); //TX
				vertices[i++] = (float) (r.getY() + r.getHeight()); //TY
			}
			xOffset += letter.lWidth * scale;
		}
		
		return new FloatShortArraysInt(vertices, indices, (int) xOffset);
	}
	
	public void activate()
	{
		atlas.activate();
	}

	private class LetterMeta
	{
		private final char letter; //For debug messages.
		private final BufferedImage rendered;
		private final float lWidth;
		private final int yOffset;
		
		private Rectangle2D textureBounds;
		
		public LetterMeta(char c)
		{
			this.letter = c;
			
			char[] text = new char[]{c};
			AffineTransform transformation = new AffineTransform();
			FontRenderContext frc = new FontRenderContext(transformation, true, false); //TODO: Does it use second rendering hint?
			GlyphVector gv = font.createGlyphVector(frc, text);
			
			Rectangle2D lBounds = gv.getLogicalBounds();
			//TODO: Which width to use? The logical might be too wide and the visual too tight.
			lWidth = (float) lBounds.getWidth();
			
			Rectangle2D vBounds = gv.getVisualBounds();
			int w = (int) Math.ceil(vBounds.getWidth());
			int h = (int) Math.ceil(vBounds.getHeight());
			
			if(w == 0 || h == 0)
			{
				rendered = null;
				yOffset = 0;
				return;
			}
			
			rendered = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
			Graphics2D g = (Graphics2D) rendered.getGraphics();
			g.setColor(Color.white);
			g.fillRect(0, 0, w, h);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
			g.setFont(font);
			
			g.setColor(Color.black);
			yOffset = (int) Math.floor(vBounds.getY());
			g.drawChars(text, 0, 1, -(int) Math.floor(vBounds.getX()), -yOffset);
			g.dispose();
		}
	}
}

package de.ecconia.java.opentung.libwrap;

import java.awt.image.BufferedImage;

import de.ecconia.java.opentung.settings.Settings;

public class SDF2
{
	public static BufferedImage start(BufferedImage image)
	{
		double spread = 25;
		
		int width = image.getWidth();
		int height = image.getHeight();
		
		double downscale = (double) width / (double) Settings.labelSDFTexturePixelResolution; //Uff but yes.
		
		Field fieldBitmap = new Field(width, height);
		
		int x, y;
		
		for(int i = 0; i < width * height; i++)
		{
			x = i % width;
			y = i / width;
			
			int argb = image.getRGB(x, y);
			int bit = !inside(argb) ? 0xff : 0x00;
			fieldBitmap.set(x, y, bit);
		}
		
		int outWidth = (int) Math.floor((double) width / downscale);
		int outHeight = (int) Math.floor((double) height / downscale);
		
		Field fieldOutput = new Field(outWidth, outHeight);
		compute(fieldOutput, fieldBitmap, width, height, spread, downscale);
		
		BufferedImage newImage = new BufferedImage(outWidth, outHeight, BufferedImage.TYPE_INT_ARGB);
		
		for(int i = 0; i < outWidth * outHeight; i++)
		{
			x = i % outWidth;
			y = i / outHeight;
			
			int c = fieldOutput.get(x, y);
			int color = c << 24 | c << 16 | c << 8 | c;
			newImage.setRGB(x, y, color);
		}
		
		return newImage;
	}
	
	private static void compute(Field output, Field bitmap, int width, int height, double spread, double downscale)
	{
		int outWidth = output.width;
		int outHeight = output.height;
		
		for(int y = 0; y < outHeight; ++y)
		{
			for(int x = 0; x < outWidth; ++x)
			{
				int centerX = (int) Math.floor((double) x * downscale + downscale / 2D);
				int centerY = (int) Math.floor((double) y * downscale + downscale / 2D);
				
				double signedDistance = findSignedDistance(bitmap, width, height, centerX, centerY, spread);
				
				double alpha = 0.5 + 0.5 * (signedDistance / spread);
				if(alpha < 0)
				{
					alpha = 0;
				}
				else if(alpha > 1)
				{
					alpha = 1;
				}
				int alpha1 = (int) Math.floor(alpha * 0xff);
				output.set(x, y, alpha1);
			}
		}
	}
	
	private static double findSignedDistance(Field bitmap, int width, int height, int centerX, int centerY, double spread)
	{
		byte base = bitmap.get(centerX, centerY);
		
		int delta = (int) Math.ceil(spread);
		int startX = Math.max(0, centerX - delta);
		int endX = Math.min(width - 1, centerX + delta);
		int startY = Math.max(0, centerY - delta);
		int endY = Math.min(height - 1, centerY + delta);
		
		double closestSquareDist = delta * delta;
		
		for(int y = startY; y <= endY; ++y)
		{
			for(int x = startX; x <= endX; ++x)
			{
				if(base != bitmap.get(x, y))
				{
					double sqDist = squareDist(centerX, centerY, x, y);
					if(sqDist < closestSquareDist)
					{
						closestSquareDist = sqDist;
					}
				}
			}
		}
		
		double closestDist = Math.sqrt(closestSquareDist);
		return (base == (byte) 0xff ? 1 : -1) * Math.min(closestDist, spread);
	}
	
	private static double squareDist(double x1, double y1, double x2, double y2)
	{
		double dx = x1 - x2;
		double dy = y1 - y2;
		return dx * dx + dy * dy;
	}
	
	private static boolean inside(int argb)
	{
		int t = 128;
		return ((argb >> 24) & 0xff) > t && (((argb >> 16) & 0xff) > t || ((argb >> 8) & 0xff) > t || (argb & 0xff) > t);
	}
	
	private static class Field
	{
		private final byte[] scratch;
		private final int width;
		private final int height;
		
		public Field(int width, int height)
		{
			this.width = width;
			this.height = height;
			scratch = new byte[width * height];
		}
		
		public void set(int x, int y, int bit)
		{
			scratch[y * width + x] = (byte) bit;
		}
		
		public byte get(int x, int y)
		{
			return scratch[y * width + x];
		}
	}
}

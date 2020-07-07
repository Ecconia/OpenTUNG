package de.ecconia.java.opentung.libwrap;

import java.awt.image.BufferedImage;

public class SDF1
{
	private static class Point
	{
		private int dx, dy;
		
		public Point(int dx, int dy)
		{
			this.dx = dx;
			this.dy = dy;
		}
		
		int distSq()
		{
			return dx * dx + dy * dy;
		}
		
		Point cloneIt()
		{
			return new Point(dx, dy);
		}
	}
	
	private static class Grid
	{
		private final Point[][] grid;
		
		public Grid(int width, int height)
		{
			grid = new Point[height][width];
		}
		
		private Point get(int x, int y)
		{
			if(x >= 0 && y >= 0 && x < grid[0].length && y < grid.length)
			{
				return grid[y][x];
			}
			else
			{
				return empty;
			}
		}
		
		private void put(int x, int y, Point p)
		{
			grid[y][x] = p;
		}
		
		private Point compare(Point p, int x, int y, int offsetX, int offsetY)
		{
			Point other = get(x + offsetX, y + offsetY).cloneIt();
			other.dx += offsetX;
			other.dy += offsetY;
			
			if(other.distSq() < p.distSq())
			{
				return other;
			}
			else
			{
				return p;
			}
		}
		
		private void generateSDF()
		{
			// Pass 0
			for(int y = 0; y < grid.length; y++)
			{
				for(int x = 0; x < grid[0].length; x++)
				{
					Point p = get(x, y);
					p = compare(p, x, y, -1, 0);
					p = compare(p, x, y, 0, -1);
					p = compare(p, x, y, -1, -1);
					p = compare(p, x, y, 1, -1);
					put(x, y, p);
				}
				
				for(int x = grid[0].length - 1; x >= 0; x--)
				{
					Point p = get(x, y);
					p = compare(p, x, y, 1, 0);
					put(x, y, p);
				}
			}
			
			// Pass 1
			for(int y = grid.length - 1; y >= 0; y--)
			{
				for(int x = grid[0].length - 1; x >= 0; x--)
				{
					Point p = get(x, y);
					p = compare(p, x, y, 1, 0);
					p = compare(p, x, y, 0, 1);
					p = compare(p, x, y, -1, 1);
					p = compare(p, x, y, 1, 1);
					put(x, y, p);
				}
				
				for(int x = 0; x < grid[0].length; x++)
				{
					Point p = get(x, y);
					p = compare(p, x, y, -1, 0);
					put(x, y, p);
				}
			}
		}
	}
	
	private static Point empty = new Point(9999, 9999);
	private static Point inside = new Point(0, 0);
	
	public static BufferedImage start(BufferedImage image)
	{
		int width = image.getWidth();
		int height = image.getHeight();
		Grid grid1 = new Grid(width, height);
		Grid grid2 = new Grid(width, height);
		
		for(int y = 0; y < height; y++)
		{
			for(int x = 0; x < width; x++)
			{
				int argb = image.getRGB(x, y);
//				byte r = (byte) ((argb >> 16) & 0xff);
				byte g = (byte) ((argb >> 8) & 0xff);
//				byte b = (byte) ((argb >> 0) & 0xff);
				
				// Points inside get marked with a dx/dy of zero.
				// Points outside get marked with an infinitely large distance.
				if(g < 0)
				{
					grid1.put(x, y, inside);
					grid2.put(x, y, empty);
				}
				else
				{
					grid2.put(x, y, inside);
					grid1.put(x, y, empty);
				}
			}
		}
		
		grid1.generateSDF();
		grid2.generateSDF();
		
		for(int y = 0; y < height; y++)
		{
			for(int x = 0; x < width; x++)
			{
				// Calculate the actual distance from the dx/dy
				int dist1 = (int) (Math.sqrt(grid1.get(x, y).distSq()));
				int dist2 = (int) (Math.sqrt(grid2.get(x, y).distSq()));
				int dist = dist1 - dist2;
				
				// Clamp and scale it, just for display purposes.
				int c = dist * 3 + 128;
				if(c < 0)
				{
					c = 0;
				}
				if(c > 255)
				{
					c = 255;
				}
				
				image.setRGB(x, y, c << 24 | c << 16 | c << 8 | c);
			}
		}
		
		return image;
	}
}

package de.ecconia.java.opentung.util;

import de.ecconia.java.opentung.util.math.Vector3;

public class MinMaxBox
{
	private Vector3 min;
	private Vector3 max;
	
	public MinMaxBox(Vector3 a, Vector3 b)
	{
		double sx;
		double bx;
		if(a.getX() < b.getX())
		{
			sx = a.getX();
			bx = b.getX();
		}
		else
		{
			sx = b.getX();
			bx = a.getX();
		}
		
		double sy;
		double by;
		if(a.getY() < b.getY())
		{
			sy = a.getY();
			by = b.getY();
		}
		else
		{
			sy = b.getY();
			by = a.getY();
		}
		
		double sz;
		double bz;
		if(a.getZ() < b.getZ())
		{
			sz = a.getZ();
			bz = b.getZ();
		}
		else
		{
			sz = b.getZ();
			bz = a.getZ();
		}
		
		min = new Vector3(sx, sy, sz);
		max = new Vector3(bx, by, bz);
	}
	
	public MinMaxBox(Vector3 vec)
	{
		min = vec;
		max = vec;
	}
	
	public MinMaxBox(MinMaxBox that)
	{
		this.min = that.min;
		this.max = that.max;
	}
	
	public boolean contains(Vector3 vec)
	{
		return min.getX() <= vec.getX() && max.getX() >= vec.getX() && min.getY() <= vec.getY() && max.getY() >= vec.getY() && min.getZ() <= vec.getZ() && max.getZ() >= vec.getZ();
	}
	
	public void expand(Vector3 vec)
	{
		if(vec.getX() > max.getX())
		{
			max = new Vector3(vec.getX(), max.getY(), max.getZ());
		}
		else if(vec.getX() < min.getX())
		{
			min = new Vector3(vec.getX(), min.getY(), min.getZ());
		}
		
		if(vec.getY() > max.getY())
		{
			max = new Vector3(max.getX(), vec.getY(), max.getZ());
		}
		else if(vec.getY() < min.getY())
		{
			min = new Vector3(min.getX(), vec.getY(), min.getZ());
		}
		
		if(vec.getZ() > max.getZ())
		{
			max = new Vector3(max.getX(), max.getY(), vec.getZ());
		}
		else if(vec.getZ() < min.getZ())
		{
			min = new Vector3(min.getX(), min.getY(), vec.getZ());
		}
	}
	
	public void expand(MinMaxBox that)
	{
		expand(that.min);
		expand(that.max);
	}
	
	@Override
	public String toString()
	{
		return "[X: " + Ansi.yellow + r(min.getX()) + " " + r(max.getX()) + Ansi.r + " Y: " + Ansi.yellow + r(min.getY()) + " " + r(max.getY()) + Ansi.r + " Z: " + Ansi.yellow + r(min.getZ()) + " " + r(max.getZ()) + Ansi.r + "]";
	}
	
	private String r(double v)
	{
		v /= 0.075;
		v = Math.round(v);
		v *= 0.075;
		String s = String.valueOf(v);
		String o = s;
		if(s.length() > 7)
		{
			s = s.substring(0, s.length() - 1); //Get rid of 8 or so.
			if(s.charAt(s.length() - 1) == '9')
			{
				while(s.charAt(s.length() - 1) == '9')
				{
					s = s.substring(0, s.length() - 1);
				}
				
				if(s.charAt(s.length() - 1) == '.')
				{
					System.out.println("Issue: " + s + " ~ " + o);
				}
				else
				{
					s = s.substring(0, s.length() - 1) + (char) (s.charAt(s.length() - 1) + 1);
				}
			}
			else
			{
				throw new RuntimeException("Float shortening, handle: " + v);
			}
		}
		return s;
	}
	
	public Vector3 getMin()
	{
		return min;
	}
	
	public Vector3 getMax()
	{
		return max;
	}
}

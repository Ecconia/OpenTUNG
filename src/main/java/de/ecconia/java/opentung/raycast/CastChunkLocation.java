package de.ecconia.java.opentung.raycast;

public class CastChunkLocation
{
	private final int x;
	private final int y;
	private final int z;
	
	public CastChunkLocation(int x, int y, int z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public int getX()
	{
		return x;
	}
	
	public int getY()
	{
		return y;
	}
	
	public int getZ()
	{
		return z;
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
		CastChunkLocation that = (CastChunkLocation) o;
		return x == that.x &&
				y == that.y &&
				z == that.z;
	}
	
	@Override
	public int hashCode()
	{
		return 31 * (31 * (31 + x) + y) + z;
	}
	
	@Override
	public String toString()
	{
		return "[" + x + " " + y + " " + z + "]";
	}
}

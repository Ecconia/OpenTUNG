package de.ecconia.java.opentung.units;

import java.util.LinkedList;
import java.util.Queue;

public abstract class IDManager
{
	private final int max;
	private final Queue<Integer> unusedIDs = new LinkedList<>();
	
	private int latest;
	
	public IDManager(int initial, int max)
	{
		this.max = max;
		this.latest = initial;
	}
	
	protected Integer getNewIDInternal()
	{
		Integer index = unusedIDs.poll();
		if(index == null)
		{
			if(latest >= max)
			{
				return null;
			}
			index = latest++;
		}
		return index;
	}
	
	public abstract Integer getNewID();
	
	public void freeID(int id)
	{
		unusedIDs.add(id);
	}
	
	public int getFreeIDs()
	{
		return unusedIDs.size();
	}
}

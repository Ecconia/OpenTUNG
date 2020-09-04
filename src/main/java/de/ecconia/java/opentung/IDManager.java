package de.ecconia.java.opentung;

import java.util.LinkedList;
import java.util.Queue;

public class IDManager
{
	private final int max;
	private final Queue<Integer> unusedIDs = new LinkedList<>();
	
	private int latest;
	
	public IDManager(int initial, int max)
	{
		this.max = max;
		this.latest = initial;
	}
	
	public Integer getNewID()
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
	
	public void freeID(int id)
	{
		unusedIDs.add(id);
	}
}

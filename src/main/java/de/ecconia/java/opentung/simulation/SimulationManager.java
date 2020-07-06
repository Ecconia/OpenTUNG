package de.ecconia.java.opentung.simulation;

import de.ecconia.java.opentung.Settings;
import de.ecconia.java.opentung.components.fragments.Color;

import java.util.ArrayList;
import java.util.List;

public class SimulationManager extends Thread
{
	private List<Updateable> updateNextTickThreadSafe = new ArrayList<>();
	private List<Updateable> updateThisTickThreadSafe = new ArrayList<>();
	private List<Updateable> updateNextTick = new ArrayList<>();
	private List<Updateable> updateThisTick = new ArrayList<>();
	
	private List<Cluster> clustersToUpdate = new ArrayList<>();
	private List<Cluster> sourceClusters = new ArrayList<>();
	
	private int[] connectorMeshStates;
	private int[] colorMeshStates;
	
	private int tps;
	private int ups;
	private int upsCounter;
	
	public SimulationManager()
	{
		super("Simulation-Thread");
	}
	
	public void setConnectorMeshStates(int[] connectorMeshStates)
	{
		this.connectorMeshStates = connectorMeshStates;
	}
	
	public void setColorMeshStates(int[] colorMeshStates)
	{
		this.colorMeshStates = colorMeshStates;
	}
	
	@Override
	public void run()
	{
		long past = System.currentTimeMillis();
		int finishedTicks = 0;
		
		long before = 0;
		long after;
		long targetSleep;
		
		while(!Thread.currentThread().isInterrupted())
		{
			doTick();
			
			finishedTicks++;
			long now = System.currentTimeMillis();
			if(now - past > 1000)
			{
				past = now;
				tps = finishedTicks;
				finishedTicks = 0;
				ups = upsCounter;
				upsCounter = 0;
			}
			
			if(Settings.targetTPS > 0)
			{
				targetSleep = 1000000000L / Settings.targetTPS;
				if(targetSleep > 1000000)
				{
					try
					{
						Thread.sleep(targetSleep / 1000000);
					}
					catch(InterruptedException e)
					{
						break;
					}
				}
				else
				{
					after = System.nanoTime();
					long delta = after - before;
					long targetTime = after + targetSleep - delta;
					while(System.nanoTime() < targetTime) ;
					before = System.nanoTime();
				}
			}
		}
		
		System.out.println("Simulation thread has turned off.");
	}
	
	public void updateNextTickThreadSafe(Updateable updateable)
	{
		synchronized(this)
		{
			updateNextTickThreadSafe.add(updateable);
		}
	}
	
	public void updateNextTick(Updateable updateable)
	{
		updateNextTick.add(updateable);
	}
	
	public void mightHaveChanged(Cluster cluster)
	{
		clustersToUpdate.add(cluster);
	}
	
	private void doTick()
	{
		{
			List<Updateable> tmp = updateThisTick;
			updateThisTick = updateNextTick;
			updateNextTick = tmp;
			updateNextTick.clear();
		}
		if(!updateNextTickThreadSafe.isEmpty())
		{
			synchronized (this) {
				List<Updateable> tmp = updateThisTickThreadSafe;
				updateThisTickThreadSafe = updateNextTickThreadSafe;
				updateNextTickThreadSafe = tmp;
				updateNextTickThreadSafe.clear();
			}
			updateThisTick.addAll(updateThisTickThreadSafe);
		}
		
		upsCounter += updateThisTick.size();
		
		//Actual tick processing:
		
		for(Updateable updateable : updateThisTick)
		{
			updateable.update(this);
		}

		{
			List<Cluster> tmp = sourceClusters;
			sourceClusters.clear();
			sourceClusters = clustersToUpdate;
			clustersToUpdate = tmp;
		}

		for(Cluster cluster : sourceClusters)
		{
			cluster.update(this);
		}
		
		for(Cluster cluster : clustersToUpdate)
		{
			cluster.update(this);
		}
		if(!clustersToUpdate.isEmpty())
		{
			clustersToUpdate.clear();
		}
	}
	
	public void changeState(int id, boolean active)
	{
		int index = id / 32;
		int offset = id % 32;
		int mask = (1 << offset);
		
		int value = connectorMeshStates[index];
		value = value & ~mask;
		if(active)
		{
			value |= mask;
		}
		connectorMeshStates[index] = value;
	}
	
	public int getTPS()
	{
		return tps;
	}
	
	public float getLoad()
	{
		return (float) Math.round(((float) ups / (float) tps) * 100f) / 100f;
	}
	
	public void setColor(int colorID, Color color)
	{
		colorMeshStates[colorID] = color.getR() << 24 | color.getG() << 16 | color.getB() << 8 | 255;
	}
}

package de.ecconia.java.opentung.simulation;

import de.ecconia.java.opentung.settings.Settings;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SimulationManager extends Thread
{
	//TODO: Replace with custom lists:
	private List<UpdateJob> updateJobNextTickThreadSafe = new ArrayList<>();
	private List<UpdateJob> updateJobThisTickThreadSafe = new ArrayList<>();
	private List<Updateable> updateNextTickThreadSafe = new ArrayList<>();
	private List<Updateable> updateThisTickThreadSafe = new ArrayList<>();
	private List<Updateable> updateNextTick = new ArrayList<>();
	private List<Updateable> updateThisTick = new ArrayList<>();
	private List<Cluster> updateClusterNextStage = new ArrayList<>();
	private List<Cluster> updateClusterThisStage = new ArrayList<>();
	
	private int tps;
	private int ups;
	private int upsCounter;
	
	private boolean paused;
	private AtomicInteger pauseArrived;
	
	private long tickLoopIndex;
	
	public SimulationManager()
	{
		super("Simulation-Thread");
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
			if(paused)
			{
				if(pauseArrived != null)
				{
					pauseArrived.incrementAndGet();
					pauseArrived = null; //Remove reference to not trigger it again.
				}
				tps = 0;
				ups = 0;
				try
				{
					Thread.sleep(500);
				}
				catch(InterruptedException e)
				{
					break; //Just break the while loop. May happen on exit while saving.
				}
			}
			else
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
						while(System.nanoTime() < targetTime)
						{
						}
						before = System.nanoTime();
					}
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
	
	public void updateJobNextTickThreadSafe(UpdateJob updateJob)
	{
		synchronized(this)
		{
			updateJobNextTickThreadSafe.add(updateJob);
		}
	}
	
	public void updateNextTick(Updateable updateable)
	{
		updateNextTick.add(updateable);
	}
	
	public void updateNextStage(Cluster cluster)
	{
		updateClusterNextStage.add(cluster);
	}
	
	public void updateThisStage(Cluster cluster)
	{
		updateClusterThisStage.add(cluster);
	}
	
	private void doTick()
	{
		if(!updateJobNextTickThreadSafe.isEmpty())
		{
			synchronized(this)
			{
				List<UpdateJob> tmp = updateJobThisTickThreadSafe;
				updateJobThisTickThreadSafe = updateJobNextTickThreadSafe;
				updateJobNextTickThreadSafe = tmp;
				//TBI: The clearing could be done in the synchronized section of the input/graphic thread.
				//TBI: Alternatively overwrite the class and let clear only reset the pointer.
				updateJobNextTickThreadSafe.clear();
			}
			for(UpdateJob updateJob : updateJobThisTickThreadSafe)
			{
				updateJob.update(this);
			}
		}
		
		tickLoopIndex++;
		
		{
			List<Updateable> tmp = updateThisTick;
			updateThisTick = updateNextTick;
			updateNextTick = tmp;
			updateNextTick.clear();
		}
		
		if(!updateNextTickThreadSafe.isEmpty())
		{
			synchronized(this)
			{
				List<Updateable> tmp = updateThisTickThreadSafe;
				updateThisTickThreadSafe = updateNextTickThreadSafe;
				updateNextTickThreadSafe = tmp;
				//TBI: The clearing could be done in the synchronized section of the input/graphic thread.
				//TBI: Alternatively overwrite the class and let clear only reset the pointer.
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
			List<Cluster> tmp = updateClusterThisStage;
			updateClusterThisStage = updateClusterNextStage;
			updateClusterNextStage = tmp;
		}
		
		//Source clusters:
		for(Cluster cluster : updateClusterThisStage)
		{
			cluster.update(this);
		}
		
		//Inheriting clusters:
		for(Cluster cluster : updateClusterNextStage)
		{
			cluster.update(this);
		}
		
		if(!updateClusterThisStage.isEmpty())
		{
			updateClusterThisStage.clear();
		}
		if(!updateClusterNextStage.isEmpty())
		{
			updateClusterNextStage.clear();
		}
	}
	
	public long getTickLoopIndex()
	{
		return tickLoopIndex;
	}
	
	public int getTPS()
	{
		return tps;
	}
	
	public float getLoad()
	{
		return (float) Math.round(((float) ups / (float) tps) * 100f) / 100f;
	}
	
	public void pauseSimulation(AtomicInteger pauseArrived)
	{
		if(!this.isAlive())
		{
			System.out.println("Simulation thread crashed, saving anyway.");
			pauseArrived.incrementAndGet();
			return;
		}
		this.pauseArrived = pauseArrived;
		paused = true;
	}
	
	public void resumeSimulation()
	{
		paused = false;
	}
	
	public interface UpdateJob
	{
		void update(SimulationManager simulation);
	}
}

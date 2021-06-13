package de.ecconia.java.opentung.simulation;

import de.ecconia.java.opentung.settings.Settings;
import java.util.ArrayList;
import java.util.List;

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
	private boolean locked;
	private int currentJobQueueSize;
	private boolean isSimulationHalted;
	
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
			if(paused || locked)
			{
				tps = 0;
				ups = 0;
				try
				{
					Thread.sleep(50);
				}
				catch(InterruptedException e)
				{
					break; //Just break the while loop. May happen on exit while saving.
				}
				isSimulationHalted = true;
				processJobs(); //For now just process the jobs every now and then.
			}
			else
			{
				isSimulationHalted = false;
				processJobs();
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
	
	//Used by inputs like Buttons/Switches, when interacted with.
	//Used to prime new components.
	public void updateNextTickThreadSafe(Updateable updateable)
	{
		synchronized(this)
		{
			updateNextTickThreadSafe.add(updateable);
		}
	}
	
	//Used by a lot, whenever there are edits to the clusters.
	public void updateJobNextTickThreadSafe(UpdateJob updateJob)
	{
		synchronized(this)
		{
			updateJobNextTickThreadSafe.add(updateJob);
		}
	}
	
	//Used for priming a new cluster
	//Used to schedule a component again (delayer)
	//Used to add components if a cluster state changed.
	public void updateNextTick(Updateable updateable)
	{
		if(!updateable.isQueuedForUpdate()) //Already queued, no need to queue again. [Primary for Delayers]
		{
			updateable.setQueuedForUpdate(true);
			updateNextTick.add(updateable);
		}
	}
	
	//Called if a components output state changes (if component updated by simulation).
	//Called by inheriting-clusters, if the user changed the cluster network.
	public void updateNextStage(Cluster cluster)
	{
		updateClusterNextStage.add(cluster);
	}
	
	//Not called at all.
//	public void updateThisStage(Cluster cluster)
//	{
//		updateClusterThisStage.add(cluster);
//	}
	
	private void processJobs()
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
			currentJobQueueSize = updateJobThisTickThreadSafe.size();
			for(UpdateJob updateJob : updateJobThisTickThreadSafe)
			{
				updateJob.update(this);
			}
		}
	}
	
	/**
	 * Although this is a public access method, it must only be called from the simulation thread.
	 */
	public void doTick()
	{
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
			updateable.setQueuedForUpdate(false);
			updateable.update(this);
		}
		
		{
			//TODO: Swap very likely obsolete and should be removed - improves semantic.
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
	
	//Getters:
	
	public int getTPS()
	{
		return tps;
	}
	
	public float getLoad()
	{
		return (float) Math.round(((float) ups / (float) tps) * 100f) / 100f;
	}
	
	public int getCurrentJobQueueSize()
	{
		return currentJobQueueSize;
	}
	
	public boolean isSimulationHalted()
	{
		return isSimulationHalted;
	}
	
	//Locking/Pausing:
	
	public void lockSimulation()
	{
		this.locked = true;
	}
	
	public void unlockSimulation()
	{
		this.locked = false;
	}
	
	public void togglePaused()
	{
		this.paused = !this.paused;
		if(this.paused)
		{
			System.out.println("[Simulation] Paused.");
		}
		else
		{
			System.out.println("[Simulation] Resumed." + (this.locked ? " But currently locked by saving." : ""));
		}
	}
	
	//Classes:
	
	public interface UpdateJob
	{
		void update(SimulationManager simulation);
	}
}

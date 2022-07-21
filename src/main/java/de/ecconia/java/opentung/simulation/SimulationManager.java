package de.ecconia.java.opentung.simulation;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import de.ecconia.java.opentung.settings.Settings;

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
	
	private boolean paused; //Set to true, if the user requested the simulation to be paused.
	private boolean locked; //Set to true, if the system forced the simulation to be paused.
	private int currentJobQueueSize;
	private boolean isSimulationHalted;
	
	public SimulationManager()
	{
		super("Simulation-Thread");
	}
	
	@Override
	public void run()
	{
		long slotsPerSecond = 20;
		long durationPerSlot = 1000 / slotsPerSecond;
		initSlot(); //Initialize the system.
		
		//The end of the current slot and start of the new slot, used by the slot processing to detect overtime:
		long nextSlotStartTime = System.currentTimeMillis() + durationPerSlot;
		//When a full slot was skipped, tell the slot processor, so that he can catch up:
		long skippedSlots = 0;
		//Internal counter, to detect if a second has passed (20 slots over).
		int slotCounter = 1;
		boolean secondOver = false; //And the flag storing that state.
		while(!Thread.currentThread().isInterrupted())
		{
			processSlot(secondOver, (int) skippedSlots, nextSlotStartTime);
			//Reset for next cycle/slot:
			secondOver = false;
			skippedSlots = 0;
			
			//Calculate the amount of time, that is left in this slot:
			long timeRemaining = nextSlotStartTime - System.currentTimeMillis();
			if(timeRemaining != 0) //No time remaining, so just continue!
			{
				if(timeRemaining > 1) //We will subtract 1, so lets not wait for 0ms.
				{
					//For stability just wait one less ms. It can just wait longer next time.
					// Basically a very stupid form of forced rounding down.
					sleepWrapper(timeRemaining - 1);
				}
				else if(timeRemaining < 0) //Negative means, we ran out of time in this slot.
				{
					//Calculate how many ticks are skipped, and add the amount to the continuing values:
					skippedSlots = timeRemaining / -durationPerSlot;
					nextSlotStartTime += skippedSlots * durationPerSlot; //Add the full-slot skipped time to the next deadline.
					if(skippedSlots > slotsPerSecond)
					{
						secondOver = true; //We skipped more than '20' slots, so a second must have passed.
					}
					slotCounter += skippedSlots % slotsPerSecond; //Lets not add more than 20. So that an if is sufficient later on.
				}
			}
			
			//Prepare for the next cycle/slot:
			nextSlotStartTime += durationPerSlot; //Time when the next slot will end.
			if(++slotCounter > slotsPerSecond) //Can at most increase by 20, so no worry here.
			{
				secondOver = true; //Counter overflowed, thus second must have passed.
				slotCounter -= slotsPerSecond;
			}
		}
		
		System.out.println("Simulation thread has turned off.");
	}
	
	private void sleepWrapper(long milliseconds)
	{
		try
		{
			Thread.sleep(milliseconds);
		}
		catch(InterruptedException e)
		{
			//The exception removes the interrupt flag, lets help ourself and just set it again.
			interrupt();
		}
	}
	
	//Tick scheduling fields:
	
	private boolean boostMode;
	private int tickCounter;
	private boolean wasPause;
	
	private double ticksPerSlot;
	private double toBeProcessedTicks;
	
	private void initSlot()
	{
		updateValues();
	}
	
	private void updateValues()
	{
		//Read settings.txt and apply values:
		boostMode = Settings.targetTPS < 0;
		ticksPerSlot = Settings.targetTPS / 20D;
		//If there are over 1000 leftover ticks from the last slot, discard them and warn.
		if(toBeProcessedTicks > 1000)
		{
			int skippedTicks = (int) Math.ceil(toBeProcessedTicks);
			if(Settings.warnOnTPSSkipping)
			{
				System.out.println("[Simulation] Skipping " + skippedTicks + " ticks.");
			}
			toBeProcessedTicks = 0;
		}
	}
	
	private boolean doNotShowPopupAgain = false;
	
	private void processSlot(boolean secondPassed, int skippedSlots, long timeNextSlot)
	{
		processJobs(); //Jobs have to be done frequently regardless circumstances.
		
		if(secondPassed) //Do this only every so often (per second), less overhead.
		{
			//Set new boost times:
			updateValues();
			
			//Update statistics:
			tps = tickCounter;
			tickCounter = 0;
			ups = upsCounter;
			upsCounter = 0;
		}
		
		if(locked || paused)
		{
			//Basically fully reset every variable, that could cause the simulation to run.
			isSimulationHalted = true; //This one is only used for the jobs, so that saving can stop simulation properly.
			ticksPerSlot = 0; //Do not increase the amount of ticks to be processed again.
			boostMode = false;
			wasPause = true; //To restore the values, we need to detect when we resume, this variable is used for that.
			toBeProcessedTicks = 0; //Pausing means there will not be ticks to catch up on resume.
		}
		else if(wasPause)
		{
			//Restore values:
			updateValues();
			isSimulationHalted = false;
			wasPause = false;
		}
		
		if(boostMode)
		{
			try
			{
				//As many ticks as possible, until slot runs out of time:
				while(System.currentTimeMillis() < timeNextSlot)
				{
					doTick();
				}
			}
			catch(Exception e)
			{
				System.out.println("Exception while executing ticks. Skipping some. Please restart and report issue.");
				e.printStackTrace(System.out);
				if(!doNotShowPopupAgain)
				{
					doNotShowPopupAgain = true;
					JOptionPane.showMessageDialog(null, "Exceptions while simulating tick. Please report stacktrace. And restart. Message will not be shown again.");
				}
			}
		}
		else
		{
			//One slot and all the skipped slots need to be counted.
			// We are counting in floating point, so that we never loose precision and are as close to correct as possible.
			toBeProcessedTicks += ticksPerSlot * (skippedSlots + 1);
			int beforeTicks = tickCounter; //Used to count ticks processed.
			int ticksToProcess = (int) Math.ceil(toBeProcessedTicks); //Convert the floating point amount back to integer. Round up, to do more than required, so that later on less work.
			try
			{
				for(int i = 0; i < ticksToProcess; i++)
				{
					doTick();
					
					//Timeout, slot has no more time left:
					if(System.currentTimeMillis() >= timeNextSlot)
					{
						break;
					}
				}
			}
			catch(Exception e)
			{
				System.out.println("Exception while executing ticks. Skipping some. Please restart and report issue.");
				e.printStackTrace(System.out);
				if(!doNotShowPopupAgain)
				{
					doNotShowPopupAgain = true;
					JOptionPane.showMessageDialog(null, "Exceptions while simulating tick. Please report stacktrace. And restart. Message will not be shown again.");
				}
			}
			int processedTicks = tickCounter - beforeTicks; //Calculate processed ticks.
			toBeProcessedTicks -= processedTicks;
		}
	}
	
	//Non-Schedule normal Simulation code:
	
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
	
	private boolean doNotShowPopupAgainUpdateJob = false;
	
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
				try
				{
					updateJob.update(this);
				}
				catch(Throwable t)
				{
					System.out.println("Failed to run job on simulation thread. Stacktrace:");
					t.printStackTrace(System.out);
					if(!doNotShowPopupAgainUpdateJob)
					{
						doNotShowPopupAgainUpdateJob = true;
						JOptionPane.showMessageDialog(null, "Failed to run job on simulation thread. World is probably corrupted now. Please report stacktrace. And restart. This message is only shown once.");
					}
				}
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
		
		tickCounter++;
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

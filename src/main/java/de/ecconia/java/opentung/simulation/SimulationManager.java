package de.ecconia.java.opentung.simulation;

import java.util.ArrayList;
import java.util.List;

public class SimulationManager extends Thread
{
	private List<Updateable> updateNextTick = new ArrayList<>();
	//These two are for internal usage.
	private List<Cluster> clustersToUpdate = new ArrayList<>();
	private List<Updateable> updateTick = new ArrayList<>();
	
	//TODO: Remove this shame of programming (after debugging stage):
	public static SimulationManager instance;
	
	private int[] connectorMeshStates;
	
	public SimulationManager()
	{
		super("Simulation-Thread");
		instance = this;
	}
	
	public void setConnectorMeshStates(int[] connectorMeshStates)
	{
		this.connectorMeshStates = connectorMeshStates;
	}
	
	@Override
	public void run()
	{
		while(!Thread.currentThread().isInterrupted())
		{
			doTick();
			
			try
			{
				Thread.sleep(1);
			}
			catch(InterruptedException e)
			{
				break;
			}
		}
		
		System.out.println("Simulation thread has turned off.");
	}
	
	public void updateNextTick(Updateable updateable)
	{
		updateNextTick.add(updateable);
	}
	
	public void mightHaveChanged(Cluster cluster)
	{
		clustersToUpdate.add(cluster);
	}
	
	public void doTick()
	{
		updateTick.clear();
		clustersToUpdate.clear();
		
		{
			List<Updateable> swappy = updateTick;
			updateTick = updateNextTick;
			//Thread-safe swapping: Either a component from another thread is in this or the next tick. Who cares - bad timing.
			updateNextTick = swappy;
		}
		
		//Actual tick processing:
		
		for(Updateable updateable : updateTick)
		{
			updateable.update(this);
		}
		
		for(Cluster cluster : clustersToUpdate)
		{
			cluster.update(this);
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
}

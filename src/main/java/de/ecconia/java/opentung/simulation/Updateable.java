package de.ecconia.java.opentung.simulation;

public interface Updateable
{
	void update(SimulationManager simulation);
	
	boolean isQueuedForUpdate();
	
	void setQueuedForUpdate(boolean state);
}

package de.ecconia.java.opentung.simulation;

public interface Powerable
{
	void setPowered(boolean powered);
	
	boolean isPowered();
	
	void forceUpdateOutput();
}

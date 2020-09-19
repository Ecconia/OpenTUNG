package de.ecconia.java.opentung.simulation;

public interface Powerable
{
	void setPowered(int port, boolean powered);
	
	boolean isPowered(int port);
	
	void forceUpdateOutput();
}

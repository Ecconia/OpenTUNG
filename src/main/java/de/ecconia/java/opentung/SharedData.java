package de.ecconia.java.opentung;

public class SharedData
{
	private PlaceableInfo currentPlaceable;
	
	public PlaceableInfo getCurrentPlaceable()
	{
		return currentPlaceable;
	}
	
	public void setCurrentPlaceable(PlaceableInfo currentPlaceable)
	{
		this.currentPlaceable = currentPlaceable;
	}
}

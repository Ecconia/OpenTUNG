package de.ecconia.java.opentung.components.meta;

import de.ecconia.java.opentung.components.fragments.Color;

public interface Colorable
{
	void setColorID(int id, int colorID);
	
	int getColorID(int id);
	
	Color getCurrentColor(int id);
}

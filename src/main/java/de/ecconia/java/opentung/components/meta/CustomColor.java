package de.ecconia.java.opentung.components.meta;

import de.ecconia.java.opentung.components.fragments.Color;

//Should only ever be implemented by Components.
public interface CustomColor
{
	Color getColor();
	
	void setColor(Color color);
}

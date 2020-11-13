package de.ecconia.java.opentung.components.meta;

import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.meshing.ColorMeshBagReference;

public interface Colorable
{
	void setColorMeshBag(int id, ColorMeshBagReference colorMeshBag);
	
	ColorMeshBagReference getColorMeshBag(int id);
	
	ColorMeshBagReference removeColorMeshBag(int id);
	
	Color getCurrentColor(int id);
	
	void updateColors();
}

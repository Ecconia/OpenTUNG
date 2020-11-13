package de.ecconia.java.opentung.meshing;

import de.ecconia.java.opentung.components.fragments.Color;

public class ColorMeshBagReference
{
	private final ColorMeshBag colorMeshBag;
	private final int index;
	
	public ColorMeshBagReference(ColorMeshBag colorMeshBag, int index)
	{
		this.colorMeshBag = colorMeshBag;
		this.index = index;
	}
	
	public void setColor(Color color)
	{
		colorMeshBag.setColor(index, color);
	}
	
	public ColorMeshBag getColorMeshBag()
	{
		return colorMeshBag;
	}
	
	public int getIndex()
	{
		return index;
	}
}

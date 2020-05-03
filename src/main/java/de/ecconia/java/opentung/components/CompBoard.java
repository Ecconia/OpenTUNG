package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.math.Vector3;
import de.ecconia.java.opentung.models.DynamicBoardModel;
import de.ecconia.java.opentung.models.GenericModel;

public class CompBoard extends CompContainer
{
	public static DynamicBoardModel model;
	
	public static void initGL()
	{
		model = new DynamicBoardModel();
	}
	
	private Vector3 color = new Vector3(195f / 255f, 195f / 255f, 195f / 255f);
	private int x, z;
	
	public CompBoard(CompContainer parent, int x, int z)
	{
		super(parent);
		
		this.x = x;
		this.z = z;
	}
	
	public void setColor(Vector3 color)
	{
		this.color = color;
	}
	
	public Vector3 getColor()
	{
		return color;
	}
	
	public int getX()
	{
		return x;
	}
	
	public int getZ()
	{
		return z;
	}
	
	@Override
	public GenericModel getModel()
	{
		return model;
	}
}

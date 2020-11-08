package de.ecconia.java.opentung.meshing;

public enum MeshTypeThing
{
	Raycast(false, false, true, 3 + 3),
	Board(true, true, true, 3 + 3 + 2 + 3),
	Solid(true, false, true, 3 + 3 + 3),
	Conductor(true, false, false, 3 + 3),
	Display(true, false, false, 3 + 3),
	;
	
	private final boolean usesNormals;
	private final boolean usesTextures;
	private final boolean usesColor;
	private final int floatCount;
	
	MeshTypeThing(boolean usesNormals, boolean usesTextures, boolean usesColor, int floatCount)
	{
		this.usesNormals = usesNormals;
		this.usesTextures = usesTextures;
		this.usesColor = usesColor;
		this.floatCount = floatCount;
	}
	
	public boolean usesNormals()
	{
		return usesNormals;
	}
	
	public boolean usesTextures()
	{
		return usesTextures;
	}
	
	public boolean usesColor()
	{
		return usesColor;
	}
	
	public int getFloatCount()
	{
		return floatCount;
	}
}

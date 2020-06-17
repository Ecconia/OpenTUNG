package de.ecconia.java.opentung.libwrap.meshes;

public enum MeshTypeThing
{
	Raycast(false, false, true, true),
	Board(true, true, true, false),
	Solid(true, false, true, false),
	;
	
	private final boolean usesNormals;
	private final boolean usesTextures;
	private final boolean usesColor;
	private final boolean colorISID;
	
	MeshTypeThing(boolean usesNormals, boolean usesTextures, boolean usesColor, boolean colorISID)
	{
		this.usesNormals = usesNormals;
		this.usesTextures = usesTextures;
		this.usesColor = usesColor;
		this.colorISID = colorISID;
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
	
	public boolean colorISID()
	{
		return colorISID;
	}
}

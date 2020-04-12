package de.ecconia.java.opentung.tungboard.tungobjects;

public enum TungColorEnum
{
	Off,
	Red,
	Yellow,
	Blue,
	Green,
	Orange,
	Purple,
	White,
	Cyan,
	;
	private static TungColorEnum[] colors;
	
	static
	{
		colors = new TungColorEnum[values().length];
		for(int i = 0; i < colors.length; i++)
		{
			colors[i] = values()[i];
		}
	}
	
	public static TungColorEnum lookup(int entry)
	{
		if(entry >= colors.length || entry < 0)
		{
			throw new RuntimeException("Unexisting display color: " + entry);
		}
		
		return colors[entry];
	}
}

package de.ecconia.java.opentung.tungboard.tungobjects.meta;

public enum TungColorEnum
{
	Off(32, 32, 32),
	Red(186, 0, 0),
	Yellow(255, 227, 2),
	Blue(0, 50, 200),
	Green(20, 150, 0),
	Orange(255, 95, 0),
	Purple(142, 18, 255),
	White(200, 200, 210),
	Cyan(0, 219, 206),
	;
	
	private int index;
	
	private final int r;
	private final int g;
	private final int b;
	
	TungColorEnum(int red, int green, int blue)
	{
		this.r = red;
		this.g = green;
		this.b = blue;
	}
	
	public void setIndex(int index)
	{
		this.index = index;
	}
	
	public int getIndex()
	{
		return index;
	}
	
	public int getR()
	{
		return r;
	}
	
	public int getG()
	{
		return g;
	}
	
	public int getB()
	{
		return b;
	}
	
	private static final TungColorEnum[] colors;
	
	static
	{
		colors = new TungColorEnum[values().length];
		for(int i = 0; i < colors.length; i++)
		{
			colors[i] = values()[i];
			colors[i].setIndex(i);
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

package de.ecconia.java.opentung.savefile;

import de.ecconia.java.opentung.components.meta.PlaceableInfo;
import de.ecconia.java.opentung.util.io.ByteReader;

public class DictionaryEntry
{
	private final int id;
	private final String tag;
	private final String version;
	private final int pegs;
	private final int blots;
	private final boolean customData;
	
	private int componentCount;
	
	public DictionaryEntry(int id, ByteReader reader)
	{
		this.id = id;
		String tag = reader.readCompactString();
		if(tag.startsWith("TUNG."))
		{
			tag = "TUNG-" + tag.substring(5);
		}
		this.tag = tag;
		this.version = reader.readCompactString();
		this.pegs = reader.readVariableInt();
		this.blots = reader.readVariableInt();
		this.customData = reader.readBoolean();
		this.componentCount = reader.readVariableInt();
	}
	
	public DictionaryEntry(PlaceableInfo info, int id)
	{
		this.id = id;
		this.pegs = info.getModel().getPegModels().size();
		this.blots = info.getModel().getBlotModels().size();
		this.customData = info.hasCustomData();
		this.version = info.getVersion();
		
		String tag = info.getName();
		if(tag.startsWith("TUNG-"))
		{
			tag = "TUNG." + tag.substring(5);
		}
		this.tag = tag;
	}
	
	public void incrementCounter()
	{
		componentCount++;
	}
	
	public int getId()
	{
		return id;
	}
	
	public String getTag()
	{
		return tag;
	}
	
	public String getVersion()
	{
		return version;
	}
	
	public int getComponentCount()
	{
		return componentCount;
	}
	
	public int getPegs()
	{
		return pegs;
	}
	
	public int getBlots()
	{
		return blots;
	}
	
	public boolean hasCustomData()
	{
		return customData;
	}
}

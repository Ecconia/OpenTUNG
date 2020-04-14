package de.ecconia.java.opentung.tungboard.netremoting.elements;

import de.ecconia.java.opentung.tungboard.netremoting.NRParseBundle;

public class NRHeader extends NRObject
{
	private final int rootID;
	private final int headerID;
	private final int majorVersion;
	private final int minorVersion;
	
	public NRHeader(NRParseBundle b)
	{
		rootID = b.sInt();
		headerID = b.sInt();
		majorVersion = b.sInt();
		minorVersion = b.sInt();
		
//		System.out.println("Header: " + rootID + " " + headerID + " " + majorVersion + "." + minorVersion);
	}
	
	public int getRootID()
	{
		return rootID;
	}
	
	public int getHeaderID()
	{
		return headerID;
	}
	
	public int getMajorVersion()
	{
		return majorVersion;
	}
	
	public int getMinorVersion()
	{
		return minorVersion;
	}
}

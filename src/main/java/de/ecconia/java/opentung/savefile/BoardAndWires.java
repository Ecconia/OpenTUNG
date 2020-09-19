package de.ecconia.java.opentung.savefile;

import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.conductor.CompWireRaw;

public class BoardAndWires
{
	private final CompBoard board;
	private final CompWireRaw[] wires;
	
	public BoardAndWires(CompBoard board, CompWireRaw[] wires)
	{
		this.board = board;
		this.wires = wires;
	}
	
	public CompBoard getBoard()
	{
		return board;
	}
	
	public CompWireRaw[] getWires()
	{
		return wires;
	}
}

package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.PlaceableInfo;
import de.ecconia.java.opentung.components.conductor.Peg;
import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.CubeOpen;
import de.ecconia.java.opentung.components.fragments.Direction;
import de.ecconia.java.opentung.components.meta.Colorable;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.CustomData;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.math.Vector3;
import de.ecconia.java.opentung.simulation.SimulationManager;
import de.ecconia.java.opentung.simulation.Updateable;

public class CompDisplay extends Component implements Updateable, Colorable, CustomData
{
	public static final ModelHolder modelHolder = new ModelHolder();
	public static final PlaceableInfo info = new PlaceableInfo(modelHolder, "TUNG-Display", "0.2.6", CompDisplay.class, CompDisplay::new);
	
	static
	{
		modelHolder.setPlacementOffset(new Vector3(0.0, 0.075, 0.0));
		modelHolder.addColorable(new CubeFull(new Vector3(0.0, 0.48 + 0.15, 0.0), new Vector3(0.3, 0.3, 0.3), Color.displayOff));
		modelHolder.addPeg(new CubeOpen(new Vector3(0.0, 0.24, 0.0), new Vector3(0.1, 0.48, 0.1), Direction.YPos, Color.circuitOFF));
	}
	
	@Override
	public ModelHolder getModelHolder()
	{
		return modelHolder;
	}
	
	@Override
	public PlaceableInfo getInfo()
	{
		return info;
	}
	
	//### Non-Static ###
	
	private final Peg input;
	
	private Color colorRaw = Color.displayYellow;
	
	public CompDisplay(CompContainer parent)
	{
		super(parent);
		input = pegs.get(0);
	}
	
	public void setColorRaw(Color color)
	{
		this.colorRaw = color;
	}
	
	public Color getColorRaw()
	{
		return colorRaw;
	}
	
	@Override
	public void update(SimulationManager simulation)
	{
		simulation.setColor(colorID, getCurrentColor(0));
	}
	
	private int colorID;
	
	@Override
	public void setColorID(int id, int colorID)
	{
		this.colorID = colorID;
	}
	
	@Override
	public int getColorID(int id)
	{
		return colorID;
	}
	
	@Override
	public Color getCurrentColor(int id)
	{
		boolean on = input.getCluster().isActive();
		return on ? colorRaw : Color.displayOff;
	}
	
	//### Save/Load ###
	
	@Override
	public byte[] getCustomData()
	{
		byte[] bytes = new byte[3];
		bytes[0] = (byte) colorRaw.getR();
		bytes[1] = (byte) colorRaw.getG();
		bytes[2] = (byte) colorRaw.getB();
		return bytes;
	}
	
	@Override
	public void setCustomData(byte[] data)
	{
		colorRaw = new Color((int) data[0] & 255, (int) data[1] & 255, (int) data[2] & 255);
	}
}

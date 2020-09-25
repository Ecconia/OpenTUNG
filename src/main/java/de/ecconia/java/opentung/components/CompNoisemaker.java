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
import de.ecconia.java.opentung.util.io.ByteLevelHelper;
import de.ecconia.java.opentung.util.io.ByteReader;

public class CompNoisemaker extends Component implements Updateable, Colorable, CustomData
{
	public static final ModelHolder modelHolder = new ModelHolder();
	public static final PlaceableInfo info = new PlaceableInfo(modelHolder, "TUNG-Noisemaker", "0.2.6", CompNoisemaker.class, CompNoisemaker::new);
	
	static
	{
		modelHolder.setPlacementOffset(new Vector3(0.0, 0.075, 0.0));
		modelHolder.addColorable(new CubeFull(new Vector3(0.0, 0.36 + 0.3, 0.0), new Vector3(0.24, 0.6, 0.24), Color.noisemakerOFF));
		modelHolder.addPeg(new CubeOpen(new Vector3(0.0, 0.18, 0.0), new Vector3(0.1, 0.36, 0.1), Direction.YPos, Color.circuitOFF));
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
	
	public CompNoisemaker(CompContainer parent)
	{
		super(parent);
		input = pegs.get(0);
	}
	
	private float frequency;
	
	public void setFrequency(float frequency)
	{
		this.frequency = frequency;
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
		return on ? Color.noisemakerON : Color.noisemakerOFF;
	}
	
	//### Save/Load ###
	
	@Override
	public byte[] getCustomData()
	{
		byte[] bytes = new byte[4];
		ByteLevelHelper.writeFloat(frequency, bytes, 0);
		return bytes;
	}
	
	@Override
	public void setCustomData(byte[] data)
	{
		ByteReader reader = new ByteReader(data);
		frequency = reader.readFloatLE();
	}
}

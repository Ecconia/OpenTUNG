package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.conductor.Peg;
import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.CubeOpen;
import de.ecconia.java.opentung.components.fragments.Direction;
import de.ecconia.java.opentung.components.meta.Colorable;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.CustomData;
import de.ecconia.java.opentung.components.meta.LogicComponent;
import de.ecconia.java.opentung.components.meta.ModelBuilder;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.components.meta.PlaceableInfo;
import de.ecconia.java.opentung.components.meta.PlacementSettingBoardSide;
import de.ecconia.java.opentung.components.meta.PlacementSettingBoardSquare;
import de.ecconia.java.opentung.meshing.ColorMeshBagReference;
import de.ecconia.java.opentung.simulation.SimulationManager;
import de.ecconia.java.opentung.util.io.ByteLevelHelper;
import de.ecconia.java.opentung.util.io.ByteReader;
import de.ecconia.java.opentung.util.math.Vector3;

public class CompNoisemaker extends LogicComponent implements Colorable, CustomData
{
	public static final ModelHolder modelHolder = new ModelBuilder()
			.setPlacementOffset(new Vector3(0.0, 0.075, 0.0))
			.addColorable(new CubeFull(new Vector3(0.0, 0.36 + 0.3, 0.0), new Vector3(0.24, 0.6, 0.24), Color.noisemakerOFF))
			.addPeg(new CubeOpen(new Vector3(0.0, 0.18, 0.0), new Vector3(0.1, 0.36, 0.1), Direction.YPos, Color.circuitOFF))
			.setMountPlaceable(true)
			.setBoardSidePlacementOption(PlacementSettingBoardSide.All)
			.setBoardPlacementOption(PlacementSettingBoardSquare.All)
			.build();
	public static final PlaceableInfo info = new PlaceableInfo(modelHolder, "TUNG-Noisemaker", "0.2.6", CompNoisemaker.class, CompNoisemaker::new);
	
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
		if(colorMeshBag != null)
		{
			colorMeshBag.setColor(getCurrentColor(0));
		}
	}
	
	private ColorMeshBagReference colorMeshBag;
	
	@Override
	public void setColorMeshBag(int id, ColorMeshBagReference meshBag)
	{
		this.colorMeshBag = meshBag;
	}
	
	@Override
	public ColorMeshBagReference getColorMeshBag(int id)
	{
		return colorMeshBag;
	}
	
	@Override
	public ColorMeshBagReference removeColorMeshBag(int id)
	{
		ColorMeshBagReference ret = colorMeshBag;
		colorMeshBag = null;
		return ret;
	}
	
	@Override
	public void updateColors()
	{
		ColorMeshBagReference colorMeshBag = this.colorMeshBag;
		if(colorMeshBag != null)
		{
			colorMeshBag.setColor(getCurrentColor(0));
		}
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
	
	@Override
	public Component copy()
	{
		CompNoisemaker copy = (CompNoisemaker) super.copy();
		copy.setFrequency(frequency);
		return copy;
	}
}

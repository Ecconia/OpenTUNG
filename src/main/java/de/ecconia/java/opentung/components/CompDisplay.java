package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.conductor.Peg;
import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.CubeOpen;
import de.ecconia.java.opentung.components.fragments.Direction;
import de.ecconia.java.opentung.components.meta.Colorable;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.CustomColor;
import de.ecconia.java.opentung.components.meta.CustomData;
import de.ecconia.java.opentung.components.meta.LogicComponent;
import de.ecconia.java.opentung.components.meta.ModelBuilder;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.components.meta.PlaceableInfo;
import de.ecconia.java.opentung.components.meta.PlacementSettingBoardSide;
import de.ecconia.java.opentung.components.meta.PlacementSettingBoardSquare;
import de.ecconia.java.opentung.meshing.ColorMeshBagReference;
import de.ecconia.java.opentung.simulation.SimulationManager;
import de.ecconia.java.opentung.util.math.Vector3;

public class CompDisplay extends LogicComponent implements Colorable, CustomData, CustomColor
{
	public static final ModelHolder modelHolder = new ModelBuilder()
			.setPlacementOffset(new Vector3(0.0, 0.075, 0.0))
			.addColorable(new CubeFull(new Vector3(0.0, 0.48 + 0.15, 0.0), new Vector3(0.3, 0.3, 0.3), Color.displayOff))
			.addPeg(new CubeOpen(new Vector3(0.0, 0.24, 0.0), new Vector3(0.1, 0.48, 0.1), Direction.YPos, Color.circuitOFF, 0.5f))
			.setMountPlaceable(true)
			.setBoardSidePlacementOption(PlacementSettingBoardSide.All)
			.setBoardPlacementOption(PlacementSettingBoardSquare.All)
			.build();
	public static final PlaceableInfo info = new PlaceableInfo(modelHolder, "TUNG-Display", "0.2.6", CompDisplay.class, CompDisplay::new);
	
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
	
	@Override
	public void setColor(Color color)
	{
		this.colorRaw = color;
	}
	
	@Override
	public Color getColor()
	{
		return colorRaw;
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
	
	@Override
	public Component copy()
	{
		CompDisplay copy = (CompDisplay) super.copy();
		copy.setColor(colorRaw);
		return copy;
	}
}

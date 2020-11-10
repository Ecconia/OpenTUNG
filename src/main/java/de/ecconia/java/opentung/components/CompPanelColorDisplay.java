package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.conductor.Peg;
import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.CubeOpen;
import de.ecconia.java.opentung.components.fragments.Direction;
import de.ecconia.java.opentung.components.meta.Colorable;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.components.meta.PlaceableInfo;
import de.ecconia.java.opentung.meshing.ColorMeshBagReference;
import de.ecconia.java.opentung.simulation.SimulationManager;
import de.ecconia.java.opentung.simulation.Updateable;
import de.ecconia.java.opentung.util.math.Vector3;

public class CompPanelColorDisplay extends Component implements Updateable, Colorable
{
	public static final ModelHolder modelHolder = new ModelHolder();
	public static final PlaceableInfo info = new PlaceableInfo(modelHolder, "TUNG-PanelColorDisplay", "0.2.6", CompPanelColorDisplay.class, CompPanelColorDisplay::new);
	
	static
	{
		modelHolder.setPlacementOffset(new Vector3(0.0, 0.0, 0.0));
		modelHolder.addColorable(new CubeFull(new Vector3(0.0, 0.075 + 0.05, 0.0), new Vector3(0.3, 0.1, 0.3), Color.displayOff));
		//Lets cheat a bit, to prevent z-Buffer-Fighting:
		modelHolder.addSolid(new CubeOpen(new Vector3(0.0, 0.075 - 0.125, 0.0), new Vector3(0.2, 0.1 + 0.15, 0.299), Direction.YPos, Color.material));
		modelHolder.addPeg(new CubeOpen(new Vector3(0.0, -0.075 - 0.1 - 0.06, 0.1), new Vector3(0.1, 0.12, 0.1), Direction.YPos, Color.circuitOFF));
		modelHolder.addPeg(new CubeOpen(new Vector3(0.0, -0.075 - 0.1 - 0.105, 0.0), new Vector3(0.1, 0.21, 0.1), Direction.YPos, Color.circuitOFF));
		modelHolder.addPeg(new CubeOpen(new Vector3(0.0, -0.075 - 0.1 - 0.15, -0.1), new Vector3(0.1, 0.3, 0.1), Direction.YPos, Color.circuitOFF));
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
	
	private final Peg inputSmall;
	private final Peg inputMedium;
	private final Peg inputLong;
	
	public CompPanelColorDisplay(CompContainer parent)
	{
		super(parent);
		inputSmall = pegs.get(0);
		inputMedium = pegs.get(1);
		inputLong = pegs.get(2);
	}
	
	@Override
	public void update(SimulationManager simulation)
	{
		ColorMeshBagReference colorMeshBag = this.colorMeshBag;
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
		int colorIndex = 0;
		if(inputSmall.getCluster().isActive())
		{
			colorIndex |= 1;
		}
		if(inputMedium.getCluster().isActive())
		{
			colorIndex |= 2;
		}
		if(inputLong.getCluster().isActive())
		{
			colorIndex |= 4;
		}
		
		return Color.byColorDisplayIndex(colorIndex);
	}
}

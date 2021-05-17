package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.conductor.Blot;
import de.ecconia.java.opentung.components.conductor.Peg;
import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.CubeOpenRotated;
import de.ecconia.java.opentung.components.fragments.Direction;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.CustomData;
import de.ecconia.java.opentung.components.meta.ModelBuilder;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.components.meta.PlaceableInfo;
import de.ecconia.java.opentung.components.meta.PlacementSettingBoardSide;
import de.ecconia.java.opentung.components.meta.PlacementSettingBoardSquare;
import de.ecconia.java.opentung.simulation.Powerable;
import de.ecconia.java.opentung.simulation.SimulationManager;
import de.ecconia.java.opentung.simulation.Updateable;
import de.ecconia.java.opentung.util.io.ByteLevelHelper;
import de.ecconia.java.opentung.util.io.ByteReader;
import de.ecconia.java.opentung.util.math.Quaternion;
import de.ecconia.java.opentung.util.math.Vector3;

public class CompDelayer extends Component implements Powerable, Updateable, CustomData
{
	public static final ModelHolder modelHolder = new ModelBuilder()
			.setPlacementOffset(new Vector3(0.0, 0.15 + 0.075f, 0.0))
			.addSolid(new CubeFull(new Vector3(0.0, 0.0, 0.0), new Vector3(0.3, 0.3, 0.3), Color.material))
			//TODO: Positions of the rotated components is more or less wrong.
			.addPeg(new CubeOpenRotated(Quaternion.angleAxis(45, Vector3.xp), new Vector3(0.0, 0.285, 0.0), new Vector3(0.09, 0.24, 0.09), Direction.YNeg, Color.circuitOFF))
			.addBlot(new CubeOpenRotated(Quaternion.angleAxis(45, Vector3.xn), new Vector3(0.0, 0.195, 0.0), new Vector3(0.15, 0.12, 0.15), Direction.YNeg, Color.circuitOFF))
			.setMountPlaceable(true)
			.setBoardSidePlacementOption(PlacementSettingBoardSide.None)
			.setBoardPlacementOption(PlacementSettingBoardSquare.Middle)
			.build();
	public static final PlaceableInfo info = new PlaceableInfo(modelHolder, "TUNG-Delayer", "0.2.6", CompDelayer.class, CompDelayer::new);
	
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
	private final Blot output;
	
	public CompDelayer(CompContainer parent)
	{
		super(parent);
		input = pegs.get(0);
		output = blots.get(0);
	}
	
	private boolean powered;
	
	@Override
	public void setPowered(int index, boolean powered)
	{
		this.powered = powered;
	}
	
	@Override
	public boolean isPowered(int index)
	{
		return powered;
	}
	
	private int delayCount;
	
	public void setDelayCount(int delayCount)
	{
		this.delayCount = delayCount;
	}
	
	@Override
	public void forceUpdateOutput()
	{
		//Default state is off. Only update on ON.
		if(powered)
		{
			output.forceUpdateON();
		}
	}
	
	private long lastUpdate = 0;
	
	@Override
	public void update(SimulationManager simulation)
	{
		long simulationLoopIndex = simulation.getTickLoopIndex();
		if(simulationLoopIndex == lastUpdate)
		{
			return; //Already handled this tick - if not big problem.
		}
		lastUpdate = simulationLoopIndex;
		
		boolean input = this.input.getCluster().isActive();
		if(delayCount == 9)
		{
			changeOutputState(simulation, true);
		}
		else if(delayCount == 0)
		{
			changeOutputState(simulation, false);
		}
		
		if(input == powered)
		{
			delayCount = input ? 9 : 0;
		}
		else //input != output
		{
			delayCount += input ? 1 : -1;
			simulation.updateNextTick(this);
		}
	}
	
	private void changeOutputState(SimulationManager simulation, boolean state)
	{
		if(powered != state)
		{
			powered = state;
			simulation.updateNextStage(output.getCluster());
		}
	}
	
	//### Save/Load ###
	
	@Override
	public byte[] getCustomData()
	{
		byte[] bytes = new byte[ByteLevelHelper.sizeOfUnsignedInt(delayCount)];
		ByteLevelHelper.writeUnsignedInt(delayCount, bytes, 0);
		return bytes;
	}
	
	@Override
	public void setCustomData(byte[] data)
	{
		ByteReader reader = new ByteReader(data);
		delayCount = reader.readVariableInt();
	}
}

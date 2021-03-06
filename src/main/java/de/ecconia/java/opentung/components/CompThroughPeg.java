package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.conductor.Peg;
import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.CubeOpen;
import de.ecconia.java.opentung.components.fragments.Direction;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.ConnectedComponent;
import de.ecconia.java.opentung.components.meta.ModelBuilder;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.components.meta.PlaceableInfo;
import de.ecconia.java.opentung.components.meta.PlacementSettingBoardSide;
import de.ecconia.java.opentung.components.meta.PlacementSettingBoardSquare;
import de.ecconia.java.opentung.simulation.Cluster;
import de.ecconia.java.opentung.simulation.HiddenWire;
import de.ecconia.java.opentung.simulation.InheritingCluster;
import de.ecconia.java.opentung.simulation.Wire;
import de.ecconia.java.opentung.util.math.Vector3;

public class CompThroughPeg extends ConnectedComponent
{
	public static final ModelHolder modelHolder = new ModelBuilder()
			.setPlacementOffset(new Vector3(0.0, 0.0, 0.0))
			.addSolid(new CubeFull(new Vector3(0.0, 0.0, 0.0), new Vector3(0.2, +0.15 + 0.175, 0.2), Color.material))
			.addPeg(new CubeOpen(new Vector3(0.0, +0.075 + 0.0875 + 0.15, 0.0), new Vector3(0.1, 0.3, 0.1), Direction.YNeg, Color.circuitOFF))
			.addPeg(new CubeOpen(new Vector3(0.0, -0.075 - 0.0875 - 0.15, 0.0), new Vector3(0.1, 0.3, 0.1), Direction.YPos, Color.circuitOFF))
			.setMountPlaceable(false)
			.setBoardSidePlacementOption(PlacementSettingBoardSide.None)
			.setBoardPlacementOption(PlacementSettingBoardSquare.Middle)
			.build();
	public static final PlaceableInfo info = new PlaceableInfo(modelHolder, "TUNG-ThroughPeg", "0.2.6", CompThroughPeg.class, CompThroughPeg::new);
	
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
	
	public CompThroughPeg(CompContainer parent)
	{
		super(parent);
	}
	
	@Override
	public void init()
	{
		Peg first = pegs.get(0);
		Peg second = pegs.get(1);
		
		//Physically connect the two pegs:
		Wire internalWire = new HiddenWire();
		internalWire.setConnectorA(first);
		first.addWire(internalWire);
		internalWire.setConnectorB(second);
		second.addWire(internalWire);
	}
	
	@Override
	public void initClusters()
	{
		Peg first = pegs.get(0);
		Peg second = pegs.get(1);
		Wire internalWire = first.getWires().get(0);
		
		//Create initial cluster for this component, since normal cluster initialization does not support internal wires:
		Cluster cluster = new InheritingCluster();
		cluster.addConnector(first);
		first.setCluster(cluster);
		cluster.addConnector(second);
		second.setCluster(cluster);
		cluster.addWire(internalWire);
	}
}

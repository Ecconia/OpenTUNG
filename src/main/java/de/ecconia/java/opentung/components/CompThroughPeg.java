package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.conductor.Peg;
import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.CubeOpen;
import de.ecconia.java.opentung.components.fragments.Direction;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.math.Vector3;
import de.ecconia.java.opentung.simulation.HiddenWire;
import de.ecconia.java.opentung.simulation.Wire;

public class CompThroughPeg extends Component
{
	private static final ModelHolder modelHolder = new ModelHolder();
	
	static
	{
		modelHolder.setPlacementOffset(new Vector3(0.0, 0.0, 0.0));
		modelHolder.addSolid(new CubeFull(new Vector3(0.0, 0.0, 0.0), new Vector3(0.2, +0.15 +0.175, 0.2), Color.material));
		modelHolder.addPeg(new CubeOpen(new Vector3(0.0, +0.075 +0.0875 +0.15, 0.0), new Vector3(0.1, 0.3, 0.1), Direction.YNeg));
		modelHolder.addPeg(new CubeOpen(new Vector3(0.0, -0.075 -0.0875 -0.15, 0.0), new Vector3(0.1, 0.3, 0.1), Direction.YPos));
	}
	
	@Override
	public ModelHolder getModelHolder()
	{
		return modelHolder;
	}
	
	//### Non-Static ###
	
	public CompThroughPeg(CompContainer parent)
	{
		super(parent);
		Wire internalWire = new HiddenWire();
		Peg peg;
		
		peg = new Peg(this, getModelHolder().getPegModels().get(0));
		pegs.add(peg);
		internalWire.setConnectorA(peg);
		peg.addWire(internalWire);
		
		peg = new Peg(this, getModelHolder().getPegModels().get(1));
		pegs.add(peg);
		internalWire.setConnectorB(peg);
		peg.addWire(internalWire);
	}
}

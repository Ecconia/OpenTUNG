package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.math.Quaternion;
import de.ecconia.java.opentung.math.Vector3;
import de.ecconia.java.opentung.models.BlotterModel;
import de.ecconia.java.opentung.models.DynamicBoardModel;
import de.ecconia.java.opentung.models.DynamicWireModel;
import de.ecconia.java.opentung.models.GenericModel;
import de.ecconia.java.opentung.models.InverterModel;
import de.ecconia.java.opentung.models.LabelModel;
import de.ecconia.java.opentung.models.LabelModelTex;
import de.ecconia.java.opentung.models.PegModel;
import de.ecconia.java.opentung.models.SnappingPegModel;
import de.ecconia.java.opentung.models.ThroughPegModel;

public abstract class CompGeneric
{
	//Main data:
	private Quaternion rotation;
	private Vector3 position;
	
	//Custom data:
	private CompContainer parent;
	
	public CompGeneric(CompContainer parent)
	{
		this.parent = parent;
	}
	
	public void setPosition(Vector3 position)
	{
		this.position = position;
	}
	
	public void setRotation(Quaternion rotation)
	{
		this.rotation = rotation;
	}
	
	public Vector3 getPosition()
	{
		return position;
	}
	
	public Quaternion getRotation()
	{
		return rotation;
	}
	
	public void setParent(CompContainer parent)
	{
		this.parent = parent;
	}
	
	public CompContainer getParent()
	{
		return parent;
	}
	
	public abstract GenericModel getModel();
	
	public static void initModels()
	{
		//TODO: Move improve whatever. This feels hacky, while its not that bad of a "solution/workaround".
		CompPeg.model = new PegModel();
		CompLabel.model = new LabelModel();
		CompLabel.modelTex = new LabelModelTex();
		CompBlotter.model = new BlotterModel();
		CompInverter.model = new InverterModel();
		CompBoard.model = new DynamicBoardModel();
		CompWireRaw.model = new DynamicWireModel();
		CompThroughPeg.model = new ThroughPegModel();
		CompSnappingPeg.model = new SnappingPegModel();
	}
}

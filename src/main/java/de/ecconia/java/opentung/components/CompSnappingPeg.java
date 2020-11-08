package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.util.MinMaxBox;
import de.ecconia.java.opentung.components.meta.PlaceableInfo;
import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.meshing.MeshTypeThing;
import de.ecconia.java.opentung.util.math.Vector3;
import java.util.List;

public class CompSnappingPeg extends Component
{
	public static final ModelHolder modelHolder = new ModelHolder();
	public static final PlaceableInfo info = new PlaceableInfo(modelHolder, "TUNG-SnappingPeg", "0.2.6", CompSnappingPeg.class, CompSnappingPeg::new);
	
	static
	{
		modelHolder.setPlacementOffset(new Vector3(0.0, 0.075, 0.0));
		modelHolder.addPeg(new CubeFull(new Vector3(0.0, 0.15, 0.0), new Vector3(0.09, 0.3, 0.09), Color.snappingPeg));
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
	
	public CompSnappingPeg(CompContainer parent)
	{
		super(parent);
	}
	
	@Override
	public int getWholeMeshEntryVCount(MeshTypeThing type)
	{
		if(type == MeshTypeThing.Solid)
		{
			return 6 * 4 * (3 + 3 + 3);
		}
		else if(type == MeshTypeThing.Raycast)
		{
			return 6 * 4 * (3 + 3);
		}
		else
		{
			return 0;
		}
	}
	
	@Override
	public int getWholeMeshEntryICount(MeshTypeThing type)
	{
		return type == MeshTypeThing.Solid || type == MeshTypeThing.Raycast ? 6 * (3 * 2) : 0;
	}
	
	@Override
	public void insertMeshData(float[] vertices, ModelHolder.IntHolder verticesIndex, int[] indices, ModelHolder.IntHolder indicesIndex, ModelHolder.IntHolder vertexCounter, MeshTypeThing type)
	{
		if(type == MeshTypeThing.Solid || type == MeshTypeThing.Raycast)
		{
			//TODO: This is still ungeneric.
			getModelHolder().getPegModels().get(0).generateMeshEntry(this, vertices, verticesIndex, indices, indicesIndex, vertexCounter, null, getPosition(), getRotation(), getModelHolder().getPlacementOffset(), type);
		}
	}
	
	//Bounding:
	protected MinMaxBox snappingPegBounds;
	
	public void getSnappingPegsAt(Vector3 absolutePoint, List<CompSnappingPeg> collector)
	{
		if(snappingPegBounds.contains(absolutePoint))
		{
			collector.add(this);
		}
	}
	
	public Vector3 getConnectionPoint()
	{
		Vector3 connectionPos = modelHolder.getPlacementOffset().add(new Vector3(0, 0.3 * 0.9, 0)); //Connection point in model
		connectionPos = getRotation().inverse().multiply(connectionPos); //Rotate connection point to absolute grid
		connectionPos = connectionPos.add(getPosition()); //Move connection point to absolute grid
		return connectionPos;
	}
	
	public void createSnappingPegBounds()
	{
		Vector3 connectionPos = getConnectionPoint();
		double bounds = 0.21; //Ensures 0.205
		Vector3 boundsVec = new Vector3(bounds, bounds, bounds);
		Vector3 min = connectionPos.subtract(boundsVec);
		Vector3 max = connectionPos.add(boundsVec);
		
		snappingPegBounds = new MinMaxBox(min, max);
	}
	
	public MinMaxBox getSnappingPegBounds()
	{
		return snappingPegBounds;
	}
	
	//Simulation:
	
	private CompSnappingPeg partner;
	
	public void setPartner(CompSnappingPeg partner)
	{
		this.partner = partner;
	}
	
	public CompSnappingPeg getPartner()
	{
		return partner;
	}
	
	public boolean hasPartner()
	{
		return partner != null;
	}
}

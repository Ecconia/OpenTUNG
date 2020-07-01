package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.MinMaxBox;
import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.libwrap.meshes.MeshTypeThing;
import de.ecconia.java.opentung.math.Vector3;
import java.util.List;

public class CompSnappingPeg extends Component
{
	private static final ModelHolder modelHolder = new ModelHolder();
	
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
			Vector3 color = null;
			if(type.colorISID())
			{
				int id = getRayID();
				int r = id & 0xFF;
				int g = (id & 0xFF00) >> 8;
				int b = (id & 0xFF0000) >> 16;
				color = new Vector3((float) r / 255f, (float) g / 255f, (float) b / 255f);
			}
			//TODO: This is still ungeneric.
			getModelHolder().getPegModels().get(0).generateMeshEntry(this, vertices, verticesIndex, indices, indicesIndex, vertexCounter, color, getPosition(), getRotation(), getModelHolder().getPlacementOffset(), type);
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
	
	public boolean hasParner()
	{
		return partner != null;
	}
}

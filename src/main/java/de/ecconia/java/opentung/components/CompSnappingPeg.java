package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelBuilder;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.components.meta.PlaceableInfo;
import de.ecconia.java.opentung.components.meta.PlacementSettingBoardSide;
import de.ecconia.java.opentung.components.meta.PlacementSettingBoardSquare;
import de.ecconia.java.opentung.meshing.MeshTypeThing;
import de.ecconia.java.opentung.util.math.Vector3;

public class CompSnappingPeg extends Component
{
	public static final ModelHolder modelHolder = new ModelBuilder()
			.setPlacementOffset(new Vector3(0.0, 0.075, -0.06))
			.addColoredPegModel(new CubeFull(new Vector3(0.0, 0.15, 0.0), new Vector3(0.09, 0.3, 0.09), Color.snappingPeg))
			.setMountPlaceable(false)
			.setBoardSidePlacementOption(PlacementSettingBoardSide.None) //Could theoretically be placed in the middle, but not with all 4 rotations, thus prevent for now.
			.setBoardPlacementOption(PlacementSettingBoardSquare.Cross)
			.build();
	public static final PlaceableInfo info = new PlaceableInfo(modelHolder, "TUNG-SnappingPeg", "0.2.6", CompSnappingPeg.class, CompSnappingPeg::new);
	
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
		else
		{
			return 0;
		}
	}
	
	@Override
	public int getWholeMeshEntryICount(MeshTypeThing type)
	{
		return type == MeshTypeThing.Solid ? 6 * (3 * 2) : 0;
	}
	
	@Override
	public void insertMeshData(float[] vertices, ModelHolder.IntHolder verticesIndex, int[] indices, ModelHolder.IntHolder indicesIndex, ModelHolder.IntHolder vertexCounter, MeshTypeThing type)
	{
		if(type == MeshTypeThing.Solid)
		{
			//TODO: This is still ungeneric.
			getModelHolder().getPegModels().get(0).generateMeshEntry(this, vertices, verticesIndex, indices, indicesIndex, vertexCounter, null, getPosition(), getRotation(), getModelHolder().getPlacementOffset(), type);
		}
	}
	
	public Vector3 getConnectionPoint()
	{
		Vector3 connectionPos = modelHolder.getPlacementOffset().add(new Vector3(0, 0.3 * 0.9, 0)); //Connection point in model
		connectionPos = getRotation().inverse().multiply(connectionPos); //Rotate connection point to absolute grid
		connectionPos = connectionPos.add(getPosition()); //Move connection point to absolute grid
		return connectionPos;
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

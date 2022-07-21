package de.ecconia.java.opentung.components.meta;

import java.util.ArrayList;
import java.util.List;

import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.Meshable;
import de.ecconia.java.opentung.components.fragments.TexturedFace;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;
import de.ecconia.java.opentung.libwrap.vaos.LabelVAO;
import de.ecconia.java.opentung.util.math.Vector3;

public class ModelHolder
{
	//Only allow this package to create a ModelHolder - ensures the builder is used.
	protected ModelHolder()
	{
	}
	
	//Offset section. The offset moves the model center relative to the component position.
	private Vector3 offset = Vector3.zero;
	private Vector3 importOffset = null; //This offset is applied when converting from LW (and eventually even from TUNG) components.
	
	public void setPlacementOffset(Vector3 offset)
	{
		this.offset = offset;
	}
	
	public Vector3 getPlacementOffset()
	{
		return offset;
	}
	
	public void setImportOffset(Vector3 importOffset)
	{
		this.importOffset = importOffset;
	}
	
	public Vector3 getImportOffset()
	{
		return importOffset;
	}
	
	//Contains the solid parts of the model. Each has one color which doesn't change.
	private final List<Meshable> solid = new ArrayList<>();
	private int solidVertexCount = 0;
	
	public void addSolid(Meshable meshable)
	{
		solid.add(meshable);
		//TODO: Get rid of the 4, add method to meshable - don't wanna cast this.
		solidVertexCount += ((CubeFull) meshable).getFacesCount() * 4;
	}
	
	public List<Meshable> getSolid()
	{
		return solid;
	}
	
	public int getSolidVerticesAmount()
	{
		return solidVertexCount;
	}
	
	//Contains all visual conductors. Means they turn black/red depending on their state.
	private final List<Meshable> conductors = new ArrayList<>();
	private int conductorVertexCount = 0;
	
	public void addConductor(Meshable meshable)
	{
		conductors.add(meshable);
		conductorVertexCount += ((CubeFull) meshable).getFacesCount() * 4;
	}
	
	public List<Meshable> getConductors()
	{
		return conductors;
	}
	
	public int getConductorVerticesAmount()
	{
		return conductorVertexCount;
	}
	
	//Contains all meshables which can have any non-transparent color.
	private final List<Meshable> colorables = new ArrayList<>();
	private int colorVertexCount = 0;
	
	public void addColorable(Meshable meshable)
	{
		colorables.add(meshable);
		colorVertexCount += ((CubeFull) meshable).getFacesCount() * 4;
	}
	
	public List<Meshable> getColorables()
	{
		return colorables;
	}
	
	public int getColorVerticesAmount()
	{
		return colorVertexCount;
	}
	
	//Peg and Blot models. Both connectors are the connection points for wires.
	private final List<CubeFull> pegModels = new ArrayList<>();
	private final List<CubeFull> blotModels = new ArrayList<>();
	
	public void addPegModel(CubeFull connectorModel)
	{
		pegModels.add(connectorModel);
		conductors.add(connectorModel);
		conductorVertexCount += connectorModel.getFacesCount() * 4;
	}
	
	public void addBlotModel(CubeFull connectorModel)
	{
		blotModels.add(connectorModel);
		conductors.add(connectorModel);
		conductorVertexCount += connectorModel.getFacesCount() * 4;
	}
	
	public void addColoredPegModel(CubeFull connectorModel)
	{
		pegModels.add(connectorModel);
		solid.add(connectorModel);
		solidVertexCount += connectorModel.getFacesCount() * 4;
	}
	
	public void addColoredBlotModel(CubeFull connectorModel)
	{
		blotModels.add(connectorModel);
		solid.add(connectorModel);
		solidVertexCount += connectorModel.getFacesCount() * 4;
	}
	
	public List<CubeFull> getPegModels()
	{
		return pegModels;
	}
	
	public List<CubeFull> getBlotModels()
	{
		return blotModels;
	}
	
	//Contains the textures which will be rendered along with this component.
	private final List<Meshable> textures = new ArrayList<>();
	//Stores a VAO for this texture...
	//TODO: Move to a mesh.
	private GenericVAO textureVAO;
	
	public void addTexture(TexturedFace meshable)
	{
		textures.add(meshable);
	}
	
	//Generates a full object ready to be drawn with input->output peg/blob states as given as parameters.
	public void generateTextureVAO()
	{
		//Shader only has position and texture, TODO: add normals cause it defaults to Y+
		int vCount = 0;
		int iCount = 0;
		for(Meshable m : textures)
		{
			vCount += m.getVCount();
			iCount += m.getICount();
		}
		float[] vertices = new float[vCount];
		short[] indices = new short[iCount];
		IntHolder offsetV = new IntHolder();
		IntHolder offsetI = new IntHolder();
		IntHolder indexOffset = new IntHolder();
		for(Meshable m : textures)
		{
			((TexturedFace) m).generateModel(vertices, offsetV, indices, offsetI, indexOffset, offset);
		}
		textureVAO = new LabelVAO(vertices, indices);
	}
	
	public void drawTextures()
	{
		textureVAO.use();
		textureVAO.draw();
	}
	
	//TODO: Extract this class...
	
	public static class IntHolder
	{
		int value;
		
		public IntHolder()
		{
		}
		
		public IntHolder(int value)
		{
			this.value = value;
		}
		
		public int getAndInc()
		{
			return value++;
		}
		
		public int getAndInc(int i)
		{
			int ret = value;
			value += i;
			return ret;
		}
		
		@Override
		public String toString()
		{
			return "Val: " + value;
		}
	}
	
	//PlacementSettings:
	
	private boolean canBePlacedOnMounts = true;
	private PlacementSettingBoardSquare placementSettingBoardSquare = PlacementSettingBoardSquare.Middle;
	private PlacementSettingBoardSide placementSettingBoardSide = PlacementSettingBoardSide.None;
	
	public boolean canBePlacedOnMounts()
	{
		return canBePlacedOnMounts;
	}
	
	public void setCanBePlacedOnMounts(boolean canBePlacedOnMounts)
	{
		this.canBePlacedOnMounts = canBePlacedOnMounts;
	}
	
	public void setPlacementSettingBoardSide(PlacementSettingBoardSide placementSettingBoardSide)
	{
		this.placementSettingBoardSide = placementSettingBoardSide;
	}
	
	public PlacementSettingBoardSide getPlacementSettingBoardSide()
	{
		return placementSettingBoardSide;
	}
	
	public void setPlacementSettingBoardSquare(PlacementSettingBoardSquare placementSettingBoardSquare)
	{
		this.placementSettingBoardSquare = placementSettingBoardSquare;
	}
	
	public PlacementSettingBoardSquare getPlacementSettingBoardSquare()
	{
		return placementSettingBoardSquare;
	}
}

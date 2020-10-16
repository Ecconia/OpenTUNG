package de.ecconia.java.opentung.components.meta;

import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.Meshable;
import de.ecconia.java.opentung.components.fragments.TexturedFace;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;
import de.ecconia.java.opentung.libwrap.vaos.LabelVAO;
import de.ecconia.java.opentung.util.math.Vector3;
import java.util.ArrayList;
import java.util.List;

public class ModelHolder
{
	private Vector3 offset = Vector3.zero;
	
	private final List<CubeFull> pegModels = new ArrayList<>();
	
	public void addPeg(CubeFull pegModel)
	{
		pegModels.add(pegModel);
	}
	
	public List<CubeFull> getPegModels()
	{
		return pegModels;
	}
	
	private final List<CubeFull> blotModels = new ArrayList<>();
	
	public void addBlot(CubeFull blotModel)
	{
		blotModels.add(blotModel);
	}
	
	public void addColorable(Meshable colorable)
	{
		colorables.add(colorable);
	}
	
	public List<CubeFull> getBlotModels()
	{
		return blotModels;
	}
	
	private final List<Meshable> colorables = new ArrayList<>();
	private final List<Meshable> solid = new ArrayList<>();
	private final List<Meshable> conductors = new ArrayList<>();
	private final List<Meshable> textures = new ArrayList<>();
	
	private GenericVAO textureVAO;
	
	public void setPlacementOffset(Vector3 offset)
	{
		this.offset = offset;
	}
	
	//### Adders ###
	
	public void addSolid(Meshable meshable)
	{
		//Bounds and outline
		solid.add(meshable); //Solid to be drawn.
	}
	
	public void addTexture(TexturedFace meshable)
	{
		//Non bounds, non outline
		textures.add(meshable);
	}
	
	public void addConductor(Meshable meshable)
	{
		//Non bounds, but outline
		conductors.add(meshable);
	}
	
	//### Generators ###
	
	/**
	 * Generates a full object ready to be drawn with input->output peg/blob states as given as parameters.
	 */
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
	
	// Getters:
	
	public List<Meshable> getSolid()
	{
		return solid;
	}
	
	public List<Meshable> getConductors()
	{
		return conductors;
	}
	
	public Vector3 getPlacementOffset()
	{
		return offset;
	}
	
	public boolean hasColorables()
	{
		return !colorables.isEmpty();
	}
	
	public List<Meshable> getColorables()
	{
		return colorables;
	}
}

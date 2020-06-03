package de.ecconia.java.opentung.components.meta;

import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.CubeTunnel;
import de.ecconia.java.opentung.components.fragments.Meshable;
import de.ecconia.java.opentung.components.fragments.TexturedFace;
import de.ecconia.java.opentung.libwrap.vaos.DynamicBoardVAO;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;
import de.ecconia.java.opentung.libwrap.vaos.LabelVAO;
import de.ecconia.java.opentung.libwrap.vaos.LineVAO;
import de.ecconia.java.opentung.libwrap.vaos.SolidVAO;
import de.ecconia.java.opentung.libwrap.vaos.WireVAO;
import de.ecconia.java.opentung.math.Vector3;
import java.util.ArrayList;
import java.util.List;

public class ModelHolder
{
	private Vector3 offset = Vector3.zero;
	
	private final List<Meshable> connector = new ArrayList<>(); //Separate for checking locations.
	
	private final List<Meshable> solid = new ArrayList<>();
	private final List<Meshable> conductors = new ArrayList<>();
	private final List<Meshable> textures = new ArrayList<>();
	
	private GenericVAO vao;
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
	
	public void addConnector(Meshable meshable)
	{
		//Bounds and outline - connector
		connector.add(meshable); //Check bounds for this.
		conductors.add(meshable); //Draw as wire.
	}
	
	public void addTexture(TexturedFace meshable)
	{
		//Non bounds, non outline
		textures.add(meshable);
	}
	
	public void addMeta(Meshable meshable)
	{
		//Non bounds, but outline
		if(meshable instanceof CubeFull && ((CubeFull) meshable).getColor() == null)
		{
			conductors.add(meshable);
		}
		else
		{
			//Solid own list?
			throw new IllegalArgumentException("Uff no don't put that here without checking first - dev doesn't know what he is doing.");
		}
	}
	
	//### Generators ###
	
	/**
	 * Generates a full object ready to be drawn with input->output peg/blob states as given as parameters.
	 */
	public void generateTestModel(TestModelType type, boolean... b)
	{
		int vCount = 0;
		int iCount = 0;
		for(Meshable m : solid)
		{
			vCount += m.getVCount();
			iCount += m.getICount();
			
		}
		for(Meshable m : conductors)
		{
			vCount += m.getVCount();
			iCount += m.getICount();
		}
		
		float[] vertices = new float[vCount];
		short[] indices = new short[iCount];
		IntHolder offsetV = new IntHolder();
		IntHolder offsetI = new IntHolder();
		IntHolder indexOffset = new IntHolder();
		int argCount = 0;
		for(Meshable m : solid)
		{
			m.generateModel(vertices, offsetV, indices, offsetI, indexOffset, type, offset);
		}
		for(Meshable m : conductors)
		{
			boolean extColor = m instanceof CubeFull && !(m instanceof CubeTunnel) && ((CubeFull) m).getColor() == null;
			if(extColor)
			{
				((CubeFull) m).setColor(b[argCount++] ? Color.circuitON : Color.circuitOFF);
			}
			m.generateModel(vertices, offsetV, indices, offsetI, indexOffset, type, offset);
			if(extColor)
			{
				((CubeFull) m).setColor(null);
			}
		}
		
		if(type == TestModelType.Board)
		{
			//Shader has texture+side, and scales the board
			vao = new DynamicBoardVAO(vertices, indices);
		}
		else if(type == TestModelType.Line)
		{
			//Shader has no normals
			vao = new LineVAO(vertices, indices);
		}
		//Currently only a tunnel which is a cube...
		else if(type == TestModelType.Wire)
		{
			//Shader has no color, and scales the length
			vao = new WireVAO(vertices, indices);
		}
		else
		{
			//Shader has position, normals and color
			vao = new SolidVAO(vertices, indices);
		}
		
		//Shader only has position and texture, TODO: add normals cause it defaults to Y+
		if(!textures.isEmpty())
		{
			vCount = 0;
			iCount = 0;
			for(Meshable m : textures)
			{
				vCount += m.getVCount();
				iCount += m.getICount();
			}
			vertices = new float[vCount];
			indices = new short[iCount];
			offsetV = new IntHolder();
			offsetI = new IntHolder();
			indexOffset = new IntHolder();
			for(Meshable m : textures)
			{
				m.generateModel(vertices, offsetV, indices, offsetI, indexOffset, type, offset);
			}
			textureVAO = new LabelVAO(vertices, indices);
		}
	}
	
	public void draw()
	{
		vao.use();
		vao.draw();
	}
	
	public void drawTextures()
	{
		textureVAO.use();
		textureVAO.draw();
	}
	
	public enum TestModelType
	{
		Simple,
		Board,
		Wire,
		Line;
	}
	
	public static class IntHolder
	{
		int value;
		
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
	}
	
	// Getters:
	
	public List<Meshable> getConnectors()
	{
		return connector;
	}
	
	public List<Meshable> getSolid()
	{
		return solid;
	}
	
	public Vector3 getPlacementOffset()
	{
		return offset;
	}
}

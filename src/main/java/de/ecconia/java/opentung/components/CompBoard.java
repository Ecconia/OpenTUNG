package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeBoard;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.ModelMapper;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.CustomColor;
import de.ecconia.java.opentung.components.meta.CustomData;
import de.ecconia.java.opentung.components.meta.ModelBuilder;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.components.meta.Part;
import de.ecconia.java.opentung.components.meta.PlaceableInfo;
import de.ecconia.java.opentung.components.meta.PlacementSettingBoardSide;
import de.ecconia.java.opentung.components.meta.PlacementSettingBoardSquare;
import de.ecconia.java.opentung.meshing.MeshTypeThing;
import de.ecconia.java.opentung.util.io.ByteLevelHelper;
import de.ecconia.java.opentung.util.io.ByteReader;
import de.ecconia.java.opentung.util.math.Vector3;

public class CompBoard extends CompContainer implements CustomData, CustomColor
{
	public static final ModelHolder modelHolder = new ModelBuilder()
			.setPlacementOffset(new Vector3(0.0, 0.0, 0.0))
			.addSolid(new CubeBoard(new Vector3(0.0, 0.0, 0.0), new Vector3(2.0, 0.15, 2.0), new ModelMapper()
			{
				@Override
				public Vector3 getMappedSize(Vector3 size, Part component)
				{
					CompBoard board = (CompBoard) component;
					return new Vector3(size.getX() * board.getX() * 0.15, size.getY(), size.getZ() * board.getZ() * 0.15);
				}
			})) //1 gets replaced in shader. no color cause texture.
			//The following settings will be ignored in the actual code, since boards get special treatment. But for testing they remain:
			.setMountPlaceable(true)
			.setBoardPlacementOption(PlacementSettingBoardSquare.Middle)
			.setBoardSidePlacementOption(PlacementSettingBoardSide.Middle)
			.build();
	public static final PlaceableInfo info = new PlaceableInfo(modelHolder, "TUNG-Board", "0.2.6", CompBoard.class, CompBoard::new);
	
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
	
	protected Color color = Color.boardDefault;
	protected int x, z;
	
	public CompBoard(CompContainer parent)
	{
		super(parent);
	}
	
	public CompBoard(CompContainer parent, int x, int z)
	{
		super(parent);
		
		this.x = x;
		this.z = z;
	}
	
	@Override
	public void setColor(Color color)
	{
		this.color = color;
	}
	
	@Override
	public Color getColor()
	{
		return color;
	}
	
	public void setSize(int x, int z)
	{
		this.x = x;
		this.z = z;
	}
	
	public int getX()
	{
		return x;
	}
	
	public int getZ()
	{
		return z;
	}
	
	//### GL-Stuff ###
	
	public void insertMeshData(float[] vertices, ModelHolder.IntHolder verticesIndex, int[] indices, ModelHolder.IntHolder indicesIndex, ModelHolder.IntHolder vertexCounter, MeshTypeThing type)
	{
		//TODO: This is very ungeneric. Well the model is known.
		CubeFull shape = (CubeFull) getModelHolder().getSolid().get(0);
		shape.generateMeshEntry(this, vertices, verticesIndex, indices, indicesIndex, vertexCounter, this.color, getPositionGlobal(), getAlignmentGlobal(), modelHolder.getPlacementOffset(), type);
	}
	
	//### Save/Load ###
	
	@Override
	public byte[] getCustomData()
	{
		byte[] bytes = new byte[4 + 4 + 3];
		ByteLevelHelper.writeUncompressedInt(x, bytes, 0);
		ByteLevelHelper.writeUncompressedInt(z, bytes, 4);
		bytes[8] = (byte) color.getR();
		bytes[9] = (byte) color.getG();
		bytes[10] = (byte) color.getB();
		return bytes;
	}
	
	@Override
	public void setCustomData(byte[] data)
	{
		ByteReader reader = new ByteReader(data);
		x = reader.readIntLE();
		z = reader.readIntLE();
		int r = reader.readUnsignedByte();
		int g = reader.readUnsignedByte();
		int b = reader.readUnsignedByte();
		color = new Color(r, g, b);
	}
	
	@Override
	public Component copy()
	{
		CompBoard copy = new CompBoard(null, x, z);
		copy.setPositionGlobal(positionGlobal);
		copy.setAlignmentGlobal(alignmentGlobal);
		copy.init(); //Does nothing but better be sure.
		copy.setColor(color);
		return copy;
	}
}

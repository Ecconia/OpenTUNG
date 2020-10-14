package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.PlaceableInfo;
import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeBoard;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.ModelMapper;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.CustomData;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.components.meta.Part;
import de.ecconia.java.opentung.libwrap.meshes.MeshTypeThing;
import de.ecconia.java.opentung.math.Vector3;
import de.ecconia.java.opentung.util.io.ByteLevelHelper;
import de.ecconia.java.opentung.util.io.ByteReader;

public class CompBoard extends CompContainer implements CustomData
{
	public static final ModelHolder modelHolder = new ModelHolder();
	public static final PlaceableInfo info = new PlaceableInfo(modelHolder, "TUNG-Board", "0.2.6", CompBoard.class, CompBoard::new);
	
	static
	{
		modelHolder.setPlacementOffset(new Vector3(0.0, 0.0, 0.0));
		modelHolder.addSolid(new CubeBoard(new Vector3(0.0, 0.0, 0.0), new Vector3(2.0, 0.15, 2.0), new ModelMapper()
		{
			@Override
			public Vector3 getMappedSize(Vector3 size, Part component)
			{
				CompBoard board = (CompBoard) component;
				return new Vector3(size.getX() * board.getX() * 0.15, size.getY(), size.getZ() * board.getZ() * 0.15);
			}
		})); //1 gets replaced in shader. no color cause texture.
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
	
	private Color color = Color.boardDefault;
	private int x, z;
	
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
	
	public void setColor(Color color)
	{
		this.color = color;
	}
	
	public Color getColor()
	{
		return color;
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
		//TODO: This is still ungeneric.
		CubeFull shape = (CubeFull) getModelHolder().getSolid().get(0);
		
		shape.generateMeshEntry(this, vertices, verticesIndex, indices, indicesIndex, vertexCounter, this.color, getPosition(), getRotation(), modelHolder.getPlacementOffset(), type);
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
}

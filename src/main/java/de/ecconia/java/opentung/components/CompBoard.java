package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.PlaceableInfo;
import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeBoard;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.ModelMapper;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.components.meta.Part;
import de.ecconia.java.opentung.libwrap.meshes.MeshTypeThing;
import de.ecconia.java.opentung.math.Vector3;

public class CompBoard extends CompContainer
{
	public static final ModelHolder modelHolder = new ModelHolder();
	public static final PlaceableInfo info = new PlaceableInfo(modelHolder, "TUNG-Board", new PlaceableInfo.CompGenerator()
	{
		@Override
		public Component generateComponent(CompContainer parent)
		{
			throw new RuntimeException("Board component cannot be instantiated like this!");
		}
	});
	
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
		
		Color color = this.color;
		if(type.colorISID())
		{
			int id = getRayID();
			int r = id & 0xFF;
			int g = (id & 0xFF00) >> 8;
			int b = (id & 0xFF0000) >> 16;
			color = new Color(r, g, b);
		}
		shape.generateMeshEntry(this, vertices, verticesIndex, indices, indicesIndex, vertexCounter, color, getPosition(), getRotation(), modelHolder.getPlacementOffset(), type);
	}
}

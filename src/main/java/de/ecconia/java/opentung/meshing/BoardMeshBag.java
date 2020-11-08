package de.ecconia.java.opentung.meshing;

import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.libwrap.vaos.LargeGenericVAO;
import org.lwjgl.opengl.GL30;

public class BoardMeshBag extends MeshBag
{
	public BoardMeshBag(MeshBagContainer meshBagContainer)
	{
		super(meshBagContainer);
	}
	
	@Override
	public void rebuild()
	{
		if(vao != null)
		{
			vao.unload();
		}
		
		MeshTypeThing type = MeshTypeThing.Board;
		
		int verticesAmount = this.verticesAmount * type.getFloatCount();
		int indicesAmount = this.verticesAmount / 4 * 2 * 3;
		
		float[] vertices = new float[verticesAmount];
		int[] indices = new int[indicesAmount];
		
		ModelHolder.IntHolder vertexCounter = new ModelHolder.IntHolder();
		ModelHolder.IntHolder verticesOffset = new ModelHolder.IntHolder();
		ModelHolder.IntHolder indicesOffset = new ModelHolder.IntHolder();
		for(Component component : components)
		{
			component.insertMeshData(vertices, verticesOffset, indices, indicesOffset, vertexCounter, type);
		}
		
		vao = new BoardMeshVAO(vertices, indices);
	}
	
	private static class BoardMeshVAO extends LargeGenericVAO
	{
		protected BoardMeshVAO(float[] vertices, int[] indices)
		{
			super(vertices, indices);
		}
		
		@Override
		protected void init()
		{
			//Position:
			GL30.glVertexAttribPointer(0, 3, GL30.GL_FLOAT, false, 11 * Float.BYTES, 0);
			GL30.glEnableVertexAttribArray(0);
			//Normal:
			GL30.glVertexAttribPointer(1, 3, GL30.GL_FLOAT, false, 11 * Float.BYTES, 3 * Float.BYTES);
			GL30.glEnableVertexAttribArray(1);
			//Texture-Coords:
			GL30.glVertexAttribPointer(2, 2, GL30.GL_FLOAT, false, 11 * Float.BYTES, (3 + 3) * Float.BYTES);
			GL30.glEnableVertexAttribArray(2);
			//Color:
			GL30.glVertexAttribPointer(3, 3, GL30.GL_FLOAT, false, 11 * Float.BYTES, (3 + 3 + 2) * Float.BYTES);
			GL30.glEnableVertexAttribArray(3);
		}
	}
}

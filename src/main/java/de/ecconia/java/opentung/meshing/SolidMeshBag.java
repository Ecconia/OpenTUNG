package de.ecconia.java.opentung.meshing;

import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.libwrap.vaos.LargeGenericVAO;
import org.lwjgl.opengl.GL30;

public class SolidMeshBag extends MeshBag
{
	public SolidMeshBag(MeshBagContainer meshBagContainer)
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
		
		int verticesAmount = 0;
		int indicesAmount = 0;
		for(Component component : components)
		{
			verticesAmount += component.getWholeMeshEntryVCount(MeshTypeThing.Solid);
			indicesAmount += component.getWholeMeshEntryICount(MeshTypeThing.Solid);
		}
		
		float[] vertices = new float[verticesAmount];
		int[] indices = new int[indicesAmount];
		
		ModelHolder.IntHolder vertexCounter = new ModelHolder.IntHolder();
		ModelHolder.IntHolder verticesOffset = new ModelHolder.IntHolder();
		ModelHolder.IntHolder indicesOffset = new ModelHolder.IntHolder();
		for(Component comp : components)
		{
			comp.insertMeshData(vertices, verticesOffset, indices, indicesOffset, vertexCounter, MeshTypeThing.Solid);
		}
		
		vao = new SolidMeshVAO(vertices, indices);
	}
	
	private static class SolidMeshVAO extends LargeGenericVAO
	{
		protected SolidMeshVAO(float[] vertices, int[] indices)
		{
			super(vertices, indices);
		}
		
		@Override
		protected void init()
		{
			//Position:
			GL30.glVertexAttribPointer(0, 3, GL30.GL_FLOAT, false, 9 * Float.BYTES, 0);
			GL30.glEnableVertexAttribArray(0);
			//Normal:
			GL30.glVertexAttribPointer(1, 3, GL30.GL_FLOAT, false, 9 * Float.BYTES, 3 * Float.BYTES);
			GL30.glEnableVertexAttribArray(1);
			//Color:
			GL30.glVertexAttribPointer(2, 3, GL30.GL_FLOAT, false, 9 * Float.BYTES, 6 * Float.BYTES);
			GL30.glEnableVertexAttribArray(2);
		}
	}
}

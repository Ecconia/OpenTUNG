package de.ecconia.java.opentung.libwrap.meshes;

import de.ecconia.java.opentung.components.CompWireRaw;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;
import de.ecconia.java.opentung.libwrap.vaos.LargeGenericVAO;
import java.util.List;
import org.lwjgl.opengl.GL30;

public class ConductorMesh
{
	private final ShaderProgram solidMeshShader;
	private final GenericVAO vao;
	
	public ConductorMesh(List<Component> components, List<CompWireRaw> wires)
	{
		this.solidMeshShader = new ShaderProgram("meshConductor");
		
		int verticesAmount = wires.size() * 4 * 4 * (3 + 3);
		int indicesAmount = wires.size() * 4 * (2 * 3);
		for(Component component : components)
		{
			verticesAmount += component.getWholeMeshEntryVCount(MeshTypeThing.Conductor);
			indicesAmount += component.getWholeMeshEntryICount(MeshTypeThing.Conductor);
		}
		
		float[] vertices = new float[verticesAmount];
		int[] indices = new int[indicesAmount];
		
		ModelHolder.IntHolder vertexCounter = new ModelHolder.IntHolder();
		ModelHolder.IntHolder verticesOffset = new ModelHolder.IntHolder();
		ModelHolder.IntHolder indicesOffset = new ModelHolder.IntHolder();
		for(CompWireRaw wire : wires)
		{
			wire.insertMeshData(vertices, verticesOffset, indices, indicesOffset, vertexCounter, MeshTypeThing.Conductor);
		}
		for(Component comp : components)
		{
			comp.insertMeshData(vertices, verticesOffset, indices, indicesOffset, vertexCounter, MeshTypeThing.Conductor);
		}
		
		vao = new SolidMeshVAO(vertices, indices);
	}
	
	public void draw(float[] view)
	{
		solidMeshShader.use();
		solidMeshShader.setUniform(1, view);
		vao.use();
		vao.draw();
	}
	
	public void updateProjection(float[] projection)
	{
		solidMeshShader.use();
		solidMeshShader.setUniform(0, projection);
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
			GL30.glVertexAttribPointer(0, 3, GL30.GL_FLOAT, false, 6 * Float.BYTES, 0);
			GL30.glEnableVertexAttribArray(0);
			//Normal:
			GL30.glVertexAttribPointer(1, 3, GL30.GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES);
			GL30.glEnableVertexAttribArray(1);
		}
	}
}

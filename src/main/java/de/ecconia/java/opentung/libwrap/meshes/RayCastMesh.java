package de.ecconia.java.opentung.libwrap.meshes;

import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.CompWireRaw;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;
import java.util.List;
import org.lwjgl.opengl.GL30;

public class RayCastMesh
{
	private final ShaderProgram raycastShader;
	private final GenericVAO vao;
	
	public RayCastMesh(List<CompBoard> boards, List<CompWireRaw> wires, List<Component> components)
	{
		this.raycastShader = new ShaderProgram("raycast/meshRaycast");
		
		int verticesAmount = boards.size() * 6 * 4 * (3 + 3);
		int indicesAmount = boards.size() * 6 * 2 * 3;
		verticesAmount += wires.size() * 4 * 4 * (3 + 3);
		indicesAmount += wires.size() * 4 * 2 * 3;
//		for(Component component : components)
//		{
//			verticesAmount += 0;
//			indicesAmount += 0;
//		}
		
		float[] vertices = new float[verticesAmount];
		short[] indices = new short[indicesAmount];
		
		int verticesOffset = 0;
		int indicesOffset = 0;
		ModelHolder.IntHolder vertexCounter = new ModelHolder.IntHolder();
		for(CompBoard board : boards)
		{
			board.insertMeshData(vertices, verticesOffset, indices, indicesOffset, vertexCounter, MeshTypeThing.Raycast);
			verticesOffset += 6 * 4 * (3 + 3);
			indicesOffset += 6 * 2 * 3;
		}
		for(CompWireRaw wire : wires)
		{
			wire.insertMeshData(vertices, verticesOffset, indices, indicesOffset, vertexCounter, MeshTypeThing.Raycast);
			verticesOffset += 4 * 4 * (3 + 3);
			indicesOffset += 4 * 2 * 3;
		}
		
		vao = new RayCastMeshVAO(vertices, indices);
	}
	
	public void draw(float[] view)
	{
		raycastShader.use();
		raycastShader.setUniform(1, view);
		vao.use();
		vao.draw();
	}
	
	public void updateProjection(float[] projection)
	{
		raycastShader.use();
		raycastShader.setUniform(0, projection);
	}
	
	private static class RayCastMeshVAO extends GenericVAO
	{
		protected RayCastMeshVAO(float[] vertices, short[] indices)
		{
			super(vertices, indices);
		}
		
		@Override
		protected void init()
		{
			//Position:
			GL30.glVertexAttribPointer(0, 3, GL30.GL_FLOAT, false, 6 * Float.BYTES, 0);
			GL30.glEnableVertexAttribArray(0);
			//Color:
			GL30.glVertexAttribPointer(1, 3, GL30.GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES);
			GL30.glEnableVertexAttribArray(1);
		}
	}
}

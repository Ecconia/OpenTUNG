package de.ecconia.java.opentung.libwrap.meshes;

import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.conductor.Blot;
import de.ecconia.java.opentung.components.conductor.CompWireRaw;
import de.ecconia.java.opentung.components.conductor.Peg;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;
import de.ecconia.java.opentung.libwrap.vaos.LargeGenericVAO;
import java.util.List;
import org.lwjgl.opengl.GL30;

public class RayCastMesh
{
	private final ShaderProgram raycastShader;
	private GenericVAO vao;
	
	public RayCastMesh(List<CompBoard> boards, List<CompWireRaw> wires, List<Component> components)
	{
		this.raycastShader = new ShaderProgram("mesh/meshRaycast");
		
		update(boards, wires, components);
	}
	
	public void update(List<CompBoard> boards, List<CompWireRaw> wires, List<Component> components)
	{
		if(vao != null)
		{
			vao.unload();
		}
		
		int verticesAmount = boards.size() * 6 * 4 * (3 + 3);
		int indicesAmount = boards.size() * 6 * 2 * 3;
		verticesAmount += wires.size() * 4 * 4 * (3 + 3);
		indicesAmount += wires.size() * 4 * 2 * 3;
		for(Component component : components)
		{
			verticesAmount += component.getWholeMeshEntryVCount(MeshTypeThing.Raycast);
			indicesAmount += component.getWholeMeshEntryICount(MeshTypeThing.Raycast);
			for(Peg peg : component.getPegs())
			{
				verticesAmount += peg.getWholeMeshEntryVCount(MeshTypeThing.Raycast);
				indicesAmount += peg.getWholeMeshEntryICount(MeshTypeThing.Raycast);
			}
			for(Blot blot : component.getBlots())
			{
				verticesAmount += blot.getWholeMeshEntryVCount(MeshTypeThing.Raycast);
				indicesAmount += blot.getWholeMeshEntryICount(MeshTypeThing.Raycast);
			}
		}
		
		float[] vertices = new float[verticesAmount];
		int[] indices = new int[indicesAmount];
		
		ModelHolder.IntHolder vertexCounter = new ModelHolder.IntHolder();
		ModelHolder.IntHolder verticesOffset = new ModelHolder.IntHolder();
		ModelHolder.IntHolder indicesOffset = new ModelHolder.IntHolder();
		for(CompBoard board : boards)
		{
			board.insertMeshData(vertices, verticesOffset, indices, indicesOffset, vertexCounter, MeshTypeThing.Raycast);
		}
		for(CompWireRaw wire : wires)
		{
			wire.insertMeshData(vertices, verticesOffset, indices, indicesOffset, vertexCounter, MeshTypeThing.Raycast);
		}
		for(Component comp : components)
		{
			comp.insertMeshData(vertices, verticesOffset, indices, indicesOffset, vertexCounter, MeshTypeThing.Raycast);
			for(Peg peg : comp.getPegs())
			{
				peg.insertMeshData(vertices, verticesOffset, indices, indicesOffset, vertexCounter, MeshTypeThing.Raycast);
			}
			for(Blot blot : comp.getBlots())
			{
				blot.insertMeshData(vertices, verticesOffset, indices, indicesOffset, vertexCounter, MeshTypeThing.Raycast);
			}
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
	
	private static class RayCastMeshVAO extends LargeGenericVAO
	{
		protected RayCastMeshVAO(float[] vertices, int[] indices)
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

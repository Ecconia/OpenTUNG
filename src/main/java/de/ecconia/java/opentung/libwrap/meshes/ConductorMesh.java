package de.ecconia.java.opentung.libwrap.meshes;

import de.ecconia.java.opentung.components.CompSnappingPeg;
import de.ecconia.java.opentung.components.CompWireRaw;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.Meshable;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;
import de.ecconia.java.opentung.libwrap.vaos.LargeGenericVAO;
import java.util.Arrays;
import java.util.List;
import org.lwjgl.opengl.GL30;

public class ConductorMesh
{
	private final ShaderProgram solidMeshShader;
	private final GenericVAO vao;
	
	//TODO: Apply check, that the ID's never get over the size below *32
	//TODO: Apply check, that the amount of array positions gets generated automatically.
	private final int[] falseDataArray = new int[1016 * 4];
	
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
		int[] clusterIDs = new int[indicesAmount / 6 * 4];
		
		ModelHolder.IntHolder clusterIDIndex = new ModelHolder.IntHolder();
		ModelHolder.IntHolder clusterIDCounter = new ModelHolder.IntHolder();
		ModelHolder.IntHolder vertexCounter = new ModelHolder.IntHolder();
		ModelHolder.IntHolder verticesOffset = new ModelHolder.IntHolder();
		ModelHolder.IntHolder indicesOffset = new ModelHolder.IntHolder();
		for(CompWireRaw wire : wires)
		{
			wire.insertMeshData(vertices, verticesOffset, indices, indicesOffset, vertexCounter, MeshTypeThing.Conductor);
			//TODO: Ungeneric:
			int clusterID = clusterIDCounter.getAndInc();
			//Wire has 4 Sides, each 4 vertices: 16
			for(int i = 0; i < 4 * 4; i++)
			{
				clusterIDs[clusterIDIndex.getAndInc()] = clusterID;
			}
		}
		for(Component comp : components)
		{
			comp.insertMeshData(vertices, verticesOffset, indices, indicesOffset, vertexCounter, MeshTypeThing.Conductor);
			if(comp instanceof CompSnappingPeg)
			{
				continue;
			}
			for(Meshable m : comp.getModelHolder().getConnectors())
			{
				//TODO: Ungeneric:
				int clusterID = clusterIDCounter.getAndInc();
				for(int i = 0; i < ((CubeFull) m).getFacesCount() * 4; i++)
				{
					clusterIDs[clusterIDIndex.getAndInc()] = clusterID;
				}
			}
		}
		System.out.println(clusterIDCounter);
		
		vao = new SolidMeshVAO(vertices, indices, clusterIDs);
		
		Arrays.fill(falseDataArray, 0xF0F0F0F0);
	}
	
	public void draw(float[] view)
	{
		solidMeshShader.use();
		solidMeshShader.setUniform(1, view);
		solidMeshShader.setUniformArray(2, falseDataArray);
		solidMeshShader.setUniform(3, view);
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
		protected SolidMeshVAO(float[] vertices, int[] indices, int[] ids)
		{
			super(vertices, indices, ids);
		}
		
		@Override
		protected void uploadMoreData(Object... extra)
		{
			int vboID = GL30.glGenBuffers();
			GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, vboID);
			GL30.glBufferData(GL30.GL_ARRAY_BUFFER, (int[]) extra[0], GL30.GL_STATIC_DRAW);
			//ClusterID:
			GL30.glVertexAttribIPointer(2, 1, GL30.GL_UNSIGNED_INT,	Integer.BYTES, 0);
			GL30.glEnableVertexAttribArray(2);
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

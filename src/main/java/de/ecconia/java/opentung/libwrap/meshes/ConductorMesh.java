package de.ecconia.java.opentung.libwrap.meshes;

import de.ecconia.java.opentung.components.CompSnappingPeg;
import de.ecconia.java.opentung.components.conductor.Blot;
import de.ecconia.java.opentung.components.conductor.CompWireRaw;
import de.ecconia.java.opentung.components.conductor.Peg;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;
import de.ecconia.java.opentung.libwrap.vaos.LargeGenericVAO;
import de.ecconia.java.opentung.simulation.Cluster;
import de.ecconia.java.opentung.simulation.SimulationManager;
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
	
	public ConductorMesh(List<Component> components, List<CompWireRaw> wires, List<Cluster> clusters, SimulationManager simulation)
	{
		this.solidMeshShader = new ShaderProgram("mesh/meshConductor");
		simulation.setConnectorMeshStates(falseDataArray);
		
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
		ModelHolder.IntHolder vertexCounter = new ModelHolder.IntHolder();
		ModelHolder.IntHolder verticesOffset = new ModelHolder.IntHolder();
		ModelHolder.IntHolder indicesOffset = new ModelHolder.IntHolder();
		for(CompWireRaw wire : wires)
		{
			wire.insertMeshData(vertices, verticesOffset, indices, indicesOffset, vertexCounter, MeshTypeThing.Conductor);
			//TODO: Ungeneric:
			if(wire.hasCluster())
			{
				int clusterID = wire.getCluster().getId();
				//Wire has 4 Sides, each 4 vertices: 16
				for(int i = 0; i < 4 * 4; i++)
				{
					clusterIDs[clusterIDIndex.getAndInc()] = clusterID;
				}
			}
			else
			{
				throw new RuntimeException("Found wire without a cluster :/");
			}
		}
		for(Component comp : components)
		{
			comp.insertMeshData(vertices, verticesOffset, indices, indicesOffset, vertexCounter, MeshTypeThing.Conductor);
			if(comp instanceof CompSnappingPeg)
			{
				continue;
			}
			//TODO: Ungeneric:
			for(Peg peg : comp.getPegs())
			{
				if(peg.hasCluster())
				{
					int clusterID = peg.getCluster().getId();
					for(int i = 0; i < peg.getModel().getFacesCount() * 4; i++)
					{
						clusterIDs[clusterIDIndex.getAndInc()] = clusterID;
					}
				}
				else
				{
					throw new RuntimeException("Found peg without a cluster :/");
				}
			}
			for(Blot blot : comp.getBlots())
			{
				if(blot.hasCluster())
				{
					int clusterID = blot.getCluster().getId();
					for(int i = 0; i < blot.getModel().getFacesCount() * 4; i++)
					{
						clusterIDs[clusterIDIndex.getAndInc()] = clusterID;
					}
				}
				else
				{
					throw new RuntimeException("Found blot without a cluster :/");
				}
			}
		}
		
		vao = new SolidMeshVAO(vertices, indices, clusterIDs);
		
		//By clusters:
		Arrays.fill(falseDataArray, 0);
		for(Cluster cluster : clusters)
		{
			setStateByID(cluster.getId(), cluster.isActive());
		}
	}
	
	private void setStateByID(int i, boolean active)
	{
		int index = i / 32;
		int offset = i % 32;
		int mask = (1 << offset);
		
		int value = falseDataArray[index];
		value = value & ~mask;
		if(active)
		{
			value |= mask;
		}
		falseDataArray[index] = value;
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
			GL30.glVertexAttribIPointer(2, 1, GL30.GL_UNSIGNED_INT, Integer.BYTES, 0);
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

package de.ecconia.java.opentung.meshing;

import de.ecconia.java.opentung.components.conductor.Blot;
import de.ecconia.java.opentung.components.conductor.CompWireRaw;
import de.ecconia.java.opentung.components.conductor.Peg;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.Meshable;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.libwrap.vaos.LargeGenericVAO;
import de.ecconia.java.opentung.simulation.Cluster;
import de.ecconia.java.opentung.simulation.Clusterable;
import de.ecconia.java.opentung.simulation.SimulationManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import org.lwjgl.opengl.GL30;

public class ConductorMeshBag extends MeshBag
{
	private final Map<Cluster, ClusterInfo> clusterInfos = new HashMap<>();
	private final Queue<Integer> unusedIDs = new LinkedList<>();
	
	private int[] clusters = new int[0]; //Maximum: (4096 - 32) / 4 * 4
	private int highestAvailableIndex = 1;
	
	boolean needsInitialFix;
	
	boolean dirty = false;
	
	public ConductorMeshBag(MeshBagContainer meshBagContainer)
	{
		super(meshBagContainer);
	}
	
	public void setActive(int id, boolean active)
	{
		int index = id / 32;
		int offset = id % 32;
		int mask = (1 << offset);
		
		int value = clusters[index];
		value = value & ~mask;
		if(active)
		{
			value |= mask;
		}
		clusters[index] = value;
	}
	
	private int getFreeIndex()
	{
		Integer id = unusedIDs.poll();
		if(id != null)
		{
			return id;
		}
		
		int newIndex = highestAvailableIndex++;
		if((newIndex / 32) >= clusters.length)
		{
			//Brute force expand by 4.
			clusters = new int[clusters.length + 4];
			dirty = true;
		}
		return newIndex;
	}
	
	public int[] getDataArray()
	{
		return clusters;
	}
	
	public void addComponent(Component component, int verticesAmount, SimulationManager simulation)
	{
		List<Cluster> componentClusters = collectClusters(component);
		for(Cluster cluster : componentClusters)
		{
			if(cluster != null)
			{
				ClusterInfo ci = clusterInfos.get(cluster);
				if(ci == null)
				{
					int index = getFreeIndex();
					ci = new ClusterInfo(index);
					clusterInfos.put(cluster, ci);
					ConductorMeshBagReference newReference = new ConductorMeshBagReference(this, index);
					simulation.updateJobNextTickThreadSafe((unused) -> {
						cluster.addMeshReference(newReference);
						cluster.updateState();
					});
				}
				else
				{
					ci.incrementReference();
				}
			}
			else
			{
				needsInitialFix = true;
			}
		}
		
		super.addComponent(component, verticesAmount);
	}
	
	public void removeComponent(Component component, int verticesAmount, SimulationManager simulation)
	{
		List<Cluster> componentClusters = collectClusters(component);
		for(Cluster cluster : componentClusters)
		{
			if(cluster != null)
			{
				ClusterInfo ci = clusterInfos.get(cluster);
				if(ci != null)
				{
					ci.decrementReference();
					if(ci.getUsage() == 0)
					{
						simulation.updateJobNextTickThreadSafe((unused) -> {
							cluster.removeMeshReference(this);
							unusedIDs.add(ci.getIndex());
						});
						clusterInfos.remove(cluster);
					}
				}
			}
		}
		
		super.removeComponent(component, verticesAmount);
	}
	
	private List<Cluster> collectClusters(Component component)
	{
		List<Cluster> componentClusters = new ArrayList<>();
		if(component instanceof CompWireRaw)
		{
			componentClusters.add(((CompWireRaw) component).getCluster());
		}
		for(Peg peg : component.getPegs())
		{
			componentClusters.add(peg.getCluster());
		}
		for(Blot blot : component.getBlots())
		{
			componentClusters.add(blot.getCluster());
		}
		return componentClusters;
	}
	
	//The following method is incompatible with edits to the cluster system!
	//As in only start editing them after the conductor meshes have been rebuilt.
	public void fixInitialLoading(SimulationManager simulation)
	{
		if(needsInitialFix)
		{
			needsInitialFix = false;
			for(Component component : components)
			{
				List<Cluster> componentClusters = collectClusters(component);
				for(Cluster cluster : componentClusters)
				{
					if(cluster != null)
					{
						ClusterInfo ci = clusterInfos.get(cluster);
						if(ci == null)
						{
							int index = getFreeIndex();
							ci = new ClusterInfo(index);
							clusterInfos.put(cluster, ci);
							ConductorMeshBagReference newReference = new ConductorMeshBagReference(this, index);
							simulation.updateJobNextTickThreadSafe((unused) -> {
								cluster.addMeshReference(newReference);
								cluster.updateState();
							});
						}
						else
						{
							ci.incrementReference();
						}
					}
					else
					{
						needsInitialFix = true;
					}
				}
			}
			rebuild();
		}
	}
	
	@Override
	public void rebuild()
	{
		if(vao != null)
		{
			vao.unload();
		}
		
		MeshTypeThing type = MeshTypeThing.Conductor;
		int verticesAmount = 0;
		int indicesAmount = 0;
		for(Component component : components)
		{
			for(Peg peg : component.getPegs())
			{
				verticesAmount += peg.getWholeMeshEntryVCount(type);
				indicesAmount += peg.getWholeMeshEntryICount(type);
			}
			for(Blot blot : component.getBlots())
			{
				verticesAmount += blot.getWholeMeshEntryVCount(type);
				indicesAmount += blot.getWholeMeshEntryICount(type);
			}
			for(Meshable m : component.getModelHolder().getConductors())
			{
				verticesAmount += ((CubeFull) m).getFacesCount() * 4 * type.getFloatCount();
				indicesAmount += ((CubeFull) m).getFacesCount() * (2 * 3);
			}
		}
		
		float[] vertices = new float[verticesAmount];
		int[] indices = new int[indicesAmount];
		int[] clusterIDs = new int[indicesAmount / 6 * 4];
		
		ModelHolder.IntHolder clusterIDIndex = new ModelHolder.IntHolder();
		ModelHolder.IntHolder vertexCounter = new ModelHolder.IntHolder();
		ModelHolder.IntHolder verticesOffset = new ModelHolder.IntHolder();
		ModelHolder.IntHolder indicesOffset = new ModelHolder.IntHolder();
		for(Component comp : components)
		{
			//TODO: Ungeneric:
			for(Peg peg : comp.getPegs())
			{
				peg.insertMeshData(vertices, verticesOffset, indices, indicesOffset, vertexCounter, type);
				int clusterID = getClusterID(peg);
				for(int i = 0; i < peg.getModel().getFacesCount() * 4; i++)
				{
					clusterIDs[clusterIDIndex.getAndInc()] = clusterID;
				}
			}
			for(Blot blot : comp.getBlots())
			{
				blot.insertMeshData(vertices, verticesOffset, indices, indicesOffset, vertexCounter, type);
				int clusterID = getClusterID(blot);
				for(int i = 0; i < blot.getModel().getFacesCount() * 4; i++)
				{
					clusterIDs[clusterIDIndex.getAndInc()] = clusterID;
				}
			}
			if(comp instanceof CompWireRaw)
			{
				comp.insertMeshData(vertices, verticesOffset, indices, indicesOffset, vertexCounter, type);
				//TODO: Ungeneric:
				int clusterID = getClusterID((Clusterable) comp);
				//Wire has 4 Sides, each 4 vertices: 16
				for(int i = 0; i < 4 * 4; i++)
				{
					clusterIDs[clusterIDIndex.getAndInc()] = clusterID;
				}
			}
		}
		
		vao = new ConductorMeshVAO(vertices, indices, clusterIDs);
	}
	
	private int getClusterID(Clusterable clusterable)
	{
		//TODO: Simulation thread may temporarily set cluster to null...
		Cluster cluster = clusterable.getCluster();
		if(cluster != null)
		{
			ClusterInfo ci = clusterInfos.get(cluster);
			if(ci == null)
			{
				return 0;
			}
			return ci.getIndex();
		}
		else
		{
			return 0;
		}
	}
	
	private static class ClusterInfo
	{
		private final int index;
		
		private int usage;
		
		public ClusterInfo(int index)
		{
			this.index = index;
			usage = 1;
		}
		
		public int getIndex()
		{
			return index;
		}
		
		public int getUsage()
		{
			return usage;
		}
		
		public void incrementReference()
		{
			usage++;
		}
		
		public void decrementReference()
		{
			usage--;
		}
	}
	
	private static class ConductorMeshVAO extends LargeGenericVAO
	{
		protected ConductorMeshVAO(float[] vertices, int[] indices, int[] ids)
		{
			super(vertices, indices, ids);
		}
		
		@Override
		protected void uploadMoreData(Object... extra)
		{
			System.out.println("[MeshDebug] " + getClass().getSimpleName() + " E: " + ((int[]) extra[0]).length);
			int vboID = GL30.glGenBuffers();
			deleteLater.add(vboID);
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

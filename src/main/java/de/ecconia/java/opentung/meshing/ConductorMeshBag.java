package de.ecconia.java.opentung.meshing;

import de.ecconia.java.opentung.components.conductor.CompWireRaw;
import de.ecconia.java.opentung.components.conductor.Connector;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.Meshable;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ConnectedComponent;
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
			meshBagContainer.addDirtyConductorMB(this);
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
					ci = new ClusterInfo(index, cluster);
					clusterInfos.put(cluster, ci);
					ConductorMeshBagReference newReference = new ConductorMeshBagReference(this, index);
					simulation.updateJobNextTickThreadSafe((unused) -> {
						cluster.addMeshReference(newReference);
						cluster.updateState(); //TODO: Should this update all meshes? I don't think so!
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
						setActive(ci.getIndex(), false); //Set cluster-data to false/off, to prevent flickering on placement.
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
		else if(component instanceof ConnectedComponent) //TBI: Is this type-check required, either wire or ConnectedComponent...
		{
			for(Connector connector : ((ConnectedComponent) component).getConnectors())
			{
				componentClusters.add(connector.getCluster());
			}
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
							ci = new ClusterInfo(index, cluster);
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
			if(component instanceof ConnectedComponent)
			{
				for(Connector connector : ((ConnectedComponent) component).getConnectors())
				{
					verticesAmount += connector.getWholeMeshEntryVCount(type);
					indicesAmount += connector.getWholeMeshEntryICount(type);
				}
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
			if(comp instanceof ConnectedComponent)
			{
				for(Connector connector : ((ConnectedComponent) comp).getConnectors())
				{
					connector.insertMeshData(vertices, verticesOffset, indices, indicesOffset, vertexCounter, type);
					int clusterID = getClusterID(connector);
					for(int i = 0; i < connector.getModel().getFacesCount() * 4; i++)
					{
						clusterIDs[clusterIDIndex.getAndInc()] = clusterID;
					}
				}
			}
			else if(comp instanceof CompWireRaw)
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
	
	public void refresh(SimulationManager simulation)
	{
		List<ClusterInfo> clusters = new ArrayList<>(clusterInfos.values());
		simulation.updateJobNextTickThreadSafe((unused) -> {
			for(ClusterInfo ci : clusters)
			{
				setActive(ci.getIndex(), ci.getCluster().isActive());
			}
		});
	}
	
	public void handleUpdates(List<ConductorMBUpdate> updates, SimulationManager simulation)
	{
		HashMap<Cluster, Integer> clusterCounts = new HashMap<>();
		for(ConductorMBUpdate value : updates)
		{
			Integer integer = clusterCounts.get(value.getFrom());
			int previously = integer == null ? -1 : integer - 1;
			clusterCounts.put(value.getFrom(), previously);
			integer = clusterCounts.get(value.getTo());
			previously = integer == null ? +1 : integer + 1;
			clusterCounts.put(value.getTo(), previously);
		}
		
		for(Map.Entry<Cluster, Integer> entry : clusterCounts.entrySet())
		{
			int usages = entry.getValue();
			if(usages == 0)
			{
				continue;
			}
			Cluster cluster = entry.getKey();
			ClusterInfo ci = clusterInfos.get(cluster);
			if(ci == null)
			{
				if(usages < 0)
				{
					System.out.println("[ERROR] Attempted to remove cluster from ConductorMeshBag, but the cluster was not referenced in it!");
				}
				else
				{
					//Create cluster-info:
					int index = getFreeIndex();
					ci = new ClusterInfo(index, cluster, usages);
					clusterInfos.put(cluster, ci);
					ConductorMeshBagReference newReference = new ConductorMeshBagReference(this, index);
					simulation.updateJobNextTickThreadSafe((unused) -> {
						cluster.addMeshReference(newReference);
						cluster.updateState(); //TODO: Should this update all meshes? I don't think so!
					});
				}
			}
			else
			{
				if(usages > 0)
				{
					ci.incrementReference(usages);
				}
				else
				{
					ci.decrementReference(usages);
					int newUsage = ci.getUsage();
					if(newUsage < 0)
					{
						System.out.println("[ERROR] Attempted to remove cluster from ConductorMeshBag, but the cluster didn't have that many usages!");
					}
					if(newUsage <= 0)
					{
						ClusterInfo finalCi = ci;
						simulation.updateJobNextTickThreadSafe((unused) -> {
							cluster.removeMeshReference(this);
							unusedIDs.add(finalCi.getIndex());
						});
						clusterInfos.remove(cluster);
					}
				}
			}
		}
		
		//There is probably always a need to rebuild. Cause some of the components have different clusters.
		meshBagContainer.setDirty(this);
	}
	
	private static class ClusterInfo
	{
		private final int index;
		private final Cluster cluster;
		
		private int usage;
		
		public ClusterInfo(int index, Cluster cluster)
		{
			this(index, cluster, 1);
		}
		
		public ClusterInfo(int index, Cluster cluster, int usage)
		{
			this.index = index;
			this.cluster = cluster;
			this.usage = usage;
		}
		
		public int getIndex()
		{
			return index;
		}
		
		public int getUsage()
		{
			return usage;
		}
		
		public Cluster getCluster()
		{
			return cluster;
		}
		
		public void incrementReference()
		{
			usage++;
		}
		
		public void decrementReference()
		{
			usage--;
		}
		
		public void incrementReference(int usages)
		{
			this.usage += usages;
		}
		
		public void decrementReference(int usages)
		{
			this.usage -= usages;
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
	
	public static class ConductorMBUpdate
	{
		private final Cluster from, to;
		
		public ConductorMBUpdate(Cluster from, Cluster to)
		{
			this.from = from;
			this.to = to;
		}
		
		public Cluster getFrom()
		{
			return from;
		}
		
		public Cluster getTo()
		{
			return to;
		}
	}
}

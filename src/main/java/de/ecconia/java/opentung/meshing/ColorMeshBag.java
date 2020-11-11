package de.ecconia.java.opentung.meshing;

import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.meta.Colorable;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.libwrap.vaos.LargeGenericVAO;
import de.ecconia.java.opentung.simulation.SimulationManager;
import java.util.LinkedList;
import java.util.Queue;
import org.lwjgl.opengl.GL30;

public class ColorMeshBag extends MeshBag
{
	private final Queue<Integer> unusedIDs = new LinkedList<>();
	
	private int[] colors = new int[4]; //Maximum: (4096 - 32) / 4 * 4
	private int highestIndex = 0;
	
	private boolean dirty = false;
	
	public ColorMeshBag(MeshBagContainer meshBagContainer)
	{
		super(meshBagContainer);
	}
	
	public void setColor(int colorID, Color color)
	{
		colors[colorID] = color.getR() << 24 | color.getG() << 16 | color.getB() << 8 | 255;
	}
	
	public void addComponent(Component component, int verticesAmount, SimulationManager simulation)
	{
		//TODO: When removing also free the ID.
		Colorable colorable = (Colorable) component;
		int colorableAmount = component.getModelHolder().getColorables().size();
		for(int i = 0; i < colorableAmount; i++)
		{
			colorable.setColorMeshBag(i, new ColorMeshBagReference(this, getFreeIndex()));
		}
		//Update the current color.
		simulation.updateJobNextTickThreadSafe((unused) -> {
			colorable.updateColors();
		});
		
		super.addComponent(component, verticesAmount);
	}
	
	@Override
	public void removeComponent(Component component, int verticesAmount)
	{
		Colorable colorable = (Colorable) component;
		int colorableAmount = component.getModelHolder().getColorables().size();
		for(int i = 0; i < colorableAmount; i++)
		{
			//TODO: Simulation thread?
			ColorMeshBagReference ref = colorable.removeColorMeshBag(i);
			unusedIDs.add(ref.getIndex());
		}
		
		super.removeComponent(component, verticesAmount);
	}
	
	private int getFreeIndex()
	{
		Integer id = unusedIDs.poll();
		if(id != null)
		{
			return id;
		}
		
		int newIndex = highestIndex++;
		if(newIndex >= colors.length)
		{
			colors = new int[colors.length + 4];
			dirty = true;
		}
		return newIndex;
	}
	
	public int[] getDataArray()
	{
		return colors;
	}
	
	public void refresh(SimulationManager simulationManager)
	{
		//Restore color array data:
		if(dirty)
		{
			simulationManager.updateJobNextTickThreadSafe((unused) -> {
				for(Component component : components)
				{
					((Colorable) component).updateColors();
				}
			});
		}
	}
	
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
			verticesAmount += component.getWholeMeshEntryVCount(MeshTypeThing.Display);
			indicesAmount += component.getWholeMeshEntryICount(MeshTypeThing.Display);
		}
		
		float[] vertices = new float[verticesAmount];
		int[] indices = new int[indicesAmount];
		int[] colorIDs = new int[indicesAmount / 6 * 4]; //divide by (2 * 3) to get the amount of faces, then *4 to get each vertex.
		
		ModelHolder.IntHolder colorIDIndex = new ModelHolder.IntHolder();
		ModelHolder.IntHolder vertexCounter = new ModelHolder.IntHolder();
		ModelHolder.IntHolder verticesOffset = new ModelHolder.IntHolder();
		ModelHolder.IntHolder indicesOffset = new ModelHolder.IntHolder();
		for(Component comp : components)
		{
			if(!(comp instanceof Colorable))
			{
				continue;
			}
			
			comp.insertMeshData(vertices, verticesOffset, indices, indicesOffset, vertexCounter, MeshTypeThing.Display);
			
			int colorablesCount = comp.getModelHolder().getColorables().size();
			for(int i = 0; i < colorablesCount; i++)
			{
				CubeFull cube = (CubeFull) comp.getModelHolder().getColorables().get(i);
				
				int colorID = ((Colorable) comp).getColorMeshBag(i).getIndex();
				for(int j = 0; j < cube.getFacesCount() * 4; j++)
				{
					colorIDs[colorIDIndex.getAndInc()] = colorID;
				}
			}
		}
		
		vao = new ColorMeshVAO(vertices, indices, colorIDs);
	}
	
	private static class ColorMeshVAO extends LargeGenericVAO
	{
		protected ColorMeshVAO(float[] vertices, int[] indices, int[] ids)
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
			//ColorID:
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

package de.ecconia.java.opentung.libwrap.meshes;

import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.meta.Colorable;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;
import de.ecconia.java.opentung.libwrap.vaos.LargeGenericVAO;
import de.ecconia.java.opentung.simulation.SimulationManager;
import java.util.Arrays;
import java.util.List;
import org.lwjgl.opengl.GL30;

public class ColorMesh
{
	private final ShaderProgram colorMeshShader;
	private GenericVAO vao;
	
	//TODO: Apply check, that the ID's never get over the size below *32
	//TODO: Apply check, that the amount of array positions gets generated automatically.
	private final int[] falseDataArray = new int[(4096 - 32) / 4 * 4];
	
	public ColorMesh(List<Component> components, SimulationManager simulation)
	{
		this.colorMeshShader = new ShaderProgram("mesh/meshColor");
		simulation.setColorMeshStates(falseDataArray);
		
		update(components);
		
		Color color = Color.displayOff;
		Arrays.fill(falseDataArray, color.getR() << 24 | color.getG() << 16 | color.getB() << 8 | 0xff);
	}
	
	public void update(List<Component> components)
	{
		if(vao != null)
		{
			vao.unload();
		}
		
		int verticesAmount = 0;
		int indicesAmount = 0;
		for(Component component : components)
		{
			if(!(component instanceof Colorable))
			{
				continue;
			}
			
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
				
				int colorID = ((Colorable) comp).getColorID(i);
				for(int j = 0; j < cube.getFacesCount() * 4; j++)
				{
					colorIDs[colorIDIndex.getAndInc()] = colorID;
				}
			}
		}
		
		vao = new ColorMeshVAO(vertices, indices, colorIDs);
	}
	
	public void draw(float[] view)
	{
		colorMeshShader.use();
		colorMeshShader.setUniformM4(1, view);
		colorMeshShader.setUniformArray(2, falseDataArray);
		colorMeshShader.setUniformM4(3, view);
		vao.use();
		vao.draw();
	}
	
	public void updateProjection(float[] projection)
	{
		colorMeshShader.use();
		colorMeshShader.setUniformM4(0, projection);
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

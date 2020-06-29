package de.ecconia.java.opentung.libwrap.meshes;

import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.TextureWrapper;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;
import de.ecconia.java.opentung.libwrap.vaos.LargeGenericVAO;
import java.util.List;
import org.lwjgl.opengl.GL30;

public class TextureMesh
{
	private final TextureWrapper texture;
	private final ShaderProgram textureShader;
	private final GenericVAO vao;
	
	public TextureMesh(TextureWrapper texture, List<CompBoard> boards)
	{
		this.texture = texture;
		this.textureShader = new ShaderProgram("mesh/meshTexture");
		
		int verticesAmount = boards.size() * 6 * 4 * (3 + 3 + 2 + 3);
		int indicesAmount = boards.size() * 6 * 2 * 3;
		
		float[] vertices = new float[verticesAmount];
		int[] indices = new int[indicesAmount];
		
		ModelHolder.IntHolder vertexCounter = new ModelHolder.IntHolder();
		ModelHolder.IntHolder verticesOffset = new ModelHolder.IntHolder();
		ModelHolder.IntHolder indicesOffset = new ModelHolder.IntHolder();
		for(CompBoard board : boards)
		{
			board.insertMeshData(vertices, verticesOffset, indices, indicesOffset, vertexCounter, MeshTypeThing.Board);
		}
		
		vao = new TexMeshVAO(vertices, indices);
	}
	
	public void draw(float[] view)
	{
		texture.activate();
		textureShader.use();
		textureShader.setUniform(1, view);
		textureShader.setUniform(2, view);
		vao.use();
		vao.draw();
	}
	
	public void updateProjection(float[] projection)
	{
		textureShader.use();
		textureShader.setUniform(0, projection);
	}
	
	private static class TexMeshVAO extends LargeGenericVAO
	{
		protected TexMeshVAO(float[] vertices, int[] indices)
		{
			super(vertices, indices);
		}
		
		@Override
		protected void init()
		{
			//Position:
			GL30.glVertexAttribPointer(0, 3, GL30.GL_FLOAT, false, 11 * Float.BYTES, 0);
			GL30.glEnableVertexAttribArray(0);
			//Normal:
			GL30.glVertexAttribPointer(1, 3, GL30.GL_FLOAT, false, 11 * Float.BYTES, 3 * Float.BYTES);
			GL30.glEnableVertexAttribArray(1);
			//Texture-Coords:
			GL30.glVertexAttribPointer(2, 2, GL30.GL_FLOAT, false, 11 * Float.BYTES, (3 + 3) * Float.BYTES);
			GL30.glEnableVertexAttribArray(2);
			//Color:
			GL30.glVertexAttribPointer(3, 3, GL30.GL_FLOAT, false, 11 * Float.BYTES, (3 + 3 + 2) * Float.BYTES);
			GL30.glEnableVertexAttribArray(3);
		}
	}
}

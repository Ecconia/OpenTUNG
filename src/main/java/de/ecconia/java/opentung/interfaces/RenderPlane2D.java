package de.ecconia.java.opentung.interfaces;

import de.ecconia.java.opentung.RenderPlane;
import de.ecconia.java.opentung.inputs.Controller2D;
import de.ecconia.java.opentung.inputs.InputProcessor;
import de.ecconia.java.opentung.libwrap.Matrix;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import org.lwjgl.opengl.GL30;

public class RenderPlane2D implements RenderPlane
{
	private final Matrix projectionMatrix = new Matrix();
	
	private ShaderProgram interfaceShader;
	
	private Point indicator;
	
	public RenderPlane2D(InputProcessor inputHandler)
	{
		inputHandler.setController(new Controller2D(this));
	}
	
	@Override
	public void setup()
	{
		interfaceShader = new ShaderProgram("interfaceShader");
	}
	
	@Override
	public void render()
	{
		interfaceShader.use();
		Matrix mat = new Matrix();
		interfaceShader.setUniform(1, mat.getMat());
		indicator.draw();
	}
	
	@Override
	public void newSize(int width, int height)
	{
		projectionMatrix.interfaceMatrix(width, height);
		interfaceShader.use();
		interfaceShader.setUniform(0, projectionMatrix.getMat());
		if(indicator != null)
		{
			indicator.unload();
		}
		indicator = new Point(width / 2, height / 2);
	}
	
	private static class Point
	{
		private final int vaoID;
		
		public Point(int centerX, int centerY)
		{
			vaoID = GL30.glGenVertexArrays();
			int vboID = GL30.glGenBuffers();
			
			GL30.glBindVertexArray(vaoID);
			
			GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, vboID);
			GL30.glBufferData(GL30.GL_ARRAY_BUFFER, new float[]{centerX, centerY, 1, 1, 1}, GL30.GL_STATIC_DRAW);
			
			//Position:
			GL30.glVertexAttribPointer(0, 2, GL30.GL_FLOAT, false, 5 * Float.BYTES, 0);
			GL30.glEnableVertexAttribArray(0);
			//Color
			GL30.glVertexAttribPointer(1, 3, GL30.GL_FLOAT, false, 5 * Float.BYTES, 2 * Float.BYTES);
			GL30.glEnableVertexAttribArray(1);
			
			//Cleanup:
			GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0);
		}
		
		public void draw()
		{
			GL30.glBindVertexArray(vaoID);
			GL30.glPointSize(3f);
			GL30.glDrawArrays(GL30.GL_POINTS, 0, 1);
		}
		
		public void unload()
		{
			GL30.glDeleteVertexArrays(vaoID);
		}
	}
}

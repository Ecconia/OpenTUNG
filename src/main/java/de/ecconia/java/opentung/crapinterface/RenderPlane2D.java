package de.ecconia.java.opentung.crapinterface;

import de.ecconia.java.opentung.RenderPlane;
import de.ecconia.java.opentung.libwrap.ColorVec;
import de.ecconia.java.opentung.libwrap.Matrix;
import de.ecconia.java.opentung.libwrap.ShaderProgram;

public class RenderPlane2D implements RenderPlane
{
	private final Matrix projectionMatrix = new Matrix();
	
	private ShaderProgram interfaceShader;
	private Slider[] sliders = new Slider[4];
	
	@Override
	public void setup()
	{
		interfaceShader = new ShaderProgram("interfaceShader");
		
		int magicHeight = 5;
		for(int i = 0; i < 4; i++)
		{
			sliders[i] = new Slider(10, 10 + i * (magicHeight * 2 + 5), 200, magicHeight, new ColorVec(0.5f, 0.0f, 0.0f), new ColorVec(1.0f, 0.1f, 0.1f), 1.0f);
		}
		
		newSize(500, 500);
	}
	
	@Override
	public void render()
	{
		interfaceShader.use();
		interfaceShader.setUniform(0, projectionMatrix.getMat());
		Matrix mat = new Matrix();
		mat.identity();
		interfaceShader.setUniform(1, mat.getMat());
		sliders[0].drawMain();
		sliders[1].drawMain();
		sliders[2].drawMain();
		sliders[3].drawMain();
		
		for(int i = 0; i < 4; i++)
		{
			interfaceShader.setUniform(1, sliders[i].getPlacementMatrix().getMat());
			sliders[i].drawHead();
		}
	}
	
	@Override
	public void newSize(int width, int height)
	{
		projectionMatrix.interfaceMatrix(width, height);
	}
}

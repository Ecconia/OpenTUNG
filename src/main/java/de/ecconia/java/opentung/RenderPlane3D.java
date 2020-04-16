package de.ecconia.java.opentung;

import de.ecconia.java.opentung.inputs.InputProcessor;
import de.ecconia.java.opentung.libwrap.Matrix;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.TextureWrapper;
import de.ecconia.java.opentung.scomponents.SimpleBlotterModel;
import de.ecconia.java.opentung.scomponents.SimpleDynamicBoard;
import de.ecconia.java.opentung.scomponents.SimpleInverterModel;
import de.ecconia.java.opentung.scomponents.SimplePeg;

import java.awt.*;

public class RenderPlane3D implements RenderPlane
{
	private static Camera camera;
	
	private static final Matrix projection = new Matrix();
	private static final Matrix model = new Matrix();
	
	private static ShaderProgram program;
	private static SimpleInverterModel inverter;
	private static SimpleBlotterModel blotter;
	private static SimplePeg peg;
	
	private static TextureWrapper boardTexture;
	private static ShaderProgram dynamicBoardShader;
	private static SimpleDynamicBoard dBoard;
	
	private static Quaternion quaternion;
	
	private static float color = 0.2f;
	private InputProcessor inputHandler;
	
	public RenderPlane3D(InputProcessor inputHandler)
	{
		this.inputHandler = inputHandler;
	}
	
	@Override
	public void setup()
	{
		program = new ShaderProgram("basicShader");
		dynamicBoardShader = new ShaderProgram("dynamicBoardShader");
		
		boardTexture = new TextureWrapper();
		
		inverter = new SimpleInverterModel();
		blotter = new SimpleBlotterModel();
		peg = new SimplePeg();
		dBoard = new SimpleDynamicBoard();
		
		camera = new Camera(inputHandler);
		
		quaternion = new Quaternion();
		
		projection.perspective(45f, (float) 500 / (float) 500, 0.1f, 100000f);
	}
	
	@Override
	public void render()
	{
		float[] view = camera.getMatrix();
		
		dynamicBoardShader.use();
		dynamicBoardShader.setUniform(0, projection.getMat());
		dynamicBoardShader.setUniform(1, view);
		dynamicBoardShader.setUniform(5, view);
		
		Color c = Color.getHSBColor(color, 1.0f, 1.0f); //Color.white;
		color += 0.01f;
		if(color > 1f)
		{
			color = 0f;
		}
		
		model.identity();
		model.translate(0, -0.5f, 0);
		dynamicBoardShader.setUniform(2, model.getMat());
		dynamicBoardShader.setUniformV2(3, new float[] {10f, 10f});
		dynamicBoardShader.setUniformV4(4, new float[] {(float) c.getRed() / 255f,(float) c.getGreen() / 255f,(float) c.getBlue() / 255f, 1f});
		
		dBoard.draw();
		
		float h = 0.075f + 0.15f + 0.5f;
		
		program.use();
		program.setUniform(0, projection.getMat());
		program.setUniform(1, view);
		program.setUniform(3, view);
		
		model.identity();
		model.translate(0.6f * (float) -1 + 0.15f, h, 0.15f);
		program.setUniform(2, model.getMat());
		inverter.draw();
		
		model.identity();
		model.translate(0.6f * (float) 0 + 0.15f, h, 0.15f);
		program.setUniform(2, model.getMat());
		blotter.draw();
		
		model.identity();
		model.translate(0.6f * (float) 1 + 0.15f, h, 0.15f);
		program.setUniform(2, model.getMat());
		peg.draw();
		
		program.setUniform(2, quaternion.createMatrix());
		inverter.draw();
	}
	
	@Override
	public void newSize(int width, int height)
	{
		projection.perspective(45f, (float) width / (float) height, 0.1f, 100000f);
	}
}

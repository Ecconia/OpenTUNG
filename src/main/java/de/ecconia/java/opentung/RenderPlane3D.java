package de.ecconia.java.opentung;

import de.ecconia.java.opentung.inputs.InputProcessor;
import de.ecconia.java.opentung.libwrap.Matrix;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.TextureWrapper;
import de.ecconia.java.opentung.math.Quaternion;
import de.ecconia.java.opentung.math.Vector3;
import de.ecconia.java.opentung.scomponents.CoordIndicator;
import de.ecconia.java.opentung.scomponents.SimpleBlotterModel;
import de.ecconia.java.opentung.scomponents.SimpleDynamicBoard;
import de.ecconia.java.opentung.scomponents.SimpleInverterModel;
import de.ecconia.java.opentung.scomponents.SimplePeg;

import java.awt.*;

public class RenderPlane3D implements RenderPlane
{
	private Camera camera;
	
	private final Matrix projection = new Matrix();
	
	private ShaderProgram program;
	private SimpleInverterModel inverter;
	private SimpleBlotterModel blotter;
	private SimplePeg peg;
	private CoordIndicator coords;
	
	private TextureWrapper boardTexture;
	private ShaderProgram dynamicBoardShader;
	private SimpleDynamicBoard dBoard;
	
	private final Quaternion quaternion;
	
	private static float color = 0.2f;
	
	private final InputProcessor inputHandler;
	
	public RenderPlane3D(InputProcessor inputHandler, Quaternion q)
	{
		this.inputHandler = inputHandler;
		this.quaternion = q;
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
		coords = new CoordIndicator();
		
		camera = new Camera(inputHandler);
		
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
		
		Matrix model = new Matrix();
		model.identity();
		model.translate(0, -0.5f, 0);
		dynamicBoardShader.setUniform(2, model.getMat());
		dynamicBoardShader.setUniformV2(3, new float[]{10f, 10f});
		dynamicBoardShader.setUniformV4(4, new float[]{(float) c.getRed() / 255f, (float) c.getGreen() / 255f, (float) c.getBlue() / 255f, 1f});
		
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
		
		model.identity();
		model.translate(1.5f, 0, -1.5f);
		program.setUniform(2, model.getMat());
		coords.draw();
		
		placeLayer(0f, view, new StuffConverter()
		{
			@Override
			public float[] eulerToMatrix(float x, float y, float z, Vector3 pos)
			{
				Matrix matrix = new Matrix();
				matrix.identity();
				matrix.translate(pos.getX(), pos.getY(), pos.getZ());
				matrix.rotate(y, 0, 1, 0);
				matrix.rotate(x, 1, 0, 0);
				matrix.rotate(z, 0, 0, 1);
				return matrix.getMat();
			}
		});
		
		placeLayer(1f, view, new StuffConverter()
		{
			@Override
			public float[] eulerToMatrix(float x, float y, float z, Vector3 pos)
			{
				Matrix matrix = new Matrix();
				matrix.translate(pos.getX(), pos.getY(), pos.getZ());
				
				Quaternion q = Quaternion.unityEuler(x, y, z);
				
				matrix.multiply(new Matrix(q.createMatrix()));
				return matrix.getMat();
			}
		});
	}
	
	public void placeLayer(float height, float[] matrix, StuffConverter converter)
	{
		Vector3 initialPosition = new Vector3(2, height, -2);
		
		float[] model = converter.eulerToMatrix(0, 0, 0, initialPosition);
		program.setUniform(2, model);
		inverter.draw();
		
		final float d = 0.9f;
		
		initialPosition = initialPosition.add(d, 0, 0);
		Vector3 copyPos = initialPosition;
		
		model = converter.eulerToMatrix(90, 0, 0, copyPos);
		program.setUniform(2, model);
		inverter.draw();
		
		copyPos = copyPos.add(d, 0, 0);
		
		model = converter.eulerToMatrix(0, 90, 0, copyPos);
		program.setUniform(2, model);
		inverter.draw();
		
		copyPos = copyPos.add(d, 0, 0);
		
		model = converter.eulerToMatrix(0, 0, 90, copyPos);
		program.setUniform(2, model);
		inverter.draw();
		
		initialPosition = initialPosition.add(0, 0, d);
		copyPos = initialPosition;
		
		model = converter.eulerToMatrix(90, 90, 0, copyPos);
		program.setUniform(2, model);
		inverter.draw();
		
		copyPos = copyPos.add(d, 0, 0);
		
		model = converter.eulerToMatrix(90, 0, 90, copyPos);
		program.setUniform(2, model);
		inverter.draw();
		
		copyPos = copyPos.add(d, 0, 0);
		
		model = converter.eulerToMatrix(0, 90, 90, copyPos);
		program.setUniform(2, model);
		inverter.draw();
	}
	
	private interface StuffConverter
	{
		float[] eulerToMatrix(float x, float y, float z, Vector3 pos);
	}
	
	@Override
	public void newSize(int width, int height)
	{
		projection.perspective(45f, (float) width / (float) height, 0.1f, 100000f);
	}
}

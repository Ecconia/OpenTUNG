package de.ecconia.java.opentung;

import de.ecconia.java.opentung.components.CompBlotter;
import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.CompContainer;
import de.ecconia.java.opentung.components.CompGeneric;
import de.ecconia.java.opentung.components.CompInverter;
import de.ecconia.java.opentung.components.CompPeg;
import de.ecconia.java.opentung.components.CompSnappingPeg;
import de.ecconia.java.opentung.components.CompThroughPeg;
import de.ecconia.java.opentung.components.CompWireRaw;
import de.ecconia.java.opentung.inputs.InputProcessor;
import de.ecconia.java.opentung.libwrap.Matrix;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.TextureWrapper;
import de.ecconia.java.opentung.math.Quaternion;
import de.ecconia.java.opentung.math.Vector3;
import de.ecconia.java.opentung.scomponents.CoordIndicator;
import de.ecconia.java.opentung.scomponents.NormalIndicator;
import de.ecconia.java.opentung.scomponents.SimpleBlotterModel;
import de.ecconia.java.opentung.scomponents.SimpleDynamicBoard;
import de.ecconia.java.opentung.scomponents.SimpleDynamicWire;
import de.ecconia.java.opentung.scomponents.SimpleInverterModel;
import de.ecconia.java.opentung.scomponents.SimplePeg;
import de.ecconia.java.opentung.scomponents.SimpleSnappingPeg;
import de.ecconia.java.opentung.scomponents.SimpleThroughPeg;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class RenderPlane3D implements RenderPlane
{
	private Camera camera;
	
	private final Matrix projection = new Matrix();
	
	private ShaderProgram program;
	private SimpleInverterModel inverter;
	private SimpleBlotterModel blotter;
	private SimpleThroughPeg throughPeg;
	private SimplePeg peg;
	private CoordIndicator coords;
	private SimpleSnappingPeg snappingPeg;
	
	private TextureWrapper boardTexture;
	private ShaderProgram dynamicBoardShader;
	private SimpleDynamicBoard dBoard;
	private ShaderProgram wireShader;
	private SimpleDynamicWire dWire;
	
	private ShaderProgram lineShader;
	private NormalIndicator normalIndicator;
	
	private final Quaternion quaternion = Quaternion.xp90.multiply(Quaternion.yp90);
	
	private static float color = 0.2f;
	
	private final InputProcessor inputHandler;
	
	private final List<CompBoard> boardsToRender = new ArrayList<>();
	private final List<CompInverter> invertersToRender = new ArrayList<>();
	private final List<CompPeg> pegsToRender = new ArrayList<>();
	private final List<CompThroughPeg> throughPegsToRender = new ArrayList<>();
	private final List<CompBlotter> blottersToRender = new ArrayList<>();
	private final List<CompSnappingPeg> snappingPegsToRender = new ArrayList<>();
	
	private final List<CompWireRaw> wiresToRender = new ArrayList<>();
	
	public RenderPlane3D(InputProcessor inputHandler, CompBoard board)
	{
		this.inputHandler = inputHandler;
		
		importComponent(board);
	}
	
	private void importComponent(CompGeneric component)
	{
		if(component instanceof CompBoard)
		{
			boardsToRender.add((CompBoard) component);
		}
		else if(component instanceof CompInverter)
		{
			invertersToRender.add((CompInverter) component);
		}
		else if(component instanceof CompPeg)
		{
			pegsToRender.add((CompPeg) component);
		}
		else if(component instanceof CompThroughPeg)
		{
			throughPegsToRender.add((CompThroughPeg) component);
		}
		else if(component instanceof CompBlotter)
		{
			blottersToRender.add((CompBlotter) component);
		}
		else if(component instanceof CompSnappingPeg)
		{
			snappingPegsToRender.add((CompSnappingPeg) component);
		}
		else if(component instanceof CompWireRaw)
		{
			wiresToRender.add((CompWireRaw) component);
		}
		
		if(component instanceof CompContainer)
		{
			for(CompGeneric child : ((CompContainer) component).getChildren())
			{
				importComponent(child);
			}
		}
	}
	
	@Override
	public void setup()
	{
		program = new ShaderProgram("basicShader");
		dynamicBoardShader = new ShaderProgram("dynamicBoardShader");
		lineShader = new ShaderProgram("lineShader");
		wireShader = new ShaderProgram("wireShader");
		
		boardTexture = new TextureWrapper();
		
		inverter = new SimpleInverterModel();
		blotter = new SimpleBlotterModel();
		peg = new SimplePeg();
		dBoard = new SimpleDynamicBoard();
		dWire = new SimpleDynamicWire();
		coords = new CoordIndicator();
		throughPeg = new SimpleThroughPeg();
		snappingPeg = new SimpleSnappingPeg();
		
		normalIndicator = new NormalIndicator();
		
		camera = new Camera(inputHandler);
		
		projection.perspective(45f, (float) 500 / (float) 500, 0.1f, 100000f);
		
		//TODO: Re-add the importing of a tungboard, but at a higher level.
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
		
		for(CompBoard board : boardsToRender)
		{
			model.identity();
			model.translate((float) board.getPosition().getX(), (float) board.getPosition().getY(), (float) board.getPosition().getZ());
			Matrix rotMat = new Matrix(board.getRotation().createMatrix());
			model.multiply(rotMat);
			dynamicBoardShader.setUniform(2, model.getMat());
			dynamicBoardShader.setUniformV2(3, new float[]{board.getX(), board.getZ()});
			dynamicBoardShader.setUniformV4(4, new float[]{(float) board.getColor().getX(), (float) board.getColor().getY(), (float) board.getColor().getZ(), 1f});
			
			dBoard.draw();
		}
		
		wireShader.use();
		wireShader.setUniform(0, projection.getMat());
		wireShader.setUniform(1, view);
		wireShader.setUniform(5, view);
		wireShader.setUniform(2, model.getMat());
		
		for(CompWireRaw wire : wiresToRender)
		{
			model.identity();
			model.translate((float) wire.getPosition().getX(), (float) wire.getPosition().getY(), (float) wire.getPosition().getZ());
			Matrix rotMat = new Matrix(wire.getRotation().createMatrix());
			model.multiply(rotMat);
			wireShader.setUniform(2, model.getMat());
			wireShader.setUniform(3, wire.getLength() / 2f);
			wireShader.setUniform(4, wire.isPowered() ? 1.0f : 0.0f);
			
			this.dWire.draw();
		}
		
		//Normal indicators:
//		lineShader.use();
//		lineShader.setUniform(0, projection.getMat());
//		lineShader.setUniform(1, view);
//		for(Board board : boardsToRender)
//		{
//			model.identity();
//			model.translate((float) board.getPosition().getX(), (float) board.getPosition().getY(), (float) board.getPosition().getZ());
//			Matrix rotMat = new Matrix(board.getRotation().createMatrix());
//			model.multiply(rotMat);
//			lineShader.setUniform(2, model.getMat());
//
//			normalIndicator.draw();
//		}
		
		program.use();
		program.setUniform(0, projection.getMat());
		program.setUniform(1, view);
		program.setUniform(3, view);
		
		for(CompPeg peg : pegsToRender)
		{
			model.identity();
			model.translate((float) peg.getPosition().getX(), (float) peg.getPosition().getY(), (float) peg.getPosition().getZ());
			Matrix rotMat = new Matrix(peg.getRotation().createMatrix());
			model.multiply(rotMat);
			program.setUniform(2, model.getMat());
			
			this.peg.draw();
		}
		
		for(CompPeg peg : pegsToRender)
		{
			model.identity();
			model.translate((float) peg.getPosition().getX(), (float) peg.getPosition().getY(), (float) peg.getPosition().getZ());
			Matrix rotMat = new Matrix(peg.getRotation().createMatrix());
			model.multiply(rotMat);
			program.setUniform(2, model.getMat());
			
			this.peg.draw();
		}
		
		for(CompInverter inverter : invertersToRender)
		{
			model.identity();
			model.translate((float) inverter.getPosition().getX(), (float) inverter.getPosition().getY(), (float) inverter.getPosition().getZ());
			Matrix rotMat = new Matrix(inverter.getRotation().createMatrix());
			model.multiply(rotMat);
			program.setUniform(2, model.getMat());
			
			this.inverter.draw();
		}
		
		for(CompBlotter blotter : blottersToRender)
		{
			model.identity();
			model.translate((float) blotter.getPosition().getX(), (float) blotter.getPosition().getY(), (float) blotter.getPosition().getZ());
			Matrix rotMat = new Matrix(blotter.getRotation().createMatrix());
			model.multiply(rotMat);
			program.setUniform(2, model.getMat());
			
			this.blotter.draw();
		}
		
		for(CompThroughPeg blotter : throughPegsToRender)
		{
			model.identity();
			model.translate((float) blotter.getPosition().getX(), (float) blotter.getPosition().getY(), (float) blotter.getPosition().getZ());
			Matrix rotMat = new Matrix(blotter.getRotation().createMatrix());
			model.multiply(rotMat);
			program.setUniform(2, model.getMat());
			
			this.throughPeg.draw();
		}
		
		for(CompSnappingPeg snappingPeg : snappingPegsToRender)
		{
			model.identity();
			model.translate((float) snappingPeg.getPosition().getX(), (float) snappingPeg.getPosition().getY(), (float) snappingPeg.getPosition().getZ());
			Matrix rotMat = new Matrix(snappingPeg.getRotation().createMatrix());
			model.multiply(rotMat);
			program.setUniform(2, model.getMat());
			
			this.snappingPeg.draw();
		}
		
		//Cross indicators:
//		for(Board board : boardsToRender)
//		{
//			model.identity();
//			model.translate((float) board.getPosition().getX(), (float) board.getPosition().getY(), (float) board.getPosition().getZ());
//			Matrix rotMat = new Matrix(board.getRotation().createMatrix());
//			model.multiply(rotMat);
//			lineShader.setUniform(2, model.getMat());
//
//			coords.draw();
//		}
		
		float h = 0.075f + 0.15f + 0.5f;
		
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
				matrix.translate((float) pos.getX(), (float) pos.getY(), (float) pos.getZ());
				matrix.rotate(y, 0, 1, 0);
				matrix.rotate(x, 1, 0, 0);
				matrix.rotate(z, 0, 0, 1);
				return matrix.getMat();
			}
		});
		
		//Broken euler to Quaternion function, gonna be fixed somewhen later.
//		placeLayer(1f, view, new StuffConverter()
//		{
//			@Override
//			public float[] eulerToMatrix(float x, float y, float z, Vector3 pos)
//			{
//				Matrix matrix = new Matrix();
//				matrix.translate((float) pos.getX(), (float) pos.getY(), (float) pos.getZ());
//
//				Quaternion q = Quaternion.unityEuler(x, y, z);
//
//				matrix.multiply(new Matrix(q.createMatrix()));
//				return matrix.getMat();
//			}
//		});
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

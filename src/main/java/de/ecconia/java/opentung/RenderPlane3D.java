package de.ecconia.java.opentung;

import de.ecconia.java.opentung.components.CompBlotter;
import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.CompContainer;
import de.ecconia.java.opentung.components.CompGeneric;
import de.ecconia.java.opentung.components.CompInverter;
import de.ecconia.java.opentung.components.CompLabel;
import de.ecconia.java.opentung.components.CompPeg;
import de.ecconia.java.opentung.components.CompWireRaw;
import de.ecconia.java.opentung.inputs.InputProcessor;
import de.ecconia.java.opentung.libwrap.Matrix;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.TextureWrapper;
import de.ecconia.java.opentung.math.Quaternion;
import de.ecconia.java.opentung.math.Vector3;
import de.ecconia.java.opentung.models.CoordIndicatorModel;
import de.ecconia.java.opentung.models.NormalIndicatorModel;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class RenderPlane3D implements RenderPlane
{
	private Camera camera;
	
	private final Matrix projection = new Matrix();
	
	private ShaderProgram faceShader;
	private ShaderProgram lineShader;
	private ShaderProgram wireShader;
	private ShaderProgram labelShader;
	private ShaderProgram dynamicBoardShader;
	
	private TextureWrapper boardTexture;
	
	private CoordIndicatorModel coords;
	private NormalIndicatorModel normalIndicator;
	
	private final Quaternion quaternion = Quaternion.xp90.multiply(Quaternion.yp90);
	
	private static float color = 0.2f;
	
	private final InputProcessor inputHandler;
	
	private final List<CompBoard> boardsToRender = new ArrayList<>();
	private final List<CompWireRaw> wiresToRender = new ArrayList<>();
	private final List<CompGeneric> componentsToRender = new ArrayList<>();
	private final List<CompLabel> labelsToRender = new ArrayList<>();
	
	//TODO: Remove this thing again from here. But later when there is more management.
	private final CompBoard board;
	
	public RenderPlane3D(InputProcessor inputHandler, CompBoard board)
	{
		this.board = board;
		this.inputHandler = inputHandler;
	}
	
	private void importComponent(CompGeneric component)
	{
		if(component instanceof CompBoard)
		{
			boardsToRender.add((CompBoard) component);
		}
		else if(component instanceof CompWireRaw)
		{
			wiresToRender.add((CompWireRaw) component);
		}
		else
		{
			componentsToRender.add(component);
		}
		
		if(component instanceof CompLabel)
		{
			((CompLabel) component).initialize();
			labelsToRender.add((CompLabel) component);
			return;
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
		{
			int side = 16;
			BufferedImage image = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = image.createGraphics();
			g.setColor(Color.white);
			g.fillRect(0, 0, side - 1, side - 1);
			g.setColor(new Color(0x777777));
			g.drawRect(0, 0, side - 1, side - 1);
			g.dispose();
			boardTexture = new TextureWrapper(image);
		}
		
		CompGeneric.initModels();
		importComponent(board);
		
		faceShader = new ShaderProgram("basicShader");
		dynamicBoardShader = new ShaderProgram("dynamicBoardShader");
		lineShader = new ShaderProgram("lineShader");
		wireShader = new ShaderProgram("wireShader");
		labelShader = new ShaderProgram("labelShader");
		
		normalIndicator = new NormalIndicatorModel();
		coords = new CoordIndicatorModel();
		
		camera = new Camera(inputHandler);
		
		projection.perspective(45f, (float) 500 / (float) 500, 0.1f, 100000f);
	}
	
	@Override
	public void render()
	{
		float[] view = camera.getMatrix();
		
		boardTexture.activate();
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
		
		CompBoard.model.draw();
		
		for(CompBoard board : boardsToRender)
		{
			model.identity();
			model.translate((float) board.getPosition().getX(), (float) board.getPosition().getY(), (float) board.getPosition().getZ());
			Matrix rotMat = new Matrix(board.getRotation().createMatrix());
			model.multiply(rotMat);
			dynamicBoardShader.setUniform(2, model.getMat());
			dynamicBoardShader.setUniformV2(3, new float[]{board.getX(), board.getZ()});
			dynamicBoardShader.setUniformV4(4, new float[]{(float) board.getColor().getX(), (float) board.getColor().getY(), (float) board.getColor().getZ(), 1f});
			
			CompBoard.model.draw();
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
			
			CompWireRaw.model.draw();
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
		
		labelShader.use();
		labelShader.setUniform(0, projection.getMat());
		labelShader.setUniform(1, view);
		labelShader.setUniform(3, view);
		for(CompLabel label : labelsToRender)
		{
			label.activate();
			model.identity();
			model.translate((float) label.getPosition().getX(), (float) label.getPosition().getY(), (float) label.getPosition().getZ());
			Matrix rotMat = new Matrix(label.getRotation().createMatrix());
			model.multiply(rotMat);
			labelShader.setUniform(2, model.getMat());
			label.drawLabel();
		}
		
		faceShader.use();
		faceShader.setUniform(0, projection.getMat());
		faceShader.setUniform(1, view);
		faceShader.setUniform(3, view);
		
		for(CompGeneric component : componentsToRender)
		{
			model.identity();
			model.translate((float) component.getPosition().getX(), (float) component.getPosition().getY(), (float) component.getPosition().getZ());
			Matrix rotMat = new Matrix(component.getRotation().createMatrix());
			model.multiply(rotMat);
			faceShader.setUniform(2, model.getMat());
			
			component.getModel().draw();
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
		faceShader.setUniform(2, model.getMat());
		CompInverter.model.draw();
		
		model.identity();
		model.translate(0.6f * (float) 0 + 0.15f, h, 0.15f);
		faceShader.setUniform(2, model.getMat());
		CompBlotter.model.draw();
		
		model.identity();
		model.translate(0.6f * (float) 1 + 0.15f, h, 0.15f);
		faceShader.setUniform(2, model.getMat());
		CompPeg.model.draw();
		
		faceShader.setUniform(2, quaternion.createMatrix());
		CompInverter.model.draw();
		
		model.identity();
		model.translate(1.5f, 0, -1.5f);
		faceShader.setUniform(2, model.getMat());
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
		faceShader.setUniform(2, model);
		CompInverter.model.draw();
		
		final float d = 0.9f;
		
		initialPosition = initialPosition.add(d, 0, 0);
		Vector3 copyPos = initialPosition;
		
		model = converter.eulerToMatrix(90, 0, 0, copyPos);
		faceShader.setUniform(2, model);
		CompInverter.model.draw();
		
		copyPos = copyPos.add(d, 0, 0);
		
		model = converter.eulerToMatrix(0, 90, 0, copyPos);
		faceShader.setUniform(2, model);
		CompInverter.model.draw();
		
		copyPos = copyPos.add(d, 0, 0);
		
		model = converter.eulerToMatrix(0, 0, 90, copyPos);
		faceShader.setUniform(2, model);
		CompInverter.model.draw();
		
		initialPosition = initialPosition.add(0, 0, d);
		copyPos = initialPosition;
		
		model = converter.eulerToMatrix(90, 90, 0, copyPos);
		faceShader.setUniform(2, model);
		CompInverter.model.draw();
		
		copyPos = copyPos.add(d, 0, 0);
		
		model = converter.eulerToMatrix(90, 0, 90, copyPos);
		faceShader.setUniform(2, model);
		CompInverter.model.draw();
		
		copyPos = copyPos.add(d, 0, 0);
		
		model = converter.eulerToMatrix(0, 90, 90, copyPos);
		faceShader.setUniform(2, model);
		CompInverter.model.draw();
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

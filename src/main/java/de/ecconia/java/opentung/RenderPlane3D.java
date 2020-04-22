package de.ecconia.java.opentung;

import de.ecconia.java.opentung.components.Board;
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
import de.ecconia.java.opentung.scomponents.SimpleInverterModel;
import de.ecconia.java.opentung.scomponents.SimplePeg;
import de.ecconia.java.opentung.tungboard.PrimitiveParser;
import de.ecconia.java.opentung.tungboard.tungobjects.TungBoard;
import de.ecconia.java.opentung.tungboard.tungobjects.common.TungChildable;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungAngles;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungColor;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungObject;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungPosition;
import java.awt.Color;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
	
	private ShaderProgram lineShader;
	private NormalIndicator normalIndicator;
	
	private final Quaternion quaternion = Quaternion.xp90.multiply(Quaternion.yp90);
	
	private static float color = 0.2f;
	
	private final InputProcessor inputHandler;
	
	private final List<Board> boardsToRender = new ArrayList<>();
	
	public RenderPlane3D(InputProcessor inputHandler)
	{
		this.inputHandler = inputHandler;
	}
	
	@Override
	public void setup()
	{
		program = new ShaderProgram("basicShader");
		dynamicBoardShader = new ShaderProgram("dynamicBoardShader");
		lineShader = new ShaderProgram("lineShader");
		
		boardTexture = new TextureWrapper();
		
		inverter = new SimpleInverterModel();
		blotter = new SimpleBlotterModel();
		peg = new SimplePeg();
		dBoard = new SimpleDynamicBoard();
		coords = new CoordIndicator();
		
		normalIndicator = new NormalIndicator();
		
		camera = new Camera(inputHandler);
		
		projection.perspective(45f, (float) 500 / (float) 500, 0.1f, 100000f);
		
		//Load some tungboard:
		try
		{
			TungBoard importedBoard = PrimitiveParser.importTungBoard("boards/16Bit-Paralell-CLA-ALU.tungboard");
			importedBoard.setPosition(new TungPosition(0, 0, 0));
			importedBoard.setAngles(new TungAngles(180, 0, 0));
			importChild(importedBoard, new Vector3(-20, 0, 0), Quaternion.angleAxis(0, Vector3.yp), 0, "└─", true);
		}
		catch(RuntimeException e)
		{
			if(e.getCause() != null && e.getCause() instanceof NoSuchFileException)
			{
				System.out.println("###########################################");
				System.out.println("Couldn't find tungboard file to display, you can download a nice one here: https://discordapp.com/channels/401255675264761866/588822987331993602/684761768144142337");
				System.out.println("But for gods sake, rename 'CLE' to 'Paralell-CLA'");
				System.out.println("Once you inserted a tungboard, or that one. Change the filename in " + getClass().getName() + ".");
				System.out.println("###########################################");
			}
			else
			{
				throw e;
			}
		}
	}
	
	private void importChild(TungObject object, Vector3 parentPosition, Quaternion parentRotation, int level, String prefix, boolean last)
	{
		if(object instanceof TungBoard)
		{
			TungBoard tungBoard = (TungBoard) object;
			System.out.println(prefix + " " + tungBoard.getX() + " " + tungBoard.getZ());
			prefix = prefix.substring(0, prefix.length() - 2);
			prefix += last ? "  " : "│ ";
			
			System.out.println(prefix + "Rot: " + tungBoard.getAngles());
			System.out.println(prefix + "Pos: " + tungBoard.getPosition());
			System.out.println(prefix + "Abs: " + parentPosition);
			
			//TODO: Now that it works, optimize. Make it one non-obsolete calculation method.
			Quaternion qx = Quaternion.angleAxis((double) tungBoard.getAngles().getX(), Vector3.xn); //Has to be negative, cause unity *shrug*
			Quaternion qy = Quaternion.angleAxis((double) -tungBoard.getAngles().getY(), Vector3.yp);
			Quaternion qz = Quaternion.angleAxis((double) -tungBoard.getAngles().getZ(), Vector3.zp);
			Quaternion localRotation = qz.multiply(qx).multiply(qy);
			Quaternion globalRotation = localRotation.multiply(parentRotation);
			
			Vector3 fixPoint = localRotation.inverse().multiply(new Vector3(0.15f * (float) tungBoard.getX(), 0, 0.15f * (float) tungBoard.getZ()));
			Vector3 rotatedFixPoint = parentRotation.inverse().multiply(fixPoint);
			
			Vector3 localPosition = new Vector3(tungBoard.getPosition().getX(), tungBoard.getPosition().getY(), tungBoard.getPosition().getZ());
			Vector3 rotatedPosition = parentRotation.inverse().multiply(localPosition);
			Vector3 globalPosition = parentPosition.add(rotatedPosition);
			
			Board board = new Board(tungBoard.getX(), tungBoard.getZ());
			board.setColor(new Vector3(tungBoard.getColor().getR(), tungBoard.getColor().getG(), tungBoard.getColor().getB()));
			board.setPosition(globalPosition.add(rotatedFixPoint));
			board.setRotation(globalRotation);
			boardsToRender.add(board);
			
			List<TungObject> children = tungBoard.getChildren();
			//For the sake of pretty debugging, get rid of all unrelevant entries.
			Iterator<TungObject> filterIterator = children.iterator();
			while(filterIterator.hasNext())
			{
				if(!(filterIterator.next() instanceof TungChildable))
				{
					filterIterator.remove();
				}
			}
			
			for(int i = 0; i < children.size(); i++)
			{
				TungObject child = children.get(i);
				boolean newLast = i == (children.size() - 1);
				String newPrefix = prefix + (newLast ? "└─" : "├─");
				importChild(child, globalPosition, globalRotation, level, newPrefix, newLast);
			}
		}
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
		
		for(Board board : boardsToRender)
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
		
		float h = 0.075f + 0.15f + 0.5f;
		
		program.use();
		program.setUniform(0, projection.getMat());
		program.setUniform(1, view);
		program.setUniform(3, view);
		
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

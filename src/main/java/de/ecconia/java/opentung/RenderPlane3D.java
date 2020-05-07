package de.ecconia.java.opentung;

import de.ecconia.java.opentung.components.CompBlotter;
import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.CompCrossyIndicator;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.CompInverter;
import de.ecconia.java.opentung.components.CompLabel;
import de.ecconia.java.opentung.components.CompPeg;
import de.ecconia.java.opentung.components.CompWireRaw;
import de.ecconia.java.opentung.components.meta.ComponentLibrary;
import de.ecconia.java.opentung.inputs.InputProcessor;
import de.ecconia.java.opentung.libwrap.Matrix;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.TextureWrapper;
import de.ecconia.java.opentung.models.CoordIndicatorModel;
import de.ecconia.java.opentung.models.DebugBlockModel;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.opengl.GL30;

public class RenderPlane3D implements RenderPlane
{
	private Camera camera;
	private long lastCycle;
	
	private final Matrix projection = new Matrix();
	
	private ShaderProgram faceShader;
	private ShaderProgram lineShader;
	private ShaderProgram faceOutlineShader;
	private ShaderProgram wireShader;
	private ShaderProgram labelShader;
	private ShaderProgram dynamicBoardShader;
	
	private TextureWrapper boardTexture;
	
	private CoordIndicatorModel coords;
	private DebugBlockModel block;
	
	private static float color = 0.2f;
	
	private final InputProcessor inputHandler;
	
	private final List<CompBoard> boardsToRender = new ArrayList<>();
	private final List<CompWireRaw> wiresToRender = new ArrayList<>();
	private final List<Component> componentsToRender = new ArrayList<>();
	private final List<CompLabel> labelsToRender = new ArrayList<>();
	private final List<CompCrossyIndicator> wireEndsToRender = new ArrayList<>();
	
	//TODO: Remove this thing again from here. But later when there is more management.
	private final CompBoard board;
	
	public RenderPlane3D(InputProcessor inputHandler, CompBoard board)
	{
		this.board = board;
		this.inputHandler = inputHandler;
	}
	
	private void importComponent(Component component)
	{
		if(component instanceof CompBoard)
		{
			boardsToRender.add((CompBoard) component);
		}
		else if(component instanceof CompWireRaw)
		{
			CompWireRaw wire = (CompWireRaw) component;
			wiresToRender.add(wire);
			
			wireEndsToRender.add(new CompCrossyIndicator(wire.getEnd1()));
			wireEndsToRender.add(new CompCrossyIndicator(wire.getEnd2()));
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
			for(Component child : ((CompContainer) component).getChildren())
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
		
		ComponentLibrary.initGL();
		importComponent(board);
		
		faceShader = new ShaderProgram("basicShader");
		lineShader = new ShaderProgram("lineShader");
		faceOutlineShader = new ShaderProgram("basicShaderOutline");
		dynamicBoardShader = new ShaderProgram("dynamicBoardShader");
		wireShader = new ShaderProgram("wireShader");
		labelShader = new ShaderProgram("labelShader");
		
		coords = new CoordIndicatorModel();
		block = new DebugBlockModel();
		
		camera = new Camera(inputHandler);
		
		projection.perspective(45f, (float) 500 / (float) 500, 0.1f, 100000f);
		
		lastCycle = System.currentTimeMillis();
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
		
		long current = System.currentTimeMillis();
		long timePast = current - lastCycle;
		lastCycle = current;
		Color c = Color.getHSBColor(color, 1.0f, 1.0f); //Color.white;
		color += 0.008f / 60f * (float) timePast;
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
		
		CompBoard.modelHolder.draw();
		
		for(CompBoard board : boardsToRender)
		{
			model.identity();
			model.translate((float) board.getPosition().getX(), (float) board.getPosition().getY(), (float) board.getPosition().getZ());
			Matrix rotMat = new Matrix(board.getRotation().createMatrix());
			model.multiply(rotMat);
			dynamicBoardShader.setUniform(2, model.getMat());
			dynamicBoardShader.setUniformV2(3, new float[]{board.getX(), board.getZ()});
			dynamicBoardShader.setUniformV4(4, new float[]{(float) board.getColor().getX(), (float) board.getColor().getY(), (float) board.getColor().getZ(), 1f});
			
			CompBoard.modelHolder.draw();
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
			
			CompWireRaw.modelHolder.draw();
		}
		
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
			label.getModelHolder().drawTextures();
		}
		
		lineShader.use();
		lineShader.setUniform(0, projection.getMat());
		lineShader.setUniform(1, view);
		
		for(Component component : wireEndsToRender)
		{
			model.identity();
			model.translate((float) component.getPosition().getX(), (float) component.getPosition().getY(), (float) component.getPosition().getZ());
			Matrix rotMat = new Matrix(component.getRotation().createMatrix());
			model.multiply(rotMat);
			lineShader.setUniform(2, model.getMat());
			
			component.getModelHolder().draw();
		}
		
		faceShader.use();
		faceShader.setUniform(0, projection.getMat());
		faceShader.setUniform(1, view);
		faceShader.setUniform(3, view);
		
		for(Component component : componentsToRender)
		{
			model.identity();
			model.translate((float) component.getPosition().getX(), (float) component.getPosition().getY(), (float) component.getPosition().getZ());
			Matrix rotMat = new Matrix(component.getRotation().createMatrix());
			model.multiply(rotMat);
			faceShader.setUniform(2, model.getMat());
			
			component.getModelHolder().draw();
		}
		
		float h = 0.075f + 0.15f + 0.5f;
		
		model.identity();
		model.translate(0.6f * (float) -1 + 0.15f, h, 0.15f);
		faceShader.setUniform(2, model.getMat());
		CompInverter.modelHolder.draw();
		
		model.identity();
		model.translate(0.6f * (float) 0 + 0.15f, h, 0.15f);
		faceShader.setUniform(2, model.getMat());
		CompBlotter.modelHolder.draw();
		
		model.identity();
		model.translate(0.6f * (float) 1 + 0.15f, h, 0.15f);
		faceShader.setUniform(2, model.getMat());
		CompPeg.modelHolder.draw();
		
		model.identity();
		model.translate(1.5f, 0, -1.5f);
		faceShader.setUniform(2, model.getMat());
		coords.draw();
		
		//Outline test:
		GL30.glStencilFunc(GL30.GL_ALWAYS, 1, 0xFF);
		GL30.glStencilMask(0xFF);
		
		model.identity();
		faceShader.setUniform(2, model.getMat());
		block.draw();
		
		GL30.glStencilFunc(GL30.GL_NOTEQUAL, 1, 0xFF);
		GL30.glStencilMask(0x00);
		GL30.glDisable(GL30.GL_DEPTH_TEST);
		
		faceOutlineShader.use();
		faceOutlineShader.setUniform(0, projection.getMat());
		faceOutlineShader.setUniform(1, view);
		
		float scale = 1.2f;
		Matrix sMat = new Matrix();
		sMat.scale(scale, scale, scale);
		model.multiply(sMat);
		faceOutlineShader.setUniform(2, model.getMat());
		block.draw();
		
		GL30.glEnable(GL30.GL_DEPTH_TEST);
	}
	
	@Override
	public void newSize(int width, int height)
	{
		projection.perspective(45f, (float) width / (float) height, 0.1f, 100000f);
	}
}

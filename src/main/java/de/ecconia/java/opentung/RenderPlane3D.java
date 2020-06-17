package de.ecconia.java.opentung;

import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.CompCrossyIndicator;
import de.ecconia.java.opentung.components.CompLabel;
import de.ecconia.java.opentung.components.CompWireRaw;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ComponentLibrary;
import de.ecconia.java.opentung.inputs.InputProcessor;
import de.ecconia.java.opentung.libwrap.Matrix;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.TextureWrapper;
import de.ecconia.java.opentung.libwrap.meshes.ConductorMesh;
import de.ecconia.java.opentung.libwrap.meshes.RayCastMesh;
import de.ecconia.java.opentung.libwrap.meshes.SolidMesh;
import de.ecconia.java.opentung.libwrap.meshes.TextureMesh;
import de.ecconia.java.opentung.models.CoordIndicatorModel;
import de.ecconia.java.opentung.models.DebugBlockModel;
import de.ecconia.java.opentung.tungboard.TungBoardLoader;
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
	
	private ShaderProgram faceShader;
	private ShaderProgram lineShader;
	private ShaderProgram outlineComponentShader;
	private ShaderProgram wireShader;
	private ShaderProgram labelShader;
	private ShaderProgram dynamicBoardShader;
	private ShaderProgram raycastComponentShader;
	private ShaderProgram raycastBoardShader;
	private ShaderProgram raycastWireShader;
	private ShaderProgram outlineWireShader;
	private ShaderProgram outlineBoardShader;
	
	private TextureWrapper boardTexture;
	
	private CoordIndicatorModel coords;
	private DebugBlockModel block;
	
	private final InputProcessor inputHandler;
	
	private TextureMesh textureMesh;
	private RayCastMesh rayCastMesh;
	private SolidMesh solidMesh;
	private ConductorMesh conductorMesh;
	
	private final List<CompBoard> boardsToRender = new ArrayList<>();
	private final List<CompWireRaw> wiresToRender = new ArrayList<>();
	private final List<Component> componentsToRender = new ArrayList<>();
	private final List<CompLabel> labelsToRender = new ArrayList<>();
	private final List<CompCrossyIndicator> wireEndsToRender = new ArrayList<>();
	
	//TODO: Remove this thing again from here. But later when there is more management.
	private final CompBoard board;
	
	private Component[] idLookup;
	private int currentlySelectedIndex = 0;
	private int width = 0;
	private int height = 0;
	
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

//			wireEndsToRender.add(new CompCrossyIndicator(wire.getEnd1()));
//			wireEndsToRender.add(new CompCrossyIndicator(wire.getEnd2()));
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
		
		System.out.println("Broken wires rendered: " + TungBoardLoader.brokenWires.size());
		if(!TungBoardLoader.brokenWires.isEmpty())
		{
			wiresToRender.clear();
			wiresToRender.addAll(TungBoardLoader.brokenWires); //Debuggy
			for(CompWireRaw wire : TungBoardLoader.brokenWires)
			{
				//TODO: Highlight which exactly failed (Or just remove this whole section, rip)
				wireEndsToRender.add(new CompCrossyIndicator(wire.getEnd1()));
				wireEndsToRender.add(new CompCrossyIndicator(wire.getEnd2()));
			}
		}
		
		{
			int amount = boardsToRender.size() + wiresToRender.size() + componentsToRender.size() + 1;
			System.out.println("Raycast ID amount: " + amount);
			if((amount) > 0xFFFFFF)
			{
				throw new RuntimeException("Out of raycast IDs. Tell the dev to do fancy programming, so that this never happens again.");
			}
			idLookup = new Component[amount];
			
			int id = 1;
			for(Component comp : boardsToRender)
			{
				comp.setRayCastID(id);
				idLookup[id] = comp;
				id++;
			}
			for(Component comp : wiresToRender)
			{
				comp.setRayCastID(id);
				idLookup[id] = comp;
				id++;
			}
			for(Component comp : componentsToRender)
			{
				comp.setRayCastID(id);
				idLookup[id] = comp;
				id++;
			}
		}
		
		faceShader = new ShaderProgram("basicShader");
		lineShader = new ShaderProgram("lineShader");
		dynamicBoardShader = new ShaderProgram("dynamicBoardShader");
		wireShader = new ShaderProgram("wireShader");
		labelShader = new ShaderProgram("labelShader");
		
		raycastComponentShader = new ShaderProgram("raycast/raycastComponent");
		raycastBoardShader = new ShaderProgram("raycast/raycastBoard");
		raycastWireShader = new ShaderProgram("raycast/raycastWire");
		
		outlineComponentShader = new ShaderProgram("outline/outlineComponent");
		outlineWireShader = new ShaderProgram("outline/outlineWire");
		outlineBoardShader = new ShaderProgram("outline/outlineBoard");
		
		coords = new CoordIndicatorModel();
		block = new DebugBlockModel();
		
		camera = new Camera(inputHandler);
		
		//Create meshes:
		System.out.println("Starting mesh generation...");
		textureMesh = new TextureMesh(boardTexture, boardsToRender);
		rayCastMesh = new RayCastMesh(boardsToRender, wiresToRender, componentsToRender);
		solidMesh = new SolidMesh(componentsToRender);
		conductorMesh = new ConductorMesh(componentsToRender, wiresToRender);
		System.out.println("Done.");
		
		lastCycle = System.currentTimeMillis();
	}
	
	@Override
	public void render()
	{
		float[] view = camera.getMatrix();
		raycast(view);
		drawDynamic(view);
	}
	
	private void drawDynamic(float[] view)
	{
		OpenTUNG.setBackgroundColor();
		OpenTUNG.clear();
		
		Matrix model = new Matrix();
		
		textureMesh.draw(view);
		conductorMesh.draw(view);
		solidMesh.draw(view);
		
		labelShader.use();
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
		
		//Draw selected component:
		
		if(currentlySelectedIndex != 0)
		{
			Component component = idLookup[currentlySelectedIndex];
			
			GL30.glStencilFunc(GL30.GL_ALWAYS, 1, 0xFF);
			GL30.glStencilMask(0xFF);
			
			if(component instanceof CompBoard)
			{
				boardTexture.activate();
				dynamicBoardShader.use();
				
				CompBoard board = (CompBoard) component;
				
				model.identity();
				model.translate((float) board.getPosition().getX(), (float) board.getPosition().getY(), (float) board.getPosition().getZ());
				Matrix rotMat = new Matrix(board.getRotation().createMatrix());
				model.multiply(rotMat);
				dynamicBoardShader.setUniform(2, model.getMat());
				dynamicBoardShader.setUniformV2(3, new float[]{board.getX(), board.getZ()});
				dynamicBoardShader.setUniformV4(4, new float[]{(float) board.getColor().getX(), (float) board.getColor().getY(), (float) board.getColor().getZ(), 1f});
				
				CompBoard.modelHolder.draw();
			}
			else if(component instanceof CompWireRaw)
			{
				wireShader.use();
				CompWireRaw wire = (CompWireRaw) component;
				
				model.identity();
				model.translate((float) wire.getPosition().getX(), (float) wire.getPosition().getY(), (float) wire.getPosition().getZ());
				Matrix rotMat = new Matrix(wire.getRotation().createMatrix());
				model.multiply(rotMat);
				wireShader.setUniform(2, model.getMat());
				wireShader.setUniform(3, wire.getLength() / 2f);
				wireShader.setUniform(4, wire.isPowered() ? 1.0f : 0.0f);
				
				CompWireRaw.modelHolder.draw();
			}
			else
			{
				faceShader.use();
				model.identity();
				model.translate((float) component.getPosition().getX(), (float) component.getPosition().getY(), (float) component.getPosition().getZ());
				Matrix rotMat = new Matrix(component.getRotation().createMatrix());
				model.multiply(rotMat);
				faceShader.setUniform(2, model.getMat());
				component.getModelHolder().draw();
			}
			
			GL30.glStencilFunc(GL30.GL_NOTEQUAL, 1, 0xFF);
			GL30.glStencilMask(0x00);
			GL30.glDisable(GL30.GL_DEPTH_TEST);
			
			float scale = 1.1f;
			Matrix sMat = new Matrix();
			sMat.scale(scale, scale, scale);
			model.multiply(sMat);
			
			if(component instanceof CompBoard)
			{
				outlineBoardShader.use();
				outlineBoardShader.setUniform(1, view);
				
				CompBoard board = (CompBoard) component;
				
				outlineBoardShader.setUniform(2, model.getMat());
				outlineBoardShader.setUniformV2(3, new float[]{board.getX(), board.getZ()});
				
				CompBoard.modelHolder.draw();
			}
			else if(component instanceof CompWireRaw)
			{
				outlineWireShader.use();
				outlineWireShader.setUniform(1, view);
				CompWireRaw wire = (CompWireRaw) component;
				
				outlineWireShader.setUniform(2, model.getMat());
				outlineWireShader.setUniform(3, wire.getLength() / 2f);
				
				CompWireRaw.modelHolder.draw();
			}
			else
			{
				outlineComponentShader.use();
				outlineComponentShader.setUniform(1, view);
				
				outlineComponentShader.setUniform(2, model.getMat());
				component.getModelHolder().draw();
			}
			
			GL30.glStencilFunc(GL30.GL_NOTEQUAL, 1, 0xFF);
			GL30.glEnable(GL30.GL_DEPTH_TEST);
		}
	}
	
	private void raycast(float[] view)
	{
		Matrix model = new Matrix();
		
		GL30.glClearColor(0, 0, 0, 1);
		OpenTUNG.clear();
		
		rayCastMesh.draw(view);
		
		GL30.glFlush();
		GL30.glFinish();
		GL30.glPixelStorei(GL30.GL_UNPACK_ALIGNMENT, 1);
		
		float[] values = new float[3];
		GL30.glReadPixels(width / 2, height / 2, 1, 1, GL30.GL_RGB, GL30.GL_FLOAT, values);
//		float[] distance = new float[1];
//		GL30.glReadPixels(width / 2, height / 2, 1, 1, GL30.GL_DEPTH_COMPONENT, GL30.GL_FLOAT, distance);
		
		int id = (int) (values[0] * 255f) + (int) (values[1] * 255f) * 256 + (int) (values[2] * 255f) * 256 * 256;
		if(id > idLookup.length - 1)
		{
			System.out.println("Looking at ???? (" + id + ")");
			id = 0;
		}
		
		currentlySelectedIndex = id;
	}
	
	@Override
	public void newSize(int width, int height)
	{
		this.width = width;
		this.height = height;
		Matrix p = new Matrix();
		p.perspective(45f, (float) width / (float) height, 0.1f, 100000f);
		float[] projection = p.getMat();
		
		rayCastMesh.updateProjection(projection);
		solidMesh.updateProjection(projection);
		conductorMesh.updateProjection(projection);
		
		textureMesh.updateProjection(projection);
		faceShader.use();
		faceShader.setUniform(0, projection);
		lineShader.use();
		lineShader.setUniform(0, projection);
		outlineComponentShader.use();
		outlineComponentShader.setUniform(0, projection);
		wireShader.use();
		wireShader.setUniform(0, projection);
		labelShader.use();
		labelShader.setUniform(0, projection);
		dynamicBoardShader.use();
		dynamicBoardShader.setUniform(0, projection);
		raycastComponentShader.use();
		raycastComponentShader.setUniform(0, projection);
		raycastBoardShader.use();
		raycastBoardShader.setUniform(0, projection);
		raycastWireShader.use();
		raycastWireShader.setUniform(0, projection);
		outlineWireShader.use();
		outlineWireShader.setUniform(0, projection);
		outlineBoardShader.use();
		outlineBoardShader.setUniform(0, projection);
	}
}

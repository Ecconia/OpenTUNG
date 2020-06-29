package de.ecconia.java.opentung;

import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.CompCrossyIndicator;
import de.ecconia.java.opentung.components.CompLabel;
import de.ecconia.java.opentung.components.CompPeg;
import de.ecconia.java.opentung.components.CompSnappingPeg;
import de.ecconia.java.opentung.components.CompThroughPeg;
import de.ecconia.java.opentung.components.conductor.CompWireRaw;
import de.ecconia.java.opentung.components.conductor.Connector;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ComponentLibrary;
import de.ecconia.java.opentung.components.meta.Holdable;
import de.ecconia.java.opentung.inputs.InputProcessor;
import de.ecconia.java.opentung.libwrap.Matrix;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.TextureWrapper;
import de.ecconia.java.opentung.libwrap.meshes.ColorMesh;
import de.ecconia.java.opentung.libwrap.meshes.ConductorMesh;
import de.ecconia.java.opentung.libwrap.meshes.RayCastMesh;
import de.ecconia.java.opentung.libwrap.meshes.SolidMesh;
import de.ecconia.java.opentung.libwrap.meshes.TextureMesh;
import de.ecconia.java.opentung.libwrap.vaos.InYaFaceVAO;
import de.ecconia.java.opentung.libwrap.vaos.SimpleCubeVAO;
import de.ecconia.java.opentung.math.Vector3;
import de.ecconia.java.opentung.models.CoordIndicatorModel;
import de.ecconia.java.opentung.models.DebugBlockModel;
import de.ecconia.java.opentung.simulation.Cluster;
import de.ecconia.java.opentung.simulation.HiddenWire;
import de.ecconia.java.opentung.simulation.Wire;
import de.ecconia.java.opentung.tungboard.TungBoardLoader;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.lwjgl.opengl.GL30;

public class RenderPlane3D implements RenderPlane, Camera.RightClickReceiver
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
	private ShaderProgram inYaFace;
	private InYaFaceVAO inYaFaceVAO;
	private ShaderProgram justShape;
	private SimpleCubeVAO cubeVAO;
	private TextureWrapper boardTexture;
	
	private CoordIndicatorModel coords;
	private DebugBlockModel block;
	
	private final InputProcessor inputHandler;
	
	private TextureMesh textureMesh;
	private RayCastMesh rayCastMesh;
	private SolidMesh solidMesh;
	private ConductorMesh conductorMesh;
	private ColorMesh colorMesh;
	
	private final List<CompCrossyIndicator> wireEndsToRender = new ArrayList<>();
	
	//TODO: Remove this thing again from here. But later when there is more management.
	private final BoardUniverse board;
	
	private Component[] idLookup;
	private int currentlySelectedIndex = 0;
	private Cluster clusterToHighlight;
	private Set<Connector> connectorsToHighlight = new HashSet<>();
	private int width = 0;
	private int height = 0;
	
	public RenderPlane3D(InputProcessor inputHandler, BoardUniverse board)
	{
		this.board = board;
		this.inputHandler = inputHandler;
	}
	
	//Mouse handling:
	
	private Component downer;
	private long downTime;
	private boolean down;
	private Holdable tempDowner;
	
	@Override
	public void rightUp()
	{
		if(currentlySelectedIndex != 0)
		{
			Component downer = idLookup[currentlySelectedIndex];
			long time = (System.currentTimeMillis() - downTime);
			//If the click was longer than a second, validate that its the intended component...
			if(time > Settings.longMousePressDuration)
			{
				if(this.downer == downer)
				{
					downer.rightClicked(board.getSimulation());
					componentClicked(downer);
				}
			}
			else
			{
				downer.rightClicked(board.getSimulation());
				componentClicked(downer);
			}
		}
		downTime = 0;
	}
	
	private void componentClicked(Component component)
	{
		//TODO: Move this somewhere more generic.
		Cluster cluster = null;
		if(component instanceof CompWireRaw)
		{
			cluster = ((CompWireRaw) component).getCluster();
		}
		else if(component instanceof CompThroughPeg || component instanceof CompPeg || component instanceof CompSnappingPeg)
		{
			cluster = component.getPegs().get(0).getCluster();
		}
		if(cluster != null)
		{
			if(clusterToHighlight == cluster)
			{
				clusterToHighlight = null;
				connectorsToHighlight = new HashSet<>();
			}
			else
			{
				clusterToHighlight = cluster;
				connectorsToHighlight = new HashSet<>();
				for(Wire wire : cluster.getWires())
				{
					connectorsToHighlight.add(wire.getConnectorA());
					connectorsToHighlight.add(wire.getConnectorB());
				}
			}
		}
	}
	
	@Override
	public void rightDown()
	{
		downTime = System.currentTimeMillis();
		if(currentlySelectedIndex != 0)
		{
			downer = idLookup[currentlySelectedIndex];
		}
		else
		{
			downer = null;
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
		{
			//Import section:
			for(CompLabel label : board.getLabelsToRender())
			{
				label.initialize();
			}
//			for(CompWireRaw wire : board.getWiresToRender())
//			{
//				wireEndsToRender.add(new CompCrossyIndicator(wire.getEnd1()));
//				wireEndsToRender.add(new CompCrossyIndicator(wire.getEnd2()));
//			}
		}
		
		System.out.println("Broken wires rendered: " + TungBoardLoader.brokenWires.size());
		if(!TungBoardLoader.brokenWires.isEmpty())
		{
			board.getWiresToRender().clear();
			board.getWiresToRender().addAll(TungBoardLoader.brokenWires); //Debuggy
			for(CompWireRaw wire : TungBoardLoader.brokenWires)
			{
				//TODO: Highlight which exactly failed (Or just remove this whole section, rip)
				wireEndsToRender.add(new CompCrossyIndicator(wire.getEnd1()));
				wireEndsToRender.add(new CompCrossyIndicator(wire.getEnd2()));
			}
		}
		
		{
			int amount = board.getBoardsToRender().size() + board.getWiresToRender().size() + board.getComponentsToRender().size() + 1;
			System.out.println("Raycast ID amount: " + amount);
			if((amount) > 0xFFFFFF)
			{
				throw new RuntimeException("Out of raycast IDs. Tell the dev to do fancy programming, so that this never happens again.");
			}
			idLookup = new Component[amount];
			
			int id = 1;
			for(Component comp : board.getBoardsToRender())
			{
				comp.setRayCastID(id);
				idLookup[id] = comp;
				id++;
			}
			for(Component comp : board.getWiresToRender())
			{
				comp.setRayCastID(id);
				idLookup[id] = comp;
				id++;
			}
			for(Component comp : board.getComponentsToRender())
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
		inYaFace = new ShaderProgram("outline/inYaFacePlane");
		inYaFaceVAO = InYaFaceVAO.generateInYaFacePlane();
		justShape = new ShaderProgram("justShape");
		cubeVAO = SimpleCubeVAO.generateCube();
		
		coords = new CoordIndicatorModel();
		block = new DebugBlockModel();
		
		camera = new Camera(inputHandler, this);
		
		//Create meshes:
		{
			System.out.println("Starting mesh generation...");
			textureMesh = new TextureMesh(boardTexture, board.getBoardsToRender());
			rayCastMesh = new RayCastMesh(board.getBoardsToRender(), board.getWiresToRender(), board.getComponentsToRender());
			solidMesh = new SolidMesh(board.getComponentsToRender());
			conductorMesh = new ConductorMesh(board.getComponentsToRender(), board.getWiresToRender(), board.getClusters(), board.getSimulation());
			colorMesh = new ColorMesh(board.getComponentsToRender(), board.getSimulation());
			System.out.println("Done.");
		}
		
		board.getSimulation().start();
		lastCycle = System.currentTimeMillis();
	}
	
	private void checkMouseInteraction()
	{
		if(downTime != 0)
		{
			if(currentlySelectedIndex != 0)
			{
				Component comp = idLookup[currentlySelectedIndex];
				if(comp instanceof Holdable)
				{
					Holdable currentlyHold = (Holdable) comp;
					if(currentlyHold != tempDowner)
					{
						if(tempDowner != null)
						{
							//If mouse over something else.
							tempDowner.setHold(false, board.getSimulation());
						}
						//If something new is hold:
						tempDowner = currentlyHold;
						tempDowner.setHold(true, board.getSimulation());
					}
				}
				else
				{
					if(tempDowner != null)
					{
						//If mouse over something non-holdable.
						tempDowner.setHold(false, board.getSimulation());
						tempDowner = null;
					}
				}
			}
			else
			{
				if(tempDowner != null)
				{
					//If mouse no longer over a component.
					tempDowner.setHold(false, board.getSimulation());
					tempDowner = null;
				}
			}
		}
		else if(tempDowner != null)
		{
			//If mouse has been lifted.
			tempDowner.setHold(false, board.getSimulation());
			tempDowner = null;
		}
	}
	
	@Override
	public void render()
	{
		checkMouseInteraction();
		
		float[] view = camera.getMatrix();
		if(Settings.doRaycasting)
		{
			raycast(view);
		}
		if(Settings.drawWorld)
		{
			drawDynamic(view);
		}
		highlightCluster(view);
		drawHighlight(view);
		
		if(Settings.drawComponentPositionIndicator)
		{
			lineShader.use();
			lineShader.setUniform(1, view);
			Matrix model = new Matrix();
			for(Component comp : board.getComponentsToRender())
			{
				model.identity();
				model.translate((float) comp.getPosition().getX(), (float) comp.getPosition().getY(), (float) comp.getPosition().getZ());
				labelShader.setUniform(2, model.getMat());
				CompCrossyIndicator.modelHolder.draw();
			}
		}
	}
	
	private void drawDynamic(float[] view)
	{
		OpenTUNG.setBackgroundColor();
		OpenTUNG.clear();
		
		Matrix model = new Matrix();
		
		if(Settings.drawBoards)
		{
			textureMesh.draw(view);
		}
		conductorMesh.draw(view);
		if(Settings.drawMaterial)
		{
			solidMesh.draw(view);
		}
		colorMesh.draw(view);
		
		labelShader.use();
		labelShader.setUniform(1, view);
		labelShader.setUniform(3, view);
		for(CompLabel label : board.getLabelsToRender())
		{
			label.activate();
			model.identity();
			model.translate((float) label.getPosition().getX(), (float) label.getPosition().getY(), (float) label.getPosition().getZ());
			Matrix rotMat = new Matrix(label.getRotation().createMatrix());
			model.multiply(rotMat);
			labelShader.setUniform(2, model.getMat());
			label.getModelHolder().drawTextures();
		}
		
		if(!wireEndsToRender.isEmpty())
		{
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
		}
	}
	
	private void drawHighlight(float[] view)
	{
		if(currentlySelectedIndex == 0)
		{
			return;
		}
		
		Component component = idLookup[currentlySelectedIndex];
		
		boolean isBoard = component instanceof CompBoard;
		boolean isWire = component instanceof CompWireRaw;
		if(
				isBoard && !Settings.highlightBoards
						|| isWire && !Settings.highlightWires
						|| !(isBoard || isWire) && !Settings.highlightComponents
		)
		{
			return;
		}
		
		//Enable drawing to stencil buffer
		GL30.glStencilMask(0xFF);
		
		drawStencilComponent(component, view);
		
		//Draw on top
		GL30.glDisable(GL30.GL_DEPTH_TEST);
		//Only draw if stencil bit is set.
		GL30.glStencilFunc(GL30.GL_EQUAL, 1, 0xFF);
		
		float[] color = new float[]{
				Settings.highlightColorR,
				Settings.highlightColorG,
				Settings.highlightColorB,
				Settings.highlightColorA
		};
		
		inYaFace.use();
		inYaFace.setUniformV4(0, color);
		inYaFaceVAO.use();
		inYaFaceVAO.draw();
		
		//Restore settings:
		GL30.glStencilFunc(GL30.GL_NOTEQUAL, 1, 0xFF);
		GL30.glEnable(GL30.GL_DEPTH_TEST);
		//Clear stencil buffer:
		GL30.glClear(GL30.GL_STENCIL_BUFFER_BIT);
		//After clearing, disable usage/writing of/to stencil buffer again.
		GL30.glStencilMask(0x00);
	}
	
	private void drawStencilComponent(Component component, float[] view)
	{
		float[] color = new float[]{0, 0, 0, 0};
		
		Matrix model = new Matrix();
		if(component instanceof CompBoard)
		{
			outlineBoardShader.use();
			outlineBoardShader.setUniform(1, view);
			
			CompBoard board = (CompBoard) component;
			
			model.translate((float) board.getPosition().getX(), (float) board.getPosition().getY(), (float) board.getPosition().getZ());
			Matrix rotMat = new Matrix(board.getRotation().createMatrix());
			model.multiply(rotMat);
			
			outlineBoardShader.setUniform(2, model.getMat());
			outlineBoardShader.setUniformV2(3, new float[]{board.getX(), board.getZ()});
			outlineBoardShader.setUniformV4(4, color);
			
			CompBoard.modelHolder.draw();
		}
		else if(component instanceof CompWireRaw)
		{
			outlineWireShader.use();
			outlineWireShader.setUniform(1, view);
			CompWireRaw wire = (CompWireRaw) component;
			
			model.identity();
			model.translate((float) wire.getPosition().getX(), (float) wire.getPosition().getY(), (float) wire.getPosition().getZ());
			Matrix rotMat = new Matrix(wire.getRotation().createMatrix());
			model.multiply(rotMat);
			
			outlineWireShader.setUniform(2, model.getMat());
			outlineWireShader.setUniform(3, wire.getLength() / 2f);
			outlineBoardShader.setUniformV4(4, color);
			
			CompWireRaw.modelHolder.draw();
		}
		else
		{
			outlineComponentShader.use();
			outlineComponentShader.setUniform(1, view);
			
			model.identity();
			model.translate((float) component.getPosition().getX(), (float) component.getPosition().getY(), (float) component.getPosition().getZ());
			Matrix rotMat = new Matrix(component.getRotation().createMatrix());
			model.multiply(rotMat);
			
			outlineComponentShader.setUniform(2, model.getMat());
			outlineBoardShader.setUniformV4(3, color);
			component.getModelHolder().draw();
		}
	}
	
	private void highlightCluster(float[] view)
	{
		if(clusterToHighlight == null)
		{
			return;
		}
		
		//Enable drawing to stencil buffer
		GL30.glStencilMask(0xFF);
		
		for(Wire wire : clusterToHighlight.getWires())
		{
			if(wire instanceof HiddenWire)
			{
				continue;
			}
			drawStencilComponent((CompWireRaw) wire, view);
		}
		justShape.use();
		justShape.setUniform(1, view);
		justShape.setUniformV4(3, new float[] {0,0,0,0});
		Matrix matrix = new Matrix();
		for(Connector connector : connectorsToHighlight)
		{
			CubeFull cube = connector.getModel();
			
			matrix.identity();
			Vector3 position = connector.getBase().getPosition();
			matrix.translate((float) position.getX(), (float) position.getY(), (float) position.getZ());
			Matrix rotMat = new Matrix(connector.getBase().getRotation().createMatrix());
			matrix.multiply(rotMat);
			Vector3 size = cube.getSize();
			position = cube.getPosition().add(connector.getBase().getModelHolder().getPlacementOffset());
			matrix.translate((float) position.getX(), (float) position.getY(), (float) position.getZ());
			matrix.scale((float) size.getX(), (float) size.getY(), (float) size.getZ());
			justShape.setUniform(2, matrix.getMat());
			
			cubeVAO.use();
			cubeVAO.draw();
		}
		
		//Draw on top
		GL30.glDisable(GL30.GL_DEPTH_TEST);
		//Only draw if stencil bit is set.
		GL30.glStencilFunc(GL30.GL_EQUAL, 1, 0xFF);
		
		float[] color = new float[]{
				Settings.highlightClusterColorR,
				Settings.highlightClusterColorG,
				Settings.highlightClusterColorB,
				Settings.highlightClusterColorA
		};
		
		inYaFace.use();
		inYaFace.setUniformV4(0, color);
		inYaFaceVAO.use();
		inYaFaceVAO.draw();
		
		//Restore settings:
		GL30.glStencilFunc(GL30.GL_NOTEQUAL, 1, 0xFF);
		GL30.glEnable(GL30.GL_DEPTH_TEST);
		//Clear stencil buffer:
		GL30.glClear(GL30.GL_STENCIL_BUFFER_BIT);
		//After clearing, disable usage/writing of/to stencil buffer again.
		GL30.glStencilMask(0x00);
	}
	
	private void raycast(float[] view)
	{
		Matrix model = new Matrix();
		
		if(Settings.drawWorld)
		{
			GL30.glViewport(0, 0, 1, 1);
		}
		GL30.glClearColor(0, 0, 0, 1);
		OpenTUNG.clear();
		
		rayCastMesh.draw(view);
		
		GL30.glFlush();
		GL30.glFinish();
		GL30.glPixelStorei(GL30.GL_UNPACK_ALIGNMENT, 1);
		
		float[] values = new float[3];
		GL30.glReadPixels(0, 0, 1, 1, GL30.GL_RGB, GL30.GL_FLOAT, values);
//		float[] distance = new float[1];
//		GL30.glReadPixels(width / 2, height / 2, 1, 1, GL30.GL_DEPTH_COMPONENT, GL30.GL_FLOAT, distance);
		
		int id = (int) (values[0] * 255f) + (int) (values[1] * 255f) * 256 + (int) (values[2] * 255f) * 256 * 256;
		if(id > idLookup.length - 1)
		{
			System.out.println("Looking at ???? (" + id + ")");
			id = 0;
		}
		
		if(Settings.drawWorld)
		{
			GL30.glViewport(0, 0, this.width, this.height);
		}
		
		currentlySelectedIndex = id;
	}
	
	@Override
	public void newSize(int width, int height)
	{
		this.width = width;
		this.height = height;
		Matrix p = new Matrix();
		p.perspective(Settings.fov, (float) width / (float) height, 0.1f, 100000f);
		float[] projection = p.getMat();
		
		rayCastMesh.updateProjection(projection);
		solidMesh.updateProjection(projection);
		conductorMesh.updateProjection(projection);
		colorMesh.updateProjection(projection);
		
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
		justShape.use();
		justShape.setUniform(0, projection);
	}
}

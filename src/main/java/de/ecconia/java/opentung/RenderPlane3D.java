package de.ecconia.java.opentung;

import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.CompDisplay;
import de.ecconia.java.opentung.components.CompLabel;
import de.ecconia.java.opentung.components.CompPanelDisplay;
import de.ecconia.java.opentung.components.CompPanelLabel;
import de.ecconia.java.opentung.components.CompPeg;
import de.ecconia.java.opentung.components.CompSnappingPeg;
import de.ecconia.java.opentung.components.CompThroughPeg;
import de.ecconia.java.opentung.components.conductor.Blot;
import de.ecconia.java.opentung.components.conductor.CompWireRaw;
import de.ecconia.java.opentung.components.conductor.Connector;
import de.ecconia.java.opentung.components.conductor.Peg;
import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.meta.Colorable;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.Holdable;
import de.ecconia.java.opentung.components.meta.Part;
import de.ecconia.java.opentung.inputs.Controller3D;
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
import de.ecconia.java.opentung.libwrap.vaos.LineVAO;
import de.ecconia.java.opentung.libwrap.vaos.SimpleCubeVAO;
import de.ecconia.java.opentung.libwrap.vaos.VisualShapeVAO;
import de.ecconia.java.opentung.math.MathHelper;
import de.ecconia.java.opentung.math.Quaternion;
import de.ecconia.java.opentung.math.Vector3;
import de.ecconia.java.opentung.simulation.Cluster;
import de.ecconia.java.opentung.simulation.HiddenWire;
import de.ecconia.java.opentung.simulation.InheritingCluster;
import de.ecconia.java.opentung.simulation.SourceCluster;
import de.ecconia.java.opentung.simulation.Updateable;
import de.ecconia.java.opentung.simulation.Wire;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.lwjgl.opengl.GL30;

public class RenderPlane3D implements RenderPlane
{
	private Camera camera;
	private long lastCycle;
	
	private ShaderProgram lineShader;
	private LineVAO crossyIndicator;
	private LineVAO axisIndicator;
	private ShaderProgram justShape;
	private SimpleCubeVAO cubeVAO;
	private ShaderProgram visualShapeShader;
	private VisualShapeVAO visualShape;
	private ShaderProgram inYaFace;
	private InYaFaceVAO inYaFaceVAO;
	private ShaderProgram sdfShader;
	
	private TextureWrapper boardTexture;
	
	private final InputProcessor inputHandler;
	
	private TextureMesh textureMesh;
	private RayCastMesh rayCastMesh;
	private SolidMesh solidMesh;
	private ConductorMesh conductorMesh;
	private ColorMesh colorMesh;
	
	private final List<Vector3> wireEndsToRender = new ArrayList<>();
	private final LabelToolkit labelToolkit = new LabelToolkit();
	private final BlockingQueue<GPUTask> gpuTasks = new LinkedBlockingQueue<>();
	
	//TODO: Remove this thing again from here. But later when there is more management.
	private final BoardUniverse board;
	
	private Part[] idLookup;
	private int currentlySelectedIndex = 0;
	private Cluster clusterToHighlight;
	private List<Connector> connectorsToHighlight = new ArrayList<>();
	private int width = 0;
	private int height = 0;
	private float[] latestProjectionMat;
	
	public RenderPlane3D(InputProcessor inputHandler, BoardUniverse board)
	{
		this.board = board;
		board.startFinalizeImport(gpuTasks);
		this.inputHandler = inputHandler;
	}
	
	//Other:
	
	private Vector3 placementPosition;
	private Vector3 placementNormal;
	private CompBoard placementBoard;
	private int rayID = 1;
	
	//Input handling:
	
	private Controller3D controller;
	private Connector wireStartPoint; //Selected by dragging from a connector.
	private PlaceableInfo currentPlaceable; //Selected via the 0..9 keys or mouse-wheel.
	private int placementRotation;
	
	public Part getCursorObject()
	{
		if(currentlySelectedIndex <= 0)
		{
			return null;
		}
		return idLookup[currentlySelectedIndex];
	}
	
	//Click events:
	
	public void componentLeftClicked(Part part)
	{
		part.leftClicked(board.getSimulation());
	}
	
	public void componentLeftHold(Holdable holdable)
	{
		holdable.setHold(true, board.getSimulation());
	}
	
	public void componentLeftUnHold(Holdable holdable)
	{
		holdable.setHold(false, board.getSimulation());
	}
	
	public void componentRightClicked(Part part)
	{
		//TODO: Move this somewhere more generic.
		Cluster cluster = null;
		if(part instanceof CompWireRaw)
		{
			cluster = ((CompWireRaw) part).getCluster();
		}
		else if(part instanceof CompThroughPeg || part instanceof CompPeg || part instanceof CompSnappingPeg)
		{
			cluster = ((Component) part).getPegs().get(0).getCluster();
		}
		else if(part instanceof Connector)
		{
			cluster = ((Connector) part).getCluster();
		}
		
		if(cluster != null)
		{
			if(clusterToHighlight == cluster)
			{
				clusterToHighlight = null;
				connectorsToHighlight = new ArrayList<>();
			}
			else
			{
				clusterToHighlight = cluster;
				connectorsToHighlight = cluster.getConnectors();
			}
		}
	}
	
	public void rightDragOnConnector(Connector connector)
	{
		wireStartPoint = connector;
	}
	
	public void rightDragOnConnectorStop(Connector connector)
	{
		Connector from = wireStartPoint;
		wireStartPoint = null;
		if(connector != null)
		{
			Connector to = connector;
			
			boolean fromBlot = from instanceof Blot;
			boolean toBlot = to instanceof Blot;
			if(fromBlot && toBlot)
			{
				System.out.println("Blot-Blot connections not allowed.");
				return;
			}
			
			for(Wire wire : from.getWires())
			{
				if(wire.getOtherSide(from) == to)
				{
					System.out.println("Already connected.");
					return;
				}
			}
			
			Cluster wireCluster;
			
			if(fromBlot != toBlot)
			{
				//Inheriting
				Connector blotConnector = fromBlot ? from : to;
				Connector pegConnector = fromBlot ? to : from;
				
				wireCluster = blotConnector.getCluster();
			}
			else // Peg + Peg
			{
				//Merging
				wireCluster = from.getCluster();
			}
			
			//Add wire:
			{
				CompWireRaw newWire = new CompWireRaw(null); //TODO: What is the parent?
				newWire.setConnectorA(from);
				newWire.setConnectorB(to);
				from.addWire(newWire);
				to.addWire(newWire);
				newWire.setCluster(wireCluster);
				
				Vector3 fromPos = from.getConnectionPoint();
				Vector3 toPos = to.getConnectionPoint();
				
				//Pos + Rot
				Vector3 direction = fromPos.subtract(toPos).divide(2);
				double distance = direction.length();
				Quaternion rotation = MathHelper.rotationFromVectors(Vector3.zp, direction.normalize());
				Vector3 position = toPos.add(direction);
				newWire.setRotation(rotation);
				newWire.setPosition(position);
				newWire.setLength((float) distance * 2f);
				
				//RayCast
				Part[] idLookupClone = new Part[idLookup.length + 1];
				System.arraycopy(idLookup, 0, idLookupClone, 0, idLookup.length);
				idLookup = idLookupClone;
				newWire.setRayCastID(rayID);
				idLookup[rayID] = newWire;
				rayID++;
				
				//Add it
				board.getWiresToRender().add(newWire);
			}
			
			try
			{
				gpuTasks.put((ignored) -> {
					refreshWireMeshes();
				});
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void componentPlaceSelection(PlaceableInfo placeable)
	{
		currentPlaceable = placeable;
	}
	
	public void rotatePlacement(int degrees)
	{
		placementRotation += degrees;
		if(placementRotation >= 360)
		{
			placementRotation -= 360;
		}
	}
	
	public boolean attemptPlacement()
	{
		//TODO: Ugly, not thread-safe enough for my taste. Might even cause bugs. So eventually it has to be changed.
		if(placementPosition != null && currentPlaceable != null)
		{
			Component newComponent = currentPlaceable.instance(placementBoard);
			{
				int newIDsAmount = 1 + newComponent.getPegs().size() + newComponent.getBlots().size();
				Part[] idLookupClone = new Part[idLookup.length + newIDsAmount];
				System.arraycopy(idLookup, 0, idLookupClone, 0, idLookup.length);
				idLookup = idLookupClone;
				
				newComponent.setRayCastID(rayID);
				idLookup[rayID] = newComponent;
				rayID++;
				for(Peg peg : newComponent.getPegs())
				{
					peg.setRayCastID(rayID);
					idLookup[rayID] = peg;
					rayID++;
				}
				for(Blot blot : newComponent.getBlots())
				{
					blot.setRayCastID(rayID);
					idLookup[rayID] = blot;
					rayID++;
				}
			}
			newComponent.setPosition(placementPosition);
			Quaternion rotation = Quaternion.angleAxis(placementRotation, Vector3.yn);
			Quaternion compRotation = MathHelper.rotationFromVectors(Vector3.yp, placementNormal);
			newComponent.setRotation(compRotation.multiply(rotation));
			
			//TODO: Update bounds and stuff
			board.getComponentsToRender().add(newComponent);
			placementBoard.addChild(newComponent);
			
			for(Peg peg : newComponent.getPegs())
			{
				Cluster cluster = new InheritingCluster(board.getNewClusterID());
				cluster.addConnector(peg);
				peg.setCluster(cluster);
			}
			for(Blot blot : newComponent.getBlots())
			{
				Cluster cluster = new SourceCluster(board.getNewClusterID(), blot);
				cluster.addConnector(blot);
				blot.setCluster(cluster);
			}
			
			boolean hasColorable = newComponent instanceof Colorable;
			if(hasColorable)
			{
				//TODO: Replace section with initDefault()
				if(newComponent instanceof CompDisplay)
				{
					((CompDisplay) newComponent).setColorRaw(Color.displayYellow);
				}
				else if(newComponent instanceof CompPanelDisplay)
				{
					((CompPanelDisplay) newComponent).setColorRaw(Color.displayYellow);
				}
				else
				{
					throw new RuntimeException("Unknown colorable component: " + newComponent.getClass().getSimpleName());
				}
			}
			
			if(newComponent instanceof Updateable)
			{
				board.getSimulation().updateNextTickThreadSafe((Updateable) newComponent);
			}
			
			try
			{
				gpuTasks.put((ignored) -> {
					refreshComponentMeshes(hasColorable);
				});
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
			return true;
		}
		
		return false;
	}
	
	//Setup and stuff:
	
	@Override
	public void setup()
	{
		{
			int side = 16;
			BufferedImage image = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = image.createGraphics();
			g.setColor(java.awt.Color.white);
			g.fillRect(0, 0, side - 1, side - 1);
			g.setColor(new java.awt.Color(0x777777));
			g.drawRect(0, 0, side - 1, side - 1);
			g.dispose();
			boardTexture = new TextureWrapper(image);
		}
		
		//TODO: Currently manually triggered, but to be optimized away.
		CompLabel.initGL();
		CompPanelLabel.initGL();
		System.out.println("Starting label generation.");
		labelToolkit.startProcessing(gpuTasks, board.getLabelsToRender());
		
		System.out.println("Broken wires rendered: " + board.getBrokenWires().size());
		if(!board.getBrokenWires().isEmpty())
		{
			board.getWiresToRender().clear();
			board.getWiresToRender().addAll(board.getBrokenWires()); //Debuggy
			for(CompWireRaw wire : board.getBrokenWires())
			{
				//TODO: Highlight which exactly failed (Or just remove this whole section, rip)
				wireEndsToRender.add(wire.getEnd1());
				wireEndsToRender.add(wire.getEnd2());
			}
		}
		
		{
			int amount = board.getBoardsToRender().size() + board.getWiresToRender().size() + 1;
			for(Component component : board.getComponentsToRender())
			{
				amount += 1 + component.getPegs().size() + component.getBlots().size();
			}
			System.out.println("Raycast ID amount: " + amount);
			if((amount) > 0xFFFFFF)
			{
				throw new RuntimeException("Out of raycast IDs. Tell the dev to do fancy programming, so that this never happens again.");
			}
			idLookup = new Part[amount];
			
			for(Component comp : board.getBoardsToRender())
			{
				comp.setRayCastID(rayID);
				idLookup[rayID] = comp;
				rayID++;
			}
			for(Component comp : board.getWiresToRender())
			{
				comp.setRayCastID(rayID);
				idLookup[rayID] = comp;
				rayID++;
			}
			for(Component comp : board.getComponentsToRender())
			{
				comp.setRayCastID(rayID);
				idLookup[rayID] = comp;
				rayID++;
				for(Peg peg : comp.getPegs())
				{
					peg.setRayCastID(rayID);
					idLookup[rayID] = peg;
					rayID++;
				}
				for(Blot blot : comp.getBlots())
				{
					blot.setRayCastID(rayID);
					idLookup[rayID] = blot;
					rayID++;
				}
			}
		}
		
		lineShader = new ShaderProgram("lineShader");
		crossyIndicator = LineVAO.generateCrossyIndicator();
		axisIndicator = LineVAO.generateAxisIndicator();
		justShape = new ShaderProgram("justShape");
		cubeVAO = SimpleCubeVAO.generateCube();
		visualShapeShader = new ShaderProgram("visualShape");
		visualShape = VisualShapeVAO.generateCube();
		
		inYaFace = new ShaderProgram("outline/inYaFacePlane");
		inYaFaceVAO = InYaFaceVAO.generateInYaFacePlane();
		sdfShader = new ShaderProgram("sdfLabel");
		
		camera = new Camera();
		//Do not start receiving events before here. Be sure the whole thing is properly setted up.
		controller = new Controller3D(this);
		inputHandler.setController(controller);
		
		//Create meshes:
		{
			System.out.println("Starting mesh generation...");
			textureMesh = new TextureMesh(boardTexture, board.getBoardsToRender());
			rayCastMesh = new RayCastMesh(board.getBoardsToRender(), board.getWiresToRender(), board.getComponentsToRender());
			solidMesh = new SolidMesh(board.getComponentsToRender());
			conductorMesh = new ConductorMesh(board.getComponentsToRender(), board.getWiresToRender(), board.getSimulation(), true);
			colorMesh = new ColorMesh(board.getComponentsToRender(), board.getSimulation());
			System.out.println("Done.");
		}
		
		System.out.println("Label amount: " + board.getLabelsToRender().size());
		System.out.println("Wire amount: " + board.getWiresToRender().size());
		lastCycle = System.currentTimeMillis();
	}
	
	public void refreshPostWorldLoad()
	{
		System.out.println("Update:");
		conductorMesh.update(board.getComponentsToRender(), board.getWiresToRender());
		for(Cluster cluster : board.getClusters())
		{
			cluster.updateState(board.getSimulation());
		}
		board.getSimulation().start();
		System.out.println("Done.");
	}
	
	public void refreshComponentMeshes(boolean hasColorable)
	{
		System.out.println("Update:");
		conductorMesh.update(board.getComponentsToRender(), board.getWiresToRender());
		solidMesh.update(board.getComponentsToRender());
		rayCastMesh.update(board.getBoardsToRender(), board.getWiresToRender(), board.getComponentsToRender());
		if(hasColorable)
		{
			colorMesh.update(board.getComponentsToRender());
		}
		System.out.println("Done.");
	}
	
	public void refreshWireMeshes()
	{
		System.out.println("Update:");
		conductorMesh.update(board.getComponentsToRender(), board.getWiresToRender());
		rayCastMesh.update(board.getBoardsToRender(), board.getWiresToRender(), board.getComponentsToRender());
		System.out.println("Done.");
	}
	
	public void calculatePlacementPosition()
	{
		if(currentlySelectedIndex == 0)
		{
			placementPosition = null;
			return;
		}
		
		Part part = idLookup[currentlySelectedIndex];
		
		if(!(part instanceof CompBoard))
		{
			placementPosition = null; //Only place on boards.
			return;
		}
		
		CompBoard board = (CompBoard) part;
		
		CubeFull shape = (CubeFull) board.getModelHolder().getSolid().get(0);
		Vector3 position = board.getPosition();
		Quaternion rotation = board.getRotation();
		Vector3 size = shape.getSize();
		if(shape.getMapper() != null)
		{
			size = shape.getMapper().getMappedSize(size, board);
		}
		
		Vector3 cameraPosition = camera.getPosition();
		
		Vector3 cameraRay = Vector3.zp;
		cameraRay = Quaternion.angleAxis(camera.getNeck(), Vector3.xn).multiply(cameraRay);
		cameraRay = Quaternion.angleAxis(camera.getRotation(), Vector3.yn).multiply(cameraRay);
		Vector3 cameraRayBoardSpace = rotation.multiply(cameraRay);
		
		Vector3 cameraPositionBoardSpace = rotation.multiply(cameraPosition.subtract(position)); //Convert the camera position, in the board space.
		
		double distanceLocalMin = (size.getX() - cameraPositionBoardSpace.getX()) / cameraRayBoardSpace.getX();
		double distanceLocalMax = ((-size.getX()) - cameraPositionBoardSpace.getX()) / cameraRayBoardSpace.getX();
		double distanceGlobal;
		Vector3 normalGlobal;
		if(distanceLocalMin < distanceLocalMax)
		{
			distanceGlobal = distanceLocalMin;
			normalGlobal = Vector3.xp;
		}
		else
		{
			distanceGlobal = distanceLocalMax;
			normalGlobal = Vector3.xn;
		}
		
		distanceLocalMin = (size.getY() - cameraPositionBoardSpace.getY()) / cameraRayBoardSpace.getY();
		distanceLocalMax = ((-size.getY()) - cameraPositionBoardSpace.getY()) / cameraRayBoardSpace.getY();
		double distanceLocal;
		Vector3 normalLocal;
		if(distanceLocalMin < distanceLocalMax)
		{
			distanceLocal = distanceLocalMin;
			normalLocal = Vector3.yp;
		}
		else
		{
			distanceLocal = distanceLocalMax;
			normalLocal = Vector3.yn;
		}
		if(distanceGlobal < distanceLocal)
		{
			distanceGlobal = distanceLocal;
			normalGlobal = normalLocal;
		}
		
		distanceLocalMin = (size.getZ() - cameraPositionBoardSpace.getZ()) / cameraRayBoardSpace.getZ();
		distanceLocalMax = ((-size.getZ()) - cameraPositionBoardSpace.getZ()) / cameraRayBoardSpace.getZ();
		if(distanceLocalMin < distanceLocalMax)
		{
			distanceLocal = distanceLocalMin;
			normalLocal = Vector3.zp;
		}
		else
		{
			distanceLocal = distanceLocalMax;
			normalLocal = Vector3.zn;
		}
		if(distanceGlobal < distanceLocal)
		{
			distanceGlobal = distanceLocal;
			normalGlobal = normalLocal;
		}
		
		boolean isSide = normalGlobal.getY() == 0;
		int sign = normalGlobal.oneNegative() ? -1 : 1;
		Vector3 collisionPointBoardSpace = cameraPositionBoardSpace.add(cameraRayBoardSpace.multiply(distanceGlobal));
		if(isSide)
		{
			double x = collisionPointBoardSpace.getX();
			double z = collisionPointBoardSpace.getZ();
			if(normalGlobal.getX() == 0)
			{
				double xHalf = board.getX() * 0.15;
				double xcp = x + xHalf;
				int xSteps = (int) (xcp / 0.3);
				x = (xSteps) * 0.3 - xHalf + 0.15;
				z -= sign * 0.075;
			}
			else
			{
				double zHalf = board.getZ() * 0.15;
				double zcp = z + zHalf;
				int zSteps = (int) (zcp / 0.3);
				z = zSteps * 0.3 - zHalf + 0.15;
				x -= sign * 0.075;
			}
			
			collisionPointBoardSpace = new Vector3(x, 0, z);
		}
		else
		{
			double xHalf = board.getX() * 0.15;
			double zHalf = board.getZ() * 0.15;
			
			double xcp = collisionPointBoardSpace.getX() + xHalf;
			double zcp = collisionPointBoardSpace.getZ() + zHalf;
			
			int xSteps = (int) (xcp / 0.3);
			int zSteps = (int) (zcp / 0.3);
			
			collisionPointBoardSpace = new Vector3(xSteps * 0.3 + 0.15 - xHalf, 0, zSteps * 0.3 + 0.15 - zHalf);
		}
		
		placementPosition = board.getRotation().inverse().multiply(collisionPointBoardSpace).add(board.getPosition());
		placementNormal = board.getRotation().inverse().multiply(normalGlobal).normalize(); //Safety normalization.
		placementBoard = board;
	}
	
	@Override
	public void render()
	{
		calculatePlacementPosition();
		
		while(!gpuTasks.isEmpty())
		{
			gpuTasks.poll().execute(this);
		}
		
		camera.lockLocation();
		controller.doFrameCycle();
		
		float[] view = camera.getMatrix();
		if(Settings.doRaycasting)
		{
			raycast(view);
		}
		if(Settings.drawWorld)
		{
			drawDynamic(view);
			drawPlacementPosition(view); //Must be called before drawWireToBePlaced, currently!!!
			highlightCluster(view);
			drawWireToBePlaced(view);
			drawHighlight(view);
			
			lineShader.use();
			lineShader.setUniform(1, view);
			Matrix model = new Matrix();
			if(Settings.drawComponentPositionIndicator)
			{
				for(Component comp : board.getComponentsToRender())
				{
					model.identity();
					model.translate((float) comp.getPosition().getX(), (float) comp.getPosition().getY(), (float) comp.getPosition().getZ());
					lineShader.setUniform(2, model.getMat());
					crossyIndicator.use();
					crossyIndicator.draw();
				}
			}
			if(Settings.drawWorldAxisIndicator)
			{
				model.identity();
				Vector3 position = new Vector3(0, 10, 0);
				model.translate((float) position.getX(), (float) position.getY(), (float) position.getZ());
				lineShader.setUniform(2, model.getMat());
				axisIndicator.use();
				axisIndicator.draw();
			}
		}
	}
	
	private void drawWireToBePlaced(float[] view)
	{
		if(wireStartPoint == null)
		{
			return;
		}
		
		Vector3 startingPos = wireStartPoint.getConnectionPoint();
		
		Vector3 toPos = placementPosition;
		if(toPos == null)
		{
			Part currentlyLookingAt = getCursorObject();
			if(currentlyLookingAt instanceof Connector)
			{
				toPos = ((Connector) currentlyLookingAt).getConnectionPoint();
			}
		}
		else
		{
			//Fix offset.
			toPos = toPos.add(placementNormal.multiply(0.075));
		}
		
		if(toPos != null)
		{
			//Draw wire between placementPosition and startingPos:
			Vector3 direction = toPos.subtract(startingPos).divide(2);
			double distance = direction.length();
			Quaternion rotation = MathHelper.rotationFromVectors(Vector3.zp, direction.normalize());
			
			Matrix model = new Matrix();
			Vector3 position = startingPos.add(direction);
			model.translate((float) position.getX(), (float) position.getY(), (float) position.getZ());
			model.multiply(new Matrix(rotation.createMatrix()));
			Vector3 size = new Vector3(0.025, 0.01, distance);
			model.scale((float) size.getX(), (float) size.getY(), (float) size.getZ());
			
			justShape.use();
			justShape.setUniform(1, view);
			justShape.setUniform(2, model.getMat());
			justShape.setUniformV4(3, new float[] {1.0f, 0.0f, 1.0f, 1.0f});
			
			cubeVAO.use();
			cubeVAO.draw();
		}
	}
	
	private void drawPlacementPosition(float[] view)
	{
		if(placementPosition == null)
		{
			return;
		}
		
		if(currentPlaceable == null)
		{
			lineShader.use();
			lineShader.setUniform(1, view);
			GL30.glLineWidth(5f);
			Matrix model = new Matrix();
			model.identity();
			Vector3 datPos = placementPosition.add(placementNormal.multiply(0.075));
			model.translate((float) datPos.getX(), (float) datPos.getY(), (float) datPos.getZ());
			lineShader.setUniform(2, model.getMat());
			crossyIndicator.use();
			crossyIndicator.draw();
		}
		else
		{
			Quaternion modelRotation = Quaternion.angleAxis(placementRotation, Vector3.yn);
			Quaternion compRotation = MathHelper.rotationFromVectors(Vector3.yp, placementNormal);
			World3DHelper.drawModel(visualShapeShader, visualShape, currentPlaceable.getModel(), placementPosition, modelRotation.multiply(compRotation), view);
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
		
		sdfShader.use();
		sdfShader.setUniform(1, view);
		for(CompLabel label : board.getLabelsToRender())
		{
			label.activate();
			model.identity();
			model.translate((float) label.getPosition().getX(), (float) label.getPosition().getY(), (float) label.getPosition().getZ());
			Matrix rotMat = new Matrix(label.getRotation().createMatrix());
			model.multiply(rotMat);
			sdfShader.setUniform(2, model.getMat());
			label.getModelHolder().drawTextures();
		}
		
		if(!wireEndsToRender.isEmpty())
		{
			lineShader.use();
			lineShader.setUniform(1, view);
			
			for(Vector3 position : wireEndsToRender)
			{
				model.identity();
				model.translate((float) position.getX(), (float) position.getY(), (float) position.getZ());
				lineShader.setUniform(2, model.getMat());
				crossyIndicator.use();
				crossyIndicator.draw();
			}
		}
	}
	
	private void drawHighlight(float[] view)
	{
		if(currentlySelectedIndex == 0)
		{
			return;
		}
		
		Part part = idLookup[currentlySelectedIndex];
		
		boolean isBoard = part instanceof CompBoard;
		boolean isWire = part instanceof CompWireRaw;
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
		
		if(part instanceof Component)
		{
			World3DHelper.drawStencilComponent(justShape, cubeVAO, (Component) part, view);
		}
		else //Connector
		{
			justShape.use();
			justShape.setUniform(1, view);
			justShape.setUniformV4(3, new float[]{0, 0, 0, 0});
			Matrix matrix = new Matrix();
			World3DHelper.drawCubeFull(justShape, cubeVAO, ((Connector) part).getModel(), part, part.getParent().getModelHolder().getPlacementOffset(), new Matrix());
		}
		
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
			World3DHelper.drawStencilComponent(justShape, cubeVAO, (CompWireRaw) wire, view);
		}
		justShape.use();
		justShape.setUniform(1, view);
		justShape.setUniformV4(3, new float[]{0, 0, 0, 0});
		Matrix matrix = new Matrix();
		for(Connector connector : connectorsToHighlight)
		{
			World3DHelper.drawCubeFull(justShape, cubeVAO, connector.getModel(), connector.getParent(), connector.getParent().getModelHolder().getPlacementOffset(), matrix);
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
		latestProjectionMat = projection;
		
		rayCastMesh.updateProjection(projection);
		solidMesh.updateProjection(projection);
		conductorMesh.updateProjection(projection);
		colorMesh.updateProjection(projection);
		textureMesh.updateProjection(projection);
		
		visualShapeShader.use();
		visualShapeShader.setUniform(0, projection);
		sdfShader.use();
		sdfShader.setUniform(0, projection);
		lineShader.use();
		lineShader.setUniform(0, projection);
		justShape.use();
		justShape.setUniform(0, projection);
	}
	
	public Camera getCamera()
	{
		return camera;
	}
}

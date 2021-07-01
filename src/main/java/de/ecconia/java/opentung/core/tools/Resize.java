package de.ecconia.java.opentung.core.tools;

import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.core.Camera;
import de.ecconia.java.opentung.core.RenderPlane3D;
import de.ecconia.java.opentung.core.data.Hitpoint;
import de.ecconia.java.opentung.core.data.ResizeData;
import de.ecconia.java.opentung.core.data.ShaderStorage;
import de.ecconia.java.opentung.core.data.SharedData;
import de.ecconia.java.opentung.core.structs.GPUTask;
import de.ecconia.java.opentung.libwrap.Matrix;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;
import de.ecconia.java.opentung.meshing.MeshBagContainer;
import de.ecconia.java.opentung.settings.keybinds.Keybindings;
import de.ecconia.java.opentung.simulation.SimulationManager;
import de.ecconia.java.opentung.util.math.Quaternion;
import de.ecconia.java.opentung.util.math.Vector3;
import java.util.concurrent.BlockingQueue;
import org.lwjgl.opengl.GL30;

public class Resize implements Tool
{
	private final RenderPlane3D worldRenderer;
	
	private final BlockingQueue<GPUTask> gpuTasks;
	private final ShaderStorage shaderStorage;
	private final SimulationManager simulation;
	private final MeshBagContainer worldMesh;
	
	//Custom data:
	private ResizeData resizeData;
	
	public Resize(SharedData sharedData)
	{
		worldRenderer = sharedData.getRenderPlane3D();
		
		gpuTasks = sharedData.getGpuTasks();
		simulation = sharedData.getBoardUniverse().getSimulation();
		worldMesh = sharedData.getRenderPlane3D().getWorldMesh();
		shaderStorage = sharedData.getShaderStorage();
	}
	
	@Override
	public Boolean activateKeyUp(Hitpoint hitpoint, int scancode, boolean control)
	{
		if(scancode == Keybindings.KeyResize)
		{
			if(hitpoint.getHitPart() instanceof CompBoard)
			{
				System.out.println("Starting board resizing.");
				gpuTasks.add((worldRenderer) -> {
					CompBoard board = (CompBoard) hitpoint.getHitPart();
					resizeData = new ResizeData(board);
					if(!resizeData.isResizeAllowed())
					{
						System.out.println("Cannot resize this board, no side resizeable.");
						resizeData = null;
					}
					else
					{
						worldMesh.removeComponent(board, simulation);
					}
					
					worldRenderer.toolReady(); //Enable render and input thread access.
				});
				return true;
			}
			else
			{
				//TBI: Maybe just loop to the parent until board found?
				System.out.println("Only boards can be resized, please look at one when starting resizing.");
				return false;
			}
		}
		return null;
	}
	
	@Override
	public boolean keyUp(int scancode, boolean control)
	{
		if(scancode == Keybindings.KeyResize)
		{
			worldRenderer.toolStopInputs(); //Stop the input thread from further accessing this tool.
			
			System.out.println("Stopping board resizing.");
			//Apply resizing
			gpuTasks.add((worldRenderer) -> {
				CompBoard board = resizeData.getBoard();
				board.setPositionGlobal(resizeData.getPosition());
				board.setSize(resizeData.getBoardX(), resizeData.getBoardZ());
				board.createOwnBounds(); //The board's own size has changed, update its bounds.
				board.updateBounds(); //Notify the parents that the bounds of this component changed.
				worldMesh.addComponent(board, simulation);
				
				resizeData = null;
				worldRenderer.toolDisable();
			});
			return true;
		}
		return false;
	}
	
	@Override
	public boolean abort()
	{
		worldRenderer.toolStopInputs();
		
		System.out.println("Aborting board resizing.");
		gpuTasks.add((worldRenderer) -> {
			worldMesh.addComponent(resizeData.getBoard(), simulation);
			
			resizeData = null;
			worldRenderer.toolDisable();
		});
		
		return true;
	}
	
	//Calculation:
	
	@Override
	public Hitpoint adjustHitpoint(Hitpoint hitpoint)
	{
		boolean skipHitpoint = false;
		//Check collision with drag-area:
		Quaternion alignment = resizeData.getBoard().getAlignmentGlobal();
		Vector3 position = resizeData.getPosition();
		
		//Calculate the camera ray in board space:
		Camera camera = worldRenderer.getCamera();
		Vector3 cameraPosition = camera.getPosition();
		Vector3 cameraRay = Vector3.zp;
		cameraRay = Quaternion.angleAxis(camera.getNeck(), Vector3.xn).multiply(cameraRay);
		cameraRay = Quaternion.angleAxis(camera.getRotation(), Vector3.yn).multiply(cameraRay);
		Vector3 cameraRayBoardSpace = alignment.multiply(cameraRay);
		Vector3 cameraPositionBoardSpace = alignment.multiply(cameraPosition.subtract(position));
		
		//Calculate hitpoint of Y=0 layer:
		boolean isMouseDown = worldRenderer.getController().isLeftMouseDown();
		double rayDistanceMultiplicator = -cameraPositionBoardSpace.getY() / cameraRayBoardSpace.getY();
		if(rayDistanceMultiplicator >= 0)
		{
			Vector3 cameraToCollisionVector = cameraRayBoardSpace.multiply(rayDistanceMultiplicator);
			Vector3 collisionPoint = cameraPositionBoardSpace.add(cameraToCollisionVector);
			double collisionX = collisionPoint.getX();
			double collisionZ = collisionPoint.getZ();
			
			if(!resizeData.isMouseDown() && isMouseDown)
			{
				double xVal = Math.abs(collisionX);
				double zVal = Math.abs(collisionZ);
				boolean xMatch = xVal > (double) resizeData.getBoardX() * 0.15;
				boolean zMatch = zVal > (double) resizeData.getBoardZ() * 0.15;
				if(xMatch != zMatch)
				{
					xVal -= (double) resizeData.getBoardX() * 0.15;
					zVal -= (double) resizeData.getBoardZ() * 0.15;
					xMatch &= xVal < 0.6;
					zMatch &= zVal < 0.6;
					if(xMatch || zMatch)
					{
						resizeData.setAxisX(xMatch);
						resizeData.setNegative(xMatch ? collisionX < 0 : collisionZ < 0);
						if(resizeData.isAllowed())
						{
							skipHitpoint = true;
							resizeData.setPoints(collisionX, collisionZ);
						}
					}
				}
			}
			else if(resizeData.hasPoints() && isMouseDown)
			{
				skipHitpoint = true;
				double relevantOldAxisPoint = resizeData.isAxisX() ? resizeData.getPointX() : resizeData.getPointZ();
				double relevantCollisionAxis = resizeData.isAxisX() ? collisionX : collisionZ;
				double diff = relevantOldAxisPoint - relevantCollisionAxis;
				if(!resizeData.isNegative())
				{
					diff = -diff;
				}
				int squareChange = (int) (diff / 0.3);
				resizeData.adjustSize(squareChange);
			}
		}
		if(resizeData.hasPoints() && !isMouseDown && resizeData.isMouseDown())
		{
			resizeData.setPoints(null, null);
		}
		resizeData.setMouseDown(isMouseDown);
		
		return skipHitpoint ? new Hitpoint() : hitpoint;
	}
	
	//Rendering:
	
	@Override
	public void renderWorld(float[] view)
	{
		CompBoard board = resizeData.getBoard();
		
		//Draw board:
		Vector3 position = resizeData.getPosition();
		int x = resizeData.getBoardX();
		int z = resizeData.getBoardZ();
		
		//TBI: Ehh skip the model? (For now yes, the component is very defined in TUNG and LW).
		Matrix matrix = new Matrix();
		//Apply global position:
		matrix.translate((float) position.getX(), (float) position.getY(), (float) position.getZ());
		matrix.multiply(new Matrix(board.getAlignmentGlobal().createMatrix())); //Apply global rotation.
		//The cube is centered, no translation.
		matrix.scale((float) x * 0.15f, 0.075f, (float) z * 0.15f);
		
		//Draw the board:
		shaderStorage.getBoardTexture().activate();
		ShaderProgram textureCubeShader = shaderStorage.getTextureCubeShader();
		textureCubeShader.use();
		textureCubeShader.setUniformM4(1, view);
		textureCubeShader.setUniformM4(2, matrix.getMat());
		textureCubeShader.setUniformV2(3, new float[]{x, z});
		textureCubeShader.setUniformV4(4, board.getColor().asArray());
		GenericVAO textureCube = shaderStorage.getVisibleOpTexCube();
		textureCube.use();
		textureCube.draw();
	}
	
	public void renderOverlay(float[] view)
	{
		CompBoard board = resizeData.getBoard();
		
		//Draw outer drag-surfaces:
		ShaderProgram resizeShader = shaderStorage.getResizeShader();
		GenericVAO vao = shaderStorage.getResizeSurface();
		GenericVAO vaoBorder = shaderStorage.getResizeBorder();
		
		resizeShader.use();
		resizeShader.setUniformM4(1, view);
		
		Matrix parentRotation = new Matrix(board.getAlignmentGlobal().createMatrix());
		
		Matrix modelMatrix = new Matrix();
		modelMatrix.translate(
				(float) resizeData.getPosition().getX(),
				(float) resizeData.getPosition().getY(),
				(float) resizeData.getPosition().getZ()
		);
		modelMatrix.multiply(parentRotation);
		
		GL30.glDisable(GL30.GL_CULL_FACE);
		GL30.glDepthFunc(GL30.GL_ALWAYS);
		
		int x = resizeData.getBoardX();
		int z = resizeData.getBoardZ();
		Matrix copyMatrix;
		
		//PosZ
		if(resizeData.allowsPZ())
		{
			copyMatrix = modelMatrix.copy();
			copyMatrix.translate(0, 0, z * 0.15f + 0.3f);
			copyMatrix.scale(x * 0.30f, 1, 0.6f);
			resizeShader.setUniformM4(2, copyMatrix.getMat());
			resizeShader.setUniformV4(3, new float[]{1.0f, 1.0f, 0.0f, 0.4f});
			vao.use();
			vao.draw();
			resizeShader.setUniformV4(3, new float[]{1.0f, 1.0f, 0.0f, 1.0f});
			vaoBorder.use();
			vaoBorder.draw();
		}
		//PosX
		if(resizeData.allowsPX())
		{
			copyMatrix = modelMatrix.copy();
			copyMatrix.translate(x * 0.15f + 0.3f, 0, 0);
			copyMatrix.scale(0.6f, 1, z * 0.30f);
			resizeShader.setUniformM4(2, copyMatrix.getMat());
			resizeShader.setUniformV4(3, new float[]{1.0f, 1.0f, 0.0f, 0.4f});
			vao.use();
			vao.draw();
			resizeShader.setUniformV4(3, new float[]{1.0f, 1.0f, 0.0f, 1.0f});
			vaoBorder.use();
			vaoBorder.draw();
		}
		//NegZ
		if(resizeData.allowsNZ())
		{
			copyMatrix = modelMatrix.copy();
			copyMatrix.translate(0, 0, -z * 0.15f - 0.3f);
			copyMatrix.scale(x * 0.30f, 1, 0.6f);
			resizeShader.setUniformM4(2, copyMatrix.getMat());
			resizeShader.setUniformV4(3, new float[]{1.0f, 1.0f, 0.0f, 0.4f});
			vao.use();
			vao.draw();
			resizeShader.setUniformV4(3, new float[]{1.0f, 1.0f, 0.0f, 1.0f});
			vaoBorder.use();
			vaoBorder.draw();
		}
		//NegX
		if(resizeData.allowsNX())
		{
			copyMatrix = modelMatrix.copy();
			copyMatrix.translate(-x * 0.15f - 0.3f, 0, 0);
			copyMatrix.scale(0.6f, 1, z * 0.30f);
			resizeShader.setUniformM4(2, copyMatrix.getMat());
			resizeShader.setUniformV4(3, new float[]{1.0f, 1.0f, 0.0f, 0.4f});
			vao.use();
			vao.draw();
			resizeShader.setUniformV4(3, new float[]{1.0f, 1.0f, 0.0f, 1.0f});
			vaoBorder.use();
			vaoBorder.draw();
		}
		
		//Debug bounds:
//		{
//			copyMatrix = modelMatrix.copy();
//
//			double width = resizeData.px - resizeData.nx;
//			double depth = resizeData.pz - resizeData.nz;
//			copyMatrix.translate((float) (resizeData.px - width / 2.0), 0, (float) (resizeData.pz - depth / 2.0));
//			copyMatrix.scale((float) width, 1, (float) depth);
//
//			resizeShader.setUniformM4(2, copyMatrix.getMat());
//			resizeShader.setUniformV4(3, new float[]{1.0f, 0.0f, 0.0f, 0.4f});
//			vao.use();
//			vao.draw();
//			resizeShader.setUniformV4(3, new float[]{1.0f, 0.0f, 0.0f, 1.0f});
//			vaoBorder.use();
//			vaoBorder.draw();
//		}
		
		GL30.glEnable(GL30.GL_CULL_FACE);
		GL30.glDepthFunc(GL30.GL_LESS);
	}
}

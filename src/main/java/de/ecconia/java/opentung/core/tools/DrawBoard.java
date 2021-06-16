package de.ecconia.java.opentung.core.tools;

import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.core.Camera;
import de.ecconia.java.opentung.core.data.Hitpoint;
import de.ecconia.java.opentung.core.data.HitpointContainer;
import de.ecconia.java.opentung.core.data.ShaderStorage;
import de.ecconia.java.opentung.core.data.SharedData;
import de.ecconia.java.opentung.core.structs.GPUTask;
import de.ecconia.java.opentung.inputs.InputProcessor;
import de.ecconia.java.opentung.libwrap.Matrix;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;
import de.ecconia.java.opentung.util.math.MathHelper;
import de.ecconia.java.opentung.util.math.Quaternion;
import de.ecconia.java.opentung.util.math.Vector3;
import java.util.concurrent.BlockingQueue;

public class DrawBoard implements Tool
{
	private final SharedData sharedData;
	private final BlockingQueue<GPUTask> gpuTasks;
	private final ShaderStorage shaderStorage;
	
	private HitpointContainer boardDrawStartingPoint = null; //Scope input/(render), read on many places.
	
	public DrawBoard(SharedData sharedData)
	{
		this.sharedData = sharedData;
		
		gpuTasks = sharedData.getGpuTasks();
		shaderStorage = sharedData.getShaderStorage();
	}
	
	@Override
	public Boolean activateMouseDown(Hitpoint hitpoint, int buttonCode, boolean control)
	{
		if(sharedData.getCurrentPlaceable() != CompBoard.info)
		{
			return null;
		}
		if(buttonCode != InputProcessor.MOUSE_LEFT)
		{
			return null;
		}
		if(hitpoint.canBePlacedOn())
		{
			HitpointContainer hitpointContainer = (HitpointContainer) hitpoint;
			if(hitpointContainer.getAlignment() == null || hitpointContainer.getPosition() == null)
			{
				//Should never happen.
				System.out.println("[ERROR] Could not start board drawing, cause alignment and position are not set.");
				return null;
			}
			
			//TODO: Proper abort of the placement mode, once started.
			//Only called when looking at a container.
			gpuTasks.add((worldRenderer) -> {
				//Start dragging until end.
				boardDrawStartingPoint = hitpointContainer;
				worldRenderer.toolReady();
			});
			
			return true;
		}
		return null;
	}
	
	@Override
	public Hitpoint adjustHitpoint(Hitpoint hitpoint)
	{
		//Drawing a board:
		Quaternion alignment = boardDrawStartingPoint.getAlignment();
		Vector3 position = boardDrawStartingPoint.getPosition();
		int x = 1;
		int z = 1;
		
		//Calculate the camera ray in board space:
		Camera camera = sharedData.getRenderPlane3D().getCamera();
		Vector3 cameraPosition = camera.getPosition();
		Vector3 cameraRay = Vector3.zp;
		cameraRay = Quaternion.angleAxis(camera.getNeck(), Vector3.xn).multiply(cameraRay);
		cameraRay = Quaternion.angleAxis(camera.getRotation(), Vector3.yn).multiply(cameraRay);
		Vector3 cameraRayBoardSpace = alignment.multiply(cameraRay);
		Vector3 cameraPositionBoardSpace = alignment.multiply(cameraPosition.subtract(position));
		
		boolean calculateSizeAndPos = true;
		
		if(boardDrawStartingPoint.getCameraPosition() == null && boardDrawStartingPoint.getCameraRay() == null)
		{
			//If both values are null, then we are running this code for the very first time. Thus apply the values:
			boardDrawStartingPoint.setCamera(cameraPositionBoardSpace, cameraRayBoardSpace);
			calculateSizeAndPos = false; //Once this runs for the very first time, the camera probably has not moved, thus do not grow the board.
		}
		else if(boardDrawStartingPoint.getCameraRay() != null)
		{
			//At least one of the two values is not null, check if camera-ray got reset, which indicates, that the user interacted with the board.
			//Calculate camera ray angle change and camera position change:
			double angle = MathHelper.angleFromVectors(cameraRayBoardSpace, boardDrawStartingPoint.getCameraRay());
			double distance = cameraPositionBoardSpace.subtract(boardDrawStartingPoint.getCameraPosition()).length();
			
			if(angle > 1.5 || distance > 0.1)
			{
				//User moved enough to disable this check.
				boardDrawStartingPoint.setCameraRay(null);
			}
			else
			{
				//User did not move enough, prevent board growing.
				calculateSizeAndPos = false;
			}
		}
		
		if(calculateSizeAndPos)
		{
			double rayDistanceMultiplicatorUp = -(cameraPositionBoardSpace.getY() + 0.075D) / cameraRayBoardSpace.getY();
			double rayDistanceMultiplicatorBot = -(cameraPositionBoardSpace.getY() - 0.075D) / cameraRayBoardSpace.getY();
			if(rayDistanceMultiplicatorUp >= 0 || rayDistanceMultiplicatorBot >= 0) //One of the two values is above or 0.
			{
				double rayDistanceMultiplicator;
				//Choose the shorter multiplicator, to get the nearer plane.
				if(rayDistanceMultiplicatorUp < 0 //If the Up value is negative, choose the Bottom value, since Bottom must be positive.
						|| //The Up value is non-negative!
						(rayDistanceMultiplicatorBot >= 0 //And the Bottom value is non-negative (if it would be negative, choose Up value.
								&& //Now both values are non-negative.
								rayDistanceMultiplicatorBot < rayDistanceMultiplicatorUp //Choose Bottom if it is smaller.
						))
				{
					rayDistanceMultiplicator = rayDistanceMultiplicatorBot;
				}
				else
				{
					rayDistanceMultiplicator = rayDistanceMultiplicatorUp;
				}
				
				Vector3 cameraToCollisionVector = cameraRayBoardSpace.multiply(rayDistanceMultiplicator);
				double cameraToCollisionVectorLength = cameraToCollisionVector.length();
				
				//Abort if one is not looking at the plane, but on the placement parent:
				if(hitpoint.getHitPart() != boardDrawStartingPoint.getHitPart() || hitpoint.getDistance() >= cameraToCollisionVectorLength)
				{
					double unitRayLength = cameraRayBoardSpace.length();
					if(cameraToCollisionVectorLength - unitRayLength > 20)
					{
						//TBI: Is this okay?
						//Limits the length of the camera to collision vector to 20.
						cameraToCollisionVector = cameraToCollisionVector.multiply(1.0 / cameraToCollisionVector.length() * 20);
					}
					Vector3 collisionPoint = cameraPositionBoardSpace.add(cameraToCollisionVector);
					double collisionX = collisionPoint.getX();
					double collisionZ = collisionPoint.getZ();
					
					//Y should be at 0 or very close to it - x and z can be used as are.
					x = (int) ((Math.abs(collisionX) + 0.15f) / 0.3f) + 1;
					z = (int) ((Math.abs(collisionZ) + 0.15f) / 0.3f) + 1;
					Vector3 roundedCollisionPoint = new Vector3((x - 1) * 0.15 * (collisionX >= 0 ? 1f : -1f), 0, (z - 1) * 0.15 * (collisionZ >= 0 ? 1f : -1f));
					position = position.add(alignment.inverse().multiply(roundedCollisionPoint));
				}
			}
		}
		boardDrawStartingPoint.setBoardData(position, x, z);
		
		return hitpoint;
	}
	
	@Override
	public boolean mouseLeftUp()
	{
		sharedData.getRenderPlane3D().toolStopInputs();
		
		CompContainer parent = (CompContainer) boardDrawStartingPoint.getHitPart();
		if(parent != sharedData.getBoardUniverse().getRootBoard() && parent == null)
		{
			System.out.println("Board attempted to draw board on is deleted/gone.");
			gpuTasks.add((worldRenderer) -> {
				worldRenderer.toolDisable();
			});
			return false;
		}
		int x = boardDrawStartingPoint.getBoardX();
		int z = boardDrawStartingPoint.getBoardZ();
		Component newComponent = new CompBoard(parent, x, z);
		newComponent.setRotation(boardDrawStartingPoint.getAlignment());
		newComponent.setPosition(boardDrawStartingPoint.getBoardCenterPosition());
		
		gpuTasks.add((worldRenderer) -> {
			parent.addChild(newComponent);
			parent.updateBounds();
			worldRenderer.getWorldMesh().addComponent(newComponent, sharedData.getBoardUniverse().getSimulation());
			this.boardDrawStartingPoint = null;
			
			worldRenderer.resetFineBoardOffset();
			worldRenderer.toolDisable();
		});
		return true; //Don't do all the other checks, obsolete.
	}
	
	@Override
	public boolean abort()
	{
		sharedData.getRenderPlane3D().toolStopInputs();
		gpuTasks.add((worldRenderer) -> {
			this.boardDrawStartingPoint = null; //Not needed, but will not hurt.
			worldRenderer.toolDisable();
		});
		
		return true;
	}
	
	@Override
	public void renderWorld(float[] view)
	{
		Quaternion alignment = boardDrawStartingPoint.getAlignment();
		Vector3 position = boardDrawStartingPoint.getBoardCenterPosition();
		int x = boardDrawStartingPoint.getBoardX();
		int z = boardDrawStartingPoint.getBoardZ();
		
		//TBI: Ehh skip the model? (For now yes, the component is very defined in TUNG and LW).
		Matrix matrix = new Matrix();
		//Apply global position:
		matrix.translate((float) position.getX(), (float) position.getY(), (float) position.getZ());
		matrix.multiply(new Matrix(alignment.createMatrix())); //Apply global rotation.
		//The cube is centered, no translation.
		matrix.scale((float) x * 0.15f, 0.075f, (float) z * 0.15f);
		
		//Draw the board:
		shaderStorage.getBoardTexture().activate();
		ShaderProgram textureCubeShader = shaderStorage.getTextureCubeShader();
		textureCubeShader.use();
		textureCubeShader.setUniformM4(1, view);
		textureCubeShader.setUniformM4(2, matrix.getMat());
		textureCubeShader.setUniformV2(3, new float[]{x, z});
		textureCubeShader.setUniformV4(4, Color.boardDefault.asArray());
		GenericVAO textureCube = shaderStorage.getVisibleOpTexCube();
		textureCube.use();
		textureCube.draw();
	}
}

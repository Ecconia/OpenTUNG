package de.ecconia.java.opentung.core.tools;

import java.util.concurrent.BlockingQueue;

import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.CompMount;
import de.ecconia.java.opentung.components.CompSnappingPeg;
import de.ecconia.java.opentung.components.conductor.CompWireRaw;
import de.ecconia.java.opentung.components.conductor.Connector;
import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.components.meta.Part;
import de.ecconia.java.opentung.components.meta.PlaceableInfo;
import de.ecconia.java.opentung.core.BoardUniverse;
import de.ecconia.java.opentung.core.data.Hitpoint;
import de.ecconia.java.opentung.core.data.HitpointBoard;
import de.ecconia.java.opentung.core.data.HitpointContainer;
import de.ecconia.java.opentung.core.data.ShaderStorage;
import de.ecconia.java.opentung.core.data.SharedData;
import de.ecconia.java.opentung.core.helper.OnBoardPlacementHelper;
import de.ecconia.java.opentung.core.helper.World3DHelper;
import de.ecconia.java.opentung.core.structs.GPUTask;
import de.ecconia.java.opentung.libwrap.Matrix;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;
import de.ecconia.java.opentung.settings.Settings;
import de.ecconia.java.opentung.settings.keybinds.Keybindings;
import de.ecconia.java.opentung.simulation.Updateable;
import de.ecconia.java.opentung.util.math.MathHelper;
import de.ecconia.java.opentung.util.math.Quaternion;
import de.ecconia.java.opentung.util.math.Vector3;
import org.lwjgl.opengl.GL30;

public class NormalPlacement implements Tool
{
	private final SharedData sharedData;
	private final ShaderStorage shaderStorage;
	private final BlockingQueue<GPUTask> gpuTasks;
	private final BoardUniverse board;
	
	private Hitpoint hitpoint;
	private PlaceableInfo currentPlaceable;
	
	private boolean placeableBoardIsLaying = true;
	private double placementRotation = 0;
	private double fineBoardOffset;
	
	public NormalPlacement(SharedData sharedData)
	{
		this.sharedData = sharedData;
		
		shaderStorage = sharedData.getShaderStorage();
		gpuTasks = sharedData.getGpuTasks();
		board = sharedData.getBoardUniverse();
	}
	
	public void rotatePlacement(boolean control)
	{
		gpuTasks.add((unused) -> {
			double degrees = control ? 22.5 : 90;
			if(this.currentPlaceable == CompBoard.info)
			{
				//Boards cannot be fine rotated, thus when pressing control, rotate roughly.
				degrees = 90;
			}
			placementRotation += degrees;
			if(placementRotation >= 360)
			{
				placementRotation -= 360;
			}
			if(placementRotation <= 0)
			{
				placementRotation += 360;
			}
		});
	}
	
	private double toRoughRotation()
	{
		double rotation = placementRotation;
		double remains = rotation % 90;
		return rotation - remains;
	}
	
	private boolean allowBoardOffset(boolean finePlacement)
	{
		//TBI: Maybe also disallow it, while movement keys WASD are pressed.
		Hitpoint hitpoint = this.hitpoint;
		if(finePlacement //Must only allow fine-offset.
				&& currentPlaceable == CompBoard.info //Must be a board selected in hotbar.
				&& hitpoint.getHitPart() instanceof CompBoard //Must be placed on a board.
		)
		{
			HitpointBoard hitpointBoard = (HitpointBoard) hitpoint;
			return (!placeableBoardIsLaying && hitpointBoard.getLocalNormal().getY() != 0)
					|| (placeableBoardIsLaying && hitpointBoard.getLocalNormal().getY() == 0);
		}
		return false;
	}
	
	@Override
	public boolean scroll(int amount, boolean control, boolean alt)
	{
		if(allowBoardOffset(control))
		{
			gpuTasks.add((unused) -> {
				//Only fine offset:
				fineBoardOffset += amount * 0.075;
				if(fineBoardOffset > 0.1511)
				{
					fineBoardOffset = 0.15;
				}
				else if(fineBoardOffset < -0.1511)
				{
					fineBoardOffset = -0.15;
				}
			});
			return true;
		}
		return false;
	}
	
	@Override
	public boolean mouseRightUp()
	{
		if(hitpoint.canBePlacedOn() && currentPlaceable == CompBoard.info)
		{
			//Flip board:
			gpuTasks.add((unused) -> {
				//Right clicked while placing a board -> change layout:
				placeableBoardIsLaying = !placeableBoardIsLaying;
			});
			return true;
		}
		return false;
	}
	
	@Override
	public boolean keyUp(int scancode, boolean control)
	{
		if(scancode == Keybindings.KeyRotate)
		{
			rotatePlacement(control);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean mouseLeftUp()
	{
		//TODO: All these values are not fully thread-safe, either wrap in one object, or make thread-safe.
		Hitpoint hitpoint = this.hitpoint;
		PlaceableInfo currentPlaceable = this.currentPlaceable;
		
		if(!hitpoint.canBePlacedOn())
		{
			//If not looking at a container abort.
			return false;
		}
		CompContainer parent = (CompContainer) hitpoint.getHitPart();
		if(parent != board.getRootBoard() && parent.getParent() == null)
		{
			System.out.println("Board attempted to place on is deleted/gone.");
			return false;
		}
		
		HitpointContainer hitpointContainer = (HitpointContainer) hitpoint;
		
		if(currentPlaceable != null)
		{
			if(currentPlaceable == CompBoard.info)
			{
				//System.out.println("WARNING: Called normal placement code with board selected, while boardDrawStartingPoint was null.");
				//Board was very likely dragged from air to placeable location, happens. Else uff.
				//TODO: Differentiate between these two cases.
				return true;
			}
			
			Component newComponent = currentPlaceable.instance(parent);
			newComponent.setAlignmentGlobal(hitpointContainer.getAlignment());
			newComponent.setPositionGlobal(hitpointContainer.getPosition());
			newComponent.init(); //Initializes components such as the ThroughPeg (needs to be called after position is set). TBI: Does it?
			newComponent.initClusters(); //Creates clusters for connectors of the component.
			
			if(newComponent instanceof Updateable)
			{
				board.getSimulation().updateNextTickThreadSafe((Updateable) newComponent);
			}
			
			gpuTasks.add((ignored) -> {
				parent.addChild(newComponent);
				parent.updateBounds();
				sharedData.getRenderPlane3D().getWorldMesh().addComponent(newComponent, board.getSimulation());
				
				//Link snapping peg:
				if(currentPlaceable == CompSnappingPeg.info)
				{
					sharedData.getRenderPlane3D().snapSnappingPeg((CompSnappingPeg) newComponent);
				}
			});
			return true;
		}
		//Else, placing nothing, thus return false.
		
		return false;
	}
	
	@Override
	public Hitpoint adjustHitpoint(Hitpoint hitpoint)
	{
		PlaceableInfo currentPlaceable = sharedData.getCurrentPlaceable();
		//Always calculate the hitpoint, if things could be placed on, regardless of used. It always has to be ready to be used in the next cycle and between.
		if(hitpoint.canBePlacedOn())
		{
			//TBI: When drawing, the hitpoint is inaccurate, cause it might consider the wrong target position (when control).
			HitpointContainer hitpointContainer = (HitpointContainer) hitpoint;
			
			//Normal placement (drawPlacementPosition)
			if(currentPlaceable == null)
			{
				//TBI: 0.075 Offset here or when drawing?
				//Not attempting to draw anything, thus only prepare for drawing the cross, that means lifting the position up to surface:
				CompContainer parent = (CompContainer) hitpoint.getHitPart();
				if(hitpoint.isBoard())
				{
					HitpointBoard hitpointBoard = (HitpointBoard) hitpoint;
					OnBoardPlacementHelper helper = new OnBoardPlacementHelper((CompBoard) parent, hitpointBoard.getLocalNormal(), hitpointBoard.getCollisionPointBoardSpace());
					Vector3 collisionPointBoardSpace = helper.middleEither();
					hitpointContainer.setPosition(parent.getAlignmentGlobal().inverse().multiply(collisionPointBoardSpace)
							.add(parent.getPositionGlobal())
							.add(hitpointContainer.getNormal().multiply(0.075))
					);
				}
				else //Mount:
				{
					hitpointContainer.setPosition(parent.getPositionGlobal()
							.add(hitpointContainer.getNormal().multiply(CompMount.MOUNT_HEIGHT + 0.075D)));
				}
			}
			else //Placing something:
			{
				//Calculate new alignment:
				Quaternion alignment = MathHelper.rotationFromVectors(Vector3.yp, hitpointContainer.getNormal());
				double rotation = placementRotation;
				if(currentPlaceable == CompBoard.info)
				{
					//Boards cannot be fine-rotated.
					rotation = toRoughRotation();
				}
				double normalAxisRotationAngle = -rotation + sharedData.getRenderPlane3D().calculateFixRotationOffset(alignment, hitpoint);
				Quaternion normalAxisRotation = Quaternion.angleAxis(normalAxisRotationAngle, hitpointContainer.getNormal());
				alignment = alignment.multiply(normalAxisRotation);
				if(currentPlaceable == CompBoard.info)
				{
					//Specific board rotation:
					Quaternion boardAlignment = Quaternion.angleAxis(placeableBoardIsLaying ? 0 : 90, Vector3.xn);
					alignment = boardAlignment.multiply(alignment);
				}
				alignment = alignment.normalize(); //Prevent any broken parent quaternion, from sharing its corruption.
				hitpointContainer.setAlignment(alignment);
				
				//Calculate new position:
				CompContainer parent = (CompContainer) hitpoint.getHitPart();
				if(hitpoint.isBoard())
				{
					HitpointBoard hitpointBoard = (HitpointBoard) hitpoint;
					OnBoardPlacementHelper placementHelper = new OnBoardPlacementHelper((CompBoard) parent, hitpointBoard.getLocalNormal(), hitpointBoard.getCollisionPointBoardSpace());
					Vector3 position = placementHelper.auto(currentPlaceable.getModel(), sharedData.getRenderPlane3D().getController().isControl(), alignment);
					if(position == null && currentPlaceable == CompSnappingPeg.info)
					{
						//Attempt again without control: (Should center it).
						position = placementHelper.auto(currentPlaceable.getModel(), false, alignment);
					}
					if(position == null)
					{
						hitpoint = new Hitpoint(hitpoint.getHitPart(), hitpoint.getDistance()); //Prevent the component from being drawn, by just changing the hitpoint type. [pretend non-container]
					}
					else
					{
						position = parent.getAlignmentGlobal().inverse().multiply(position).add(parent.getPositionGlobal());
						if(currentPlaceable == CompMount.info)
						{
							if(!placementHelper.isSide() && !sharedData.getRenderPlane3D().getController().isControl())
							{
								//Apply offset:
								Vector3 offset = new Vector3(0, 0, -0.15);
								position = position.add(alignment.inverse().multiply(offset));
							}
						}
						else if(currentPlaceable == CompBoard.info)
						{
							double distance = 0.15D;
							if(!placeableBoardIsLaying)
							{
								distance += 0.075D;
								if(!placementHelper.isSide() && fineBoardOffset != 0)
								{
									Vector3 offset = new Vector3(0, fineBoardOffset, 0);
									offset = alignment.inverse().multiply(offset);
									position = position.add(offset);
								}
							}
							else
							{
								if(placementHelper.isSide() && fineBoardOffset != 0)
								{
									//TODO: This code depends on where the normal of the parent points, instead of the rotation of the child.
									//Code should work like the one above, the problem is, that the offset has to be applied to either X or Z depending on rotation.
									Vector3 offset = new Vector3(0, fineBoardOffset, 0); //In parent board space, thus only up/down = Y.
									offset = parent.getAlignmentGlobal().inverse().multiply(offset);
									position = position.add(offset);
								}
							}
							position = position.add(hitpointContainer.getNormal().multiply(distance));
						}
						hitpointContainer.setPosition(position);
					}
				}
				else //Mount:
				{
					ModelHolder model = currentPlaceable.getModel();
					if(currentPlaceable == CompBoard.info)
					{
						double extraY = 0.15;
						if(!placeableBoardIsLaying)
						{
							extraY += 0.075;
						}
						hitpointContainer.setPosition(parent.getPositionGlobal().add(hitpointContainer.getNormal().multiply(CompMount.MOUNT_HEIGHT + extraY)));
					}
					else if(model.canBePlacedOnMounts())
					{
						hitpointContainer.setPosition(parent.getPositionGlobal().add(hitpointContainer.getNormal().multiply(CompMount.MOUNT_HEIGHT)));
					}
					else
					{
						hitpoint = new Hitpoint(hitpoint.getHitPart(), hitpoint.getDistance()); //Prevent the component from being drawn, by just changing the hitpoint type. [pretend non-container]
					}
				}
			}
		}
		
		this.hitpoint = hitpoint;
		this.currentPlaceable = currentPlaceable;
		return hitpoint;
	}
	
	@Override
	public void renderWorld(float[] view)
	{
		if(hitpoint.canBePlacedOn())
		{
			HitpointContainer hitpointContainer = (HitpointContainer) hitpoint;
			if(currentPlaceable == null)
			{
				ShaderProgram lineShader = shaderStorage.getLineShader();
				//TODO: Switch to line shader with uniform color.
				lineShader.use();
				lineShader.setUniformM4(1, view);
				GL30.glLineWidth(5f);
				Matrix model = new Matrix();
				model.identity();
				Vector3 datPos = hitpointContainer.getPosition();
				model.translate((float) datPos.getX(), (float) datPos.getY(), (float) datPos.getZ());
				lineShader.setUniformM4(2, model.getMat());
				GenericVAO crossyIndicator = shaderStorage.getCrossyIndicator();
				crossyIndicator.use();
				crossyIndicator.draw();
			}
			else if(currentPlaceable == CompBoard.info)
			{
				int x = 1;
				int z = 1;
				//TBI: Ehh skip the model? (For now yes, the component is very defined in TUNG and LW).
				Matrix matrix = new Matrix();
				//Apply global position:
				Vector3 position = hitpointContainer.getPosition();
				matrix.translate((float) position.getX(), (float) position.getY(), (float) position.getZ());
				Quaternion newAlignment = hitpointContainer.getAlignment();
				matrix.multiply(new Matrix(newAlignment.createMatrix())); //Apply global rotation.
				//The cube is centered, no translation.
				matrix.scale((float) x * 0.15f, 0.075f, (float) z * 0.15f); //Just use the right size from the start... At this point in code it always has that size.
				
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
			else
			{
				ShaderProgram visibleCubeShader = shaderStorage.getVisibleCubeShader();
				GenericVAO visibleCube = shaderStorage.getVisibleOpTexCube();
				World3DHelper.drawModel(visibleCubeShader, visibleCube, currentPlaceable.getModel(), hitpointContainer.getPosition(), hitpointContainer.getAlignment(), view);
			}
		}
	}
	
	@Override
	public void renderOverlay(float[] view)
	{
		if(hitpoint.isEmpty())
		{
			return;
		}
		else
		{
			Part part = hitpoint.getHitPart();
			
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
		}
		
		float[] color = new float[]{
				Settings.highlightColorR,
				Settings.highlightColorG,
				Settings.highlightColorB,
				Settings.highlightColorA
		};
		
		Part part = hitpoint.getHitPart();
		
		//Enable drawing to stencil buffer
		GL30.glStencilMask(0xFF);
		
		ShaderProgram invisibleCubeShader = shaderStorage.getInvisibleCubeShader();
		GenericVAO invisibleCube = shaderStorage.getInvisibleCube();
		if(part instanceof Component)
		{
			World3DHelper.drawStencilComponent(invisibleCubeShader, invisibleCube, (Component) part, view);
		}
		else //Connector
		{
			invisibleCubeShader.use();
			invisibleCubeShader.setUniformM4(1, view);
			invisibleCubeShader.setUniformV4(3, new float[]{0, 0, 0, 0});
			World3DHelper.drawCubeFull(invisibleCubeShader, invisibleCube, ((Connector) part).getModel(), part, part.getParent().getModelHolder().getPlacementOffset(), new Matrix());
		}
		
		//Draw on top
		GL30.glDisable(GL30.GL_DEPTH_TEST);
		//Only draw if stencil bit is set.
		GL30.glStencilFunc(GL30.GL_EQUAL, 1, 0xFF);
		
		ShaderProgram planeShader = shaderStorage.getFlatPlaneShader();
		planeShader.use();
		planeShader.setUniformV4(0, color);
		GenericVAO fullCanvasPlane = shaderStorage.getFlatPlane();
		fullCanvasPlane.use();
		fullCanvasPlane.draw();
		
		//Restore settings:
		GL30.glStencilFunc(GL30.GL_NOTEQUAL, 1, 0xFF);
		GL30.glEnable(GL30.GL_DEPTH_TEST);
		//Clear stencil buffer:
		GL30.glClear(GL30.GL_STENCIL_BUFFER_BIT);
		//After clearing, disable usage/writing of/to stencil buffer again.
		GL30.glStencilMask(0x00);
	}
	
	public void resetFineBoardOffset()
	{
		this.fineBoardOffset = 0;
	}
}

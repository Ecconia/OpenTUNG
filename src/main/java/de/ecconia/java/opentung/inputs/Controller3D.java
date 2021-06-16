package de.ecconia.java.opentung.inputs;

import de.ecconia.java.opentung.components.conductor.Connector;
import de.ecconia.java.opentung.components.meta.Holdable;
import de.ecconia.java.opentung.components.meta.Part;
import de.ecconia.java.opentung.core.Camera;
import de.ecconia.java.opentung.core.RenderPlane3D;
import de.ecconia.java.opentung.settings.Settings;
import de.ecconia.java.opentung.settings.keybinds.Keybindings;
import de.ecconia.java.opentung.simulation.SimulationManager;
import org.lwjgl.glfw.GLFW;

public class Controller3D implements Controller
{
	private final RenderPlane3D renderPlane3D;
	private final Camera camera;
	
	private InputProcessor inputProcessor;
	
	public Controller3D(RenderPlane3D renderPlane3D)
	{
		this.renderPlane3D = renderPlane3D;
		camera = renderPlane3D.getCamera();
	}
	
	@Override
	public void setInputThread(InputProcessor inputProcessor)
	{
		this.inputProcessor = inputProcessor;
	}
	
	@Override
	public void inputInterval()
	{
		camera.movement(inputProcessor.getMouseXChange(), inputProcessor.getMouseYChange(),
				inputProcessor.flyLeft,
				inputProcessor.flyRight,
				inputProcessor.flyForward,
				inputProcessor.flyBackward,
				inputProcessor.flyUp,
				inputProcessor.flyDown,
				inputProcessor.flyBoost
		);
	}
	
	@Override
	public void mouseUp(int type, int x, int y)
	{
		if(type == InputProcessor.MOUSE_LEFT)
		{
			checkMouseLeft(false);
		}
		else if(type == InputProcessor.MOUSE_RIGHT)
		{
			checkMouseRight(false);
		}
	}
	
	@Override
	public void mouseDown(int type, int x, int y)
	{
		if(type == InputProcessor.MOUSE_LEFT)
		{
			checkMouseLeft(true);
		}
		else if(type == InputProcessor.MOUSE_RIGHT)
		{
			checkMouseRight(true);
		}
		else if(type == InputProcessor.MOUSE_MIDDLE)
		{
			middleMouseDown();
		}
	}
	
	@Override
	public void scrolled(double xScroll, double yScroll)
	{
		int yAmount = (int) Math.floor(yScroll);
		if(yAmount != 0)
		{
			scrollY(yAmount);
		}
	}
	
	@Override
	public void keyUp(int keyIndex, int scancode, int mods)
	{
		if(keyIndex == GLFW.GLFW_KEY_ESCAPE)
		{
			//Abort primary tool, before opening the main menu:
			if(!renderPlane3D.toolAbort())
			{
				switchToInterface();
			}
		}
		else if(scancode == Keybindings.KeyUnlockMouseCursor)
		{
			inputProcessor.switchTo2D();
			checkMouseLeft(false);
			checkMouseRight(false);
		}
		else if(scancode == Keybindings.KeyPauseSimulation)
		{
			SimulationManager simulation = renderPlane3D.getSharedData().getBoardUniverse().getSimulation();
			simulation.togglePaused();
		}
		else if(scancode == Keybindings.KeyTickSimulation)
		{
			SimulationManager simulation = renderPlane3D.getSharedData().getBoardUniverse().getSimulation();
			simulation.updateJobNextTickThreadSafe((unused) -> {
				if(simulation.isSimulationHalted())
				{
					simulation.doTick();
				}
			});
		}
		else if(scancode == Keybindings.KeyToggleComponentsList)
		{
			inputProcessor.get2DController().openComponentList();
		}
		else if(!renderPlane3D.toolKeyUp(scancode, isControl()))
		{
			if(!renderPlane3D.checkToolActivation(scancode, isControl()))
			{
				if(scancode == Keybindings.KeyHotbarDrop)
				{
					if(isControl())
					{
						renderPlane3D.stopClusterHighlighting();
					}
					else
					{
						inputProcessor.get2DController().dropHotbarEntry();
					}
				}
				else if(keyIndex >= GLFW.GLFW_KEY_0 && keyIndex <= GLFW.GLFW_KEY_9)
				{
					numberPressed(keyIndex - GLFW.GLFW_KEY_0);
				}
			}
		}
	}
	
	@Override
	public void unfocus()
	{
		switchToInterface();
	}
	
	//Stuff:
	
	private boolean mouseDownLeft;
	private boolean mouseDownRight;
	
	private void switchToInterface()
	{
		inputProcessor.get2DController().openPauseMenu();
		checkMouseLeft(false);
		checkMouseRight(false);
	}
	
	private void checkMouseLeft(boolean shouldBePressed)
	{
		if(shouldBePressed)
		{
			if(mouseDownLeft)
			{
				System.out.println("Left click already marked down 3D-Pane, but got downed again.");
			}
			else
			{
				mouseDownLeft = true;
				mouseLeftDown();
			}
		}
		else
		{
			if(mouseDownLeft)
			{
				mouseDownLeft = false;
				mouseLeftUp();
			}
		}
	}
	
	private void checkMouseRight(boolean shouldBePressed)
	{
		if(shouldBePressed)
		{
			if(mouseDownRight)
			{
				System.out.println("Right click already marked down 3D-Pane, but got downed again.");
			}
			else
			{
				mouseDownRight = true;
				mouseRightDown();
			}
		}
		else
		{
			if(mouseDownRight)
			{
				mouseDownRight = false;
				mouseRightUp();
			}
		}
	}
	
	//State awareness / actual handling / non framework:
	
	public void doFrameCycle()
	{
		doLeftHoldableCheck();
	}
	
	//Left:
	
	private long mouseLeftDown;
	private Part mouseLeftDownOn;
	private Holdable mouseLeftHoldable;
	
	private void mouseLeftDown()
	{
		mouseLeftDown = System.currentTimeMillis();
		mouseLeftDownOn = renderPlane3D.getCursorObject();
		renderPlane3D.checkToolActivationMouseDown(InputProcessor.MOUSE_LEFT, isControl());
	}
	
	private void mouseLeftUp()
	{
		if(renderPlane3D.toolMouseLeftUp())
		{
			mouseLeftDown = 0;
			return;
		}
		
		Part mouseLeftDownOn = renderPlane3D.getCursorObject();
		if(mouseLeftDownOn != null)
		{
			long clickDuration = (System.currentTimeMillis() - mouseLeftDown);
			//If the click was longer than a second, validate that its the intended component...
			if(clickDuration > Settings.longMousePressDuration)
			{
				if(this.mouseLeftDownOn == mouseLeftDownOn)
				{
					renderPlane3D.componentLeftClicked(mouseLeftDownOn);
				}
			}
			else
			{
				renderPlane3D.componentLeftClicked(mouseLeftDownOn);
			}
		}
		mouseLeftDown = 0;
	}
	
	private void doLeftHoldableCheck()
	{
		if(mouseLeftDown != 0)
		{
			Part part = renderPlane3D.getCursorObject();
			if(part != null)
			{
				if(part instanceof Holdable)
				{
					Holdable currentlyHold = (Holdable) part;
					if(currentlyHold != mouseLeftHoldable)
					{
						if(mouseLeftHoldable != null)
						{
							//If mouse over something else.
							renderPlane3D.componentLeftUnHold(mouseLeftHoldable);
						}
						//If something new is hold:
						mouseLeftHoldable = currentlyHold;
						renderPlane3D.componentLeftHold(mouseLeftHoldable);
					}
				}
				else
				{
					if(mouseLeftHoldable != null)
					{
						//If mouse over something non-holdable.
						renderPlane3D.componentLeftUnHold(mouseLeftHoldable);
						mouseLeftHoldable = null;
					}
				}
			}
			else
			{
				if(mouseLeftHoldable != null)
				{
					//If mouse no longer over a component.
					renderPlane3D.componentLeftUnHold(mouseLeftHoldable);
					mouseLeftHoldable = null;
				}
			}
		}
		else if(mouseLeftHoldable != null)
		{
			//If mouse has been lifted.
			renderPlane3D.componentLeftUnHold(mouseLeftHoldable);
			mouseLeftHoldable = null;
		}
	}
	
	//Right:
	
	private long mouseRightDown;
	private Part mouseRightDownOn;
	
	private void mouseRightDown()
	{
		mouseRightDown = System.currentTimeMillis();
		mouseRightDownOn = renderPlane3D.getCursorObject();
		renderPlane3D.checkToolActivationMouseDown(InputProcessor.MOUSE_RIGHT, isControl());
	}
	
	private void mouseRightUp()
	{
		Part mouseRightDownOn = renderPlane3D.getCursorObject();
		if(!renderPlane3D.toolMouseRightUp())
		{
			if(mouseRightDownOn != null)
			{
				long clickDuration = (System.currentTimeMillis() - mouseRightDown);
				//If the click was longer than a second, validate that its the intended component...
				if(clickDuration > Settings.longMousePressDuration)
				{
					if(this.mouseRightDownOn == mouseRightDownOn)
					{
						renderPlane3D.componentRightClicked(mouseRightDownOn);
					}
				}
				else
				{
					renderPlane3D.componentRightClicked(mouseRightDownOn);
				}
			}
		}
		
		mouseRightDown = 0;
	}
	
	//Middle-Mouse & Wheel:
	
	private void scrollY(int val)
	{
		if(!renderPlane3D.toolScroll(val, isControl(), isAlt()))
		{
			if(!renderPlane3D.isPrimaryToolActive())
			{
				//TBI: Only allow scrolling when in default tool mode? -> Makes sense though.
				inputProcessor.get2DController().forwardScrollingToHotbar(val);
			}
		}
	}
	
	private void numberPressed(int index)
	{
		//Fix keyboard layout alignment.
		if(--index < 0)
		{
			index = 9;
		}
		
		if(isControl())
		{
			index += 10;
		}
		
		if(!renderPlane3D.isPrimaryToolActive())
		{
			inputProcessor.get2DController().forwardNumberIndexToHotbar(index);
		}
	}
	
	public boolean isControl()
	{
		boolean control = GLFW.glfwGetKey(inputProcessor.getWindowID(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS;
		return control | GLFW.glfwGetKey(inputProcessor.getWindowID(), GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
	}
	
	private void middleMouseDown()
	{
		if(renderPlane3D.isPrimaryToolActive())
		{
			//Has the potential to change hotbar slot.
			return;
		}
		
		Part part = renderPlane3D.getCursorObject();
		if(part instanceof Connector)
		{
			part = part.getParent();
		}
		
		//TODO: Control for configuration grabbing.
		if(part == null)
		{
			inputProcessor.get2DController().forwardInfoToHotbar(null);
		}
		else
		{
			inputProcessor.get2DController().forwardInfoToHotbar(part.getInfo());
		}
	}
	
	public boolean isAlt()
	{
		boolean alt = GLFW.glfwGetKey(inputProcessor.getWindowID(), GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS;
		return alt | GLFW.glfwGetKey(inputProcessor.getWindowID(), GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
	}
	
	public boolean isLeftMouseDown()
	{
		return mouseDownLeft;
	}
}

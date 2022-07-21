package de.ecconia.java.opentung.interfaces;

import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;

import de.ecconia.java.opentung.OpenTUNG;
import de.ecconia.java.opentung.core.data.ShaderStorage;
import de.ecconia.java.opentung.core.data.SharedData;
import de.ecconia.java.opentung.core.structs.RenderPlane;
import de.ecconia.java.opentung.inputs.Controller2D;
import de.ecconia.java.opentung.inputs.InputProcessor;
import de.ecconia.java.opentung.interfaces.windows.ComponentList;
import de.ecconia.java.opentung.interfaces.windows.Hotbar;
import de.ecconia.java.opentung.interfaces.windows.PauseMenu;
import de.ecconia.java.opentung.libwrap.Matrix;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.TextureWrapper;
import de.ecconia.java.opentung.settings.Settings;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL3;
import org.lwjgl.opengl.GL30;

public class RenderPlane2D implements RenderPlane
{
	private final InputProcessor inputHandler;
	private final SharedData sharedData;
	private final ShaderStorage shaderStorage;
	
	private TextureWrapper logo;
	
	private Point indicator;
	private Hotbar hotbar;
	
	//Windows:
	
	private ComponentList componentList;
	private PauseMenu pauseMenu;
	
	private final List<Window> windows = new LinkedList<>();
	
	//Other:
	
	public long vg;
	
	private final MeshText text;
	
	//TODO: Key-Binding help text, toggled on F2
	public RenderPlane2D(InputProcessor inputHandler, SharedData sharedData)
	{
		this.sharedData = sharedData;
		this.shaderStorage = sharedData.getShaderStorage();
		this.inputHandler = inputHandler;
		
		sharedData.setRenderPlane2D(this);
		
		text = new MeshText();
	}
	
	public void addWindow(Window window)
	{
		windows.add(window);
	}
	
	@Override
	public void setup()
	{
		if(vg == 0)
		{
			//NanoVGGL3.NVG_ANTIALIAS |
			vg = NanoVGGL3.nvgCreate(NanoVGGL3.NVG_STENCIL_STROKES);
			if(vg == 0)
			{
				throw new RuntimeException("Could not init NanoVG");
			}
		}
		
		try
		{
			BufferedImage image = ImageIO.read(this.getClass().getClassLoader().getResourceAsStream("Logo1024.png"));
			logo = TextureWrapper.createLogoTexture(image);
		}
		catch(Exception e)
		{
			System.out.println("Could not load logo.");
			e.printStackTrace(System.out);
			System.exit(1);
		}
		
		//Windows:
		hotbar = new Hotbar(this, sharedData);
		sharedData.getGpuTasks().add((unused) -> {
			componentList = new ComponentList(sharedData, this, hotbar);
			windows.add(componentList);
		});
		pauseMenu = new PauseMenu(this);
		
		windows.add(pauseMenu);
		
		text.createAtlas();
		
		for(Window window : windows)
		{
			window.setup();
		}
		
		//Only set this one if this plane is ready, we don't want to receive input events before here.
		inputHandler.setController(new Controller2D(this));
	}
	
	@Override
	public void render()
	{
		Matrix mat = new Matrix();
		ShaderProgram interfaceShader = shaderStorage.getInterfaceShader();
		interfaceShader.use();
		interfaceShader.setUniformM4(1, mat.getMat());
		indicator.draw();
		
		//Draw interfaces:
		GL30.glClear(GL30.GL_STENCIL_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT);
		//TODO: "DPI"
		NanoVG.nvgBeginFrame(vg, shaderStorage.getWidth(), shaderStorage.getHeight(), 1);
		float scale = Settings.guiScale;
		NanoVG.nnvgScale(vg, scale, scale);
		hotbar.draw();
		for(Window window : windows)
		{
			window.storeRenderVisibility();
			if(window.isRenderVisibilitySet())
			{
				window.renderFrame();
			}
		}
		NanoVG.nvgEndFrame(vg);
		
		//Restore everything, cause probably changed by NanoVG - now our code takes control again.
		OpenTUNG.setOpenGLMode();
		
		GL30.glDisable(GL30.GL_DEPTH_TEST);
		hotbar.drawIcons(sharedData.getShaderStorage());
		for(Window window : windows)
		{
			if(window.isRenderVisibilitySet())
			{
				window.renderDecor(sharedData.getShaderStorage());
			}
		}
		GL30.glEnable(GL30.GL_DEPTH_TEST);
	}
	
	@Override
	public void newSize(int width, int height)
	{
		if(indicator != null)
		{
			indicator.unload();
		}
		indicator = new Point(width / 2, height / 2);
	}
	
	public float realWidth(float scale)
	{
		return shaderStorage.getWidth() / scale;
	}
	
	public float realHeight(float scale)
	{
		return shaderStorage.getHeight() / scale;
	}
	
	public Hotbar getHotbar()
	{
		return hotbar;
	}
	
	public void updatePauseMenu()
	{
		if(pauseMenu.isVisible())
		{
			pauseMenu.update(sharedData);
		}
	}
	
	public void openComponentList()
	{
		componentList.activate();
	}
	
	public void openPauseMenu()
	{
		pauseMenu.activate();
		pauseMenu.update(sharedData);
	}
	
	public boolean hasWindowOpen()
	{
		for(Window window : windows)
		{
			if(window.isVisible())
			{
				return true;
			}
		}
		return false;
	}
	
	public void closeWindows()
	{
		for(Window window : windows)
		{
			if(window.isVisible())
			{
				window.close();
			}
		}
	}
	
	public boolean keyUp(int scancode)
	{
		for(Window window : windows)
		{
			if(window.isVisible())
			{
				if(window.keyUp(scancode))
				{
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean leftMouseDown(int x, int y)
	{
		for(Window window : windows)
		{
			if(window.isVisible())
			{
				//TODO: Not exactly the right solution, accept Boolean and let null slide?
				return window.leftMouseDown(x, y);
			}
		}
		return false;
	}
	
	public boolean leftMouseUp(int x, int y)
	{
		for(Window window : windows)
		{
			if(window.isVisible())
			{
				//TODO: Not exactly the right solution, accept Boolean and let null slide?
				return window.leftMouseUp(x, y);
			}
		}
		return false;
	}
	
	public void mouseMoved(int xAbs, int yAbs, boolean leftDown)
	{
		for(Window window : windows)
		{
			if(window.isVisible())
			{
				if(window.mouseMoved(xAbs, yAbs, leftDown))
				{
					break;
				}
			}
		}
	}
	
	public void middleMouse(int x, int y)
	{
		for(Window window : windows)
		{
			if(window.isVisible())
			{
				if(window.middleMouse(x, y))
				{
					break;
				}
			}
		}
	}
	
	public MeshText getText()
	{
		return text;
	}
	
	public TextureWrapper getLogo()
	{
		return logo;
	}
	
	public void issueShutdown()
	{
		inputHandler.issueShutdown();
	}
	
	public InputProcessor getInputHandler()
	{
		return inputHandler;
	}
	
	public SharedData getSharedData()
	{
		return sharedData;
	}
	
	private static class Point
	{
		private final int vaoID;
		private final int vboID;
		
		public Point(int centerX, int centerY)
		{
			vaoID = GL30.glGenVertexArrays();
			vboID = GL30.glGenBuffers();
			
			GL30.glBindVertexArray(vaoID);
			
			GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, vboID);
			GL30.glBufferData(GL30.GL_ARRAY_BUFFER, new float[]{centerX, centerY, 1, 1, 0}, GL30.GL_STATIC_DRAW);
			
			//Position:
			GL30.glVertexAttribPointer(0, 2, GL30.GL_FLOAT, false, 5 * Float.BYTES, 0);
			GL30.glEnableVertexAttribArray(0);
			//Color
			GL30.glVertexAttribPointer(1, 3, GL30.GL_FLOAT, false, 5 * Float.BYTES, 2 * Float.BYTES);
			GL30.glEnableVertexAttribArray(1);
			
			//Cleanup:
			GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0);
		}
		
		public void draw()
		{
			GL30.glBindVertexArray(vaoID);
			GL30.glPointSize(3f);
			GL30.glDrawArrays(GL30.GL_POINTS, 0, 1);
		}
		
		public void unload()
		{
			GL30.glDeleteBuffers(vboID);
			GL30.glDeleteVertexArrays(vaoID);
		}
	}
}

package de.ecconia.java.opentung.interfaces;

import de.ecconia.java.opentung.OpenTUNG;
import de.ecconia.java.opentung.RenderPlane;
import de.ecconia.java.opentung.SharedData;
import de.ecconia.java.opentung.inputs.Controller2D;
import de.ecconia.java.opentung.inputs.InputProcessor;
import de.ecconia.java.opentung.interfaces.windows.ComponentList;
import de.ecconia.java.opentung.interfaces.windows.Hotbar;
import de.ecconia.java.opentung.interfaces.windows.PauseMenu;
import de.ecconia.java.opentung.libwrap.Matrix;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.TextureWrapper;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;
import de.ecconia.java.opentung.savefile.Saver;
import de.ecconia.java.opentung.settings.Settings;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL3;
import org.lwjgl.opengl.GL30;

public class RenderPlane2D implements RenderPlane
{
	private final Matrix projectionMatrix = new Matrix();
	private final InputProcessor inputHandler;
	private final SharedData sharedData;
	
	private ShaderProgram interfaceShader;
	private ShaderProgram componentIconShader;
	private ShaderProgram labelShader;
	
	private TextureWrapper logo;
	
	private final GenericVAO iconPlane = new GenericVAO(new float[]{
			-1, -1, 0, 0, // L T
			-1, +1, 0, 1, // L B
			+1, -1, 1, 0, // R T
			+1, +1, 1, 1, // R B
	}, new short[]{
			0, 1, 2,
			1, 3, 2,
	})
	{
		@Override
		protected void init()
		{
			//Position:
			GL30.glVertexAttribPointer(0, 2, GL30.GL_FLOAT, false, 4 * Float.BYTES, 0);
			GL30.glEnableVertexAttribArray(0);
			//TextureCoord:
			GL30.glVertexAttribPointer(1, 2, GL30.GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
			GL30.glEnableVertexAttribArray(1);
		}
	};
	
	private Point indicator;
	
	private Hotbar hotbar;
	private ComponentList componentList;
	private PauseMenu pauseMenu;
	
	private boolean showComponentList;
	private boolean showPauseMenu;
	
	public long vg;
	private int width, height;
	
	private final MeshText text;
	
	public RenderPlane2D(InputProcessor inputHandler, SharedData sharedData)
	{
		this.sharedData = sharedData;
		this.inputHandler = inputHandler;
		inputHandler.setController(new Controller2D(this));
		
		text = new MeshText();
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
			componentList = new ComponentList(this, hotbar);
		});
		pauseMenu = new PauseMenu(this);
		
		interfaceShader = new ShaderProgram("interfaceShader");
		componentIconShader = new ShaderProgram("interfaces/componentIconShader");
		labelShader = new ShaderProgram("interfaces/labelShader");
		
		text.createAtlas();
		
		pauseMenu.setup();
	}
	
	@Override
	public void render()
	{
		interfaceShader.use();
		Matrix mat = new Matrix();
		interfaceShader.setUniform(1, mat.getMat());
		indicator.draw();
		
		//Draw interfaces:
		GL30.glClear(GL30.GL_STENCIL_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT);
		//TODO: "DPI"
		NanoVG.nvgBeginFrame(vg, width, height, 1);
		float scale = Settings.guiScale;
		NanoVG.nnvgScale(vg, scale, scale);
		hotbar.draw();
		boolean tsShowComponentList = showComponentList;
		if(tsShowComponentList)
		{
			componentList.draw();
		}
		boolean tsShowPauseMenu = showPauseMenu;
		if(tsShowPauseMenu)
		{
			pauseMenu.renderFrame();
		}
		NanoVG.nvgEndFrame(vg);
		
		//Restore everything, cause dunno.
		OpenTUNG.setOpenGLMode();
		
		GL30.glDisable(GL30.GL_DEPTH_TEST);
		hotbar.drawIcons(componentIconShader, iconPlane);
		if(tsShowComponentList)
		{
			componentList.drawIcons(componentIconShader, iconPlane);
		}
		if(tsShowPauseMenu)
		{
			pauseMenu.renderDecor(componentIconShader, iconPlane);
		}
		
		GL30.glEnable(GL30.GL_DEPTH_TEST);
	}
	
	@Override
	public void newSize(int width, int height)
	{
		this.width = width;
		this.height = height;
		
		projectionMatrix.interfaceMatrix(width, height);
		float[] pM = projectionMatrix.getMat();
		componentIconShader.use();
		componentIconShader.setUniform(0, pM);
		interfaceShader.use();
		interfaceShader.setUniform(0, pM);
		labelShader.use();
		labelShader.setUniform(0, pM);
		if(indicator != null)
		{
			indicator.unload();
		}
		indicator = new Point(width / 2, height / 2);
	}
	
	public float realWidth(float scale)
	{
		return width / scale;
	}
	
	public float realHeight(float scale)
	{
		return height / scale;
	}
	
	public Hotbar getHotbar()
	{
		return hotbar;
	}
	
	public void openComponentList()
	{
		showComponentList = true;
	}
	
	public void openPauseMenu()
	{
		showPauseMenu = true;
	}
	
	public boolean hasWindowOpen()
	{
		return showComponentList || showPauseMenu;
	}
	
	public void closeWindows()
	{
		showComponentList = false;
		if(showPauseMenu)
		{
			pauseMenu.close();
		}
		showPauseMenu = false;
	}
	
	public boolean leftMouseDown(int x, int y)
	{
		if(!showComponentList)
		{
			return false;
		}
		else
		{
			return componentList.leftMouseDown(x, y);
		}
	}
	
	public boolean leftMouseUp(int x, int y)
	{
		if(showComponentList)
		{
			return componentList.leftMouseUp(x, y);
		}
		else if(showPauseMenu)
		{
			return pauseMenu.leftMouseUp(x, y);
		}
		else
		{
			return false;
		}
	}
	
	public boolean toggleComponentList()
	{
		if(showComponentList)
		{
			componentList.abort();
			showComponentList = false;
		}
		else
		{
			showComponentList = true;
		}
		return showComponentList;
	}
	
	public void mouseMoved(int xAbs, int yAbs, boolean leftDown)
	{
		if(showPauseMenu)
		{
			pauseMenu.mouseMoved(xAbs, yAbs);
		}
		else if(showComponentList)
		{
			if(leftDown)
			{
				componentList.mouseDragged(xAbs, yAbs);
			}
			else
			{
				componentList.mouseMoved(xAbs, yAbs);
			}
		}
	}
	
	public void middleMouse(int x, int y)
	{
		if(showComponentList)
		{
			componentList.middleMouse(x, y);
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
	
	public ShaderProgram getLabelShader()
	{
		return labelShader;
	}
	
	public void issueShutdown()
	{
		inputHandler.issueShutdown();
	}
	
	public boolean prepareSaving()
	{
		if(sharedData.isSaving())
		{
			return false;
		}
		sharedData.setSaving();
		AtomicInteger pauseArrived = new AtomicInteger();
		sharedData.getRenderPlane3D().prepareSaving(pauseArrived);
		while(pauseArrived.get() != 2)
		{
			try
			{
				Thread.sleep(10);
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		
		//Arrived at pause state on all relevant threads.
		return true;
	}
	
	public void postSave()
	{
		sharedData.getRenderPlane3D().postSave();
		sharedData.unsetSaving();
	}
	
	public void performSave()
	{
		Saver.save(sharedData.getBoardUniverse());
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

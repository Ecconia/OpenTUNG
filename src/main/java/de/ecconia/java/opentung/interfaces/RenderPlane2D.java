package de.ecconia.java.opentung.interfaces;

import de.ecconia.java.opentung.OpenTUNG;
import de.ecconia.java.opentung.RenderPlane;
import de.ecconia.java.opentung.SharedData;
import de.ecconia.java.opentung.inputs.Controller2D;
import de.ecconia.java.opentung.inputs.InputProcessor;
import de.ecconia.java.opentung.libwrap.Matrix;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;
import de.ecconia.java.opentung.settings.Settings;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL3;
import org.lwjgl.opengl.GL30;

public class RenderPlane2D implements RenderPlane
{
	private final Matrix projectionMatrix = new Matrix();
	
	private ShaderProgram interfaceShader;
	private ShaderProgram componentIconShader;
	
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
	
	private boolean showComponentList;
	
	public long vg;
	private int width, height;
	
	public RenderPlane2D(InputProcessor inputHandler, SharedData sharedData)
	{
		inputHandler.setController(new Controller2D(this));
		hotbar = new Hotbar(this, sharedData);
		componentList = new ComponentList(this, hotbar);
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
		
		interfaceShader = new ShaderProgram("interfaceShader");
		componentIconShader = new ShaderProgram("interfaces/componentIconShader");
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
		boolean threadSafeShowList = showComponentList;
		if(threadSafeShowList)
		{
			componentList.draw();
		}
		NanoVG.nvgEndFrame(vg);
		
		//Restore everything, cause dunno.
		OpenTUNG.setOpenGLMode();
		
		GL30.glDisable(GL30.GL_DEPTH_TEST);
		hotbar.drawIcons(componentIconShader, iconPlane);
		if(threadSafeShowList)
		{
			componentList.drawIcons(componentIconShader, iconPlane);
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
	
	public boolean hasWindowOpen()
	{
		return showComponentList;
	}
	
	public void closeWindow()
	{
		//TODO: Handle other windows.
		showComponentList = false;
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
	
	public void mouseDragged(int xAbs, int yAbs)
	{
		if(showComponentList)
		{
			componentList.mouseDragged(xAbs, yAbs);
		}
	}
	
	public void middleMouse(int x, int y)
	{
		if(showComponentList)
		{
			componentList.middleMouse(x, y);
		}
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

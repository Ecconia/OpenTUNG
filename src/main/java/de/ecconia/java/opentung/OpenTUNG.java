package de.ecconia.java.opentung;

import de.ecconia.java.opentung.libwrap.InputHandler;
import de.ecconia.java.opentung.libwrap.Matrix;
import de.ecconia.java.opentung.libwrap.SWindowWrapper;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.TextureWrapper;
import de.ecconia.java.opentung.scomponents.SimpleBlotterModel;
import de.ecconia.java.opentung.scomponents.SimpleDynamicBoard;
import de.ecconia.java.opentung.scomponents.SimpleInverterModel;
import de.ecconia.java.opentung.scomponents.SimplePeg;
import org.lwjgl.Version;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.awt.*;

public class OpenTUNG
{
	private static final Matrix projection = new Matrix();
	private static final Matrix model = new Matrix();
	
	private static int fps = 30;
	
	private static InputHandler inputHandler;
	private static Camera camera;
	
	public static void main(String[] args)
	{
		System.out.println("LWJGL version: " + Version.getVersion());
		
		SWindowWrapper window = new SWindowWrapper(500, 500, "OpenTUNG FPS: ?");
		inputHandler = new InputHandler(window.getID());
		window.place();
		window.setVsync(fps == 0);
		
		//OpenGL:
		GL.createCapabilities();
		System.out.println("OpenGL version: " + GL11.glGetString(GL11.GL_VERSION));
		
		init();
		
		long past = System.currentTimeMillis();
		int finishedRenderings = 0;
		
		long frameDuration = 1000L / (long) fps;
		long lastFinishedRender = System.currentTimeMillis();
		
		while(!window.shouldClose())
		{
			Dimension newSize = window.getNewDimension();
			if(newSize != null)
			{
				GL30.glViewport(0, 0, newSize.width, newSize.height);
				projection.perspective(45f, (float) newSize.width / (float) newSize.height, 0.1f, 100000f);
			}
			
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT);
			
			render();
			
			window.update();
			
			//FPS counting:
			finishedRenderings++;
			long now = System.currentTimeMillis();
			if(now - past > 1000)
			{
				past = now;
				window.setTitle("OpenTUNG FPS: " + finishedRenderings);
				finishedRenderings = 0;
			}
			
			//FPS limiting:
			if(fps != 0)
			{
				long currentTime = System.currentTimeMillis();
				long timeToWait = frameDuration - (currentTime - lastFinishedRender);
				if(timeToWait > 0)
				{
					try
					{
						Thread.sleep(timeToWait);
					}
					catch(InterruptedException e)
					{
						e.printStackTrace(); //Should never happen though.
					}
				}
				lastFinishedRender = System.currentTimeMillis();
			}
		}
		
		inputHandler.stop();
	}
	
	private static ShaderProgram program;
	private static SimpleInverterModel inverter;
	private static SimpleBlotterModel blotter;
	private static SimplePeg peg;
	
	private static TextureWrapper boardTexture;
	private static ShaderProgram dynamicBoardShader;
	private static SimpleDynamicBoard dBoard;
	
	private static void init()
	{
		GL11.glClearColor(1f / 255f * 54f, 1f / 255f * 57f, 1f / 255f * 63f, 0.0f);
		GL30.glEnable(GL30.GL_DEPTH_TEST);
		
		program = new ShaderProgram("basicShader");
		dynamicBoardShader = new ShaderProgram("dynamicBoardShader");
		
		boardTexture = new TextureWrapper();
		
		inverter = new SimpleInverterModel();
		blotter = new SimpleBlotterModel();
		peg = new SimplePeg();
		dBoard = new SimpleDynamicBoard();
		
		camera = new Camera(inputHandler);
		
		projection.perspective(45f, (float) 500 / (float) 500, 0.1f, 100000f);
	}
	
	private static float color = 0;
	
	private static void render()
	{
		float[] view = camera.getMatrix();

		dynamicBoardShader.use();
		dynamicBoardShader.setUniform(0, projection.getMat());
		dynamicBoardShader.setUniform(1, view);
		dynamicBoardShader.setUniform(5, view);

		Color c = Color.getHSBColor(color, 1.0f, 1.0f); //Color.white;
		color += 0.01f;
		if(color > 1f)
		{
			color = 0f;
		}
		
		model.identity();
		dynamicBoardShader.setUniform(2, model.getMat());
		dynamicBoardShader.setUniformV2(3, new float[] {10f, 10f});
		dynamicBoardShader.setUniformV4(4, new float[] {(float) c.getRed() / 255f,(float) c.getGreen() / 255f,(float) c.getBlue() / 255f, 1f});
		
		dBoard.draw();
		
		float h = 0.075f + 0.15f;
		
		program.use();
		program.setUniform(0, projection.getMat());
		program.setUniform(1, view);
		program.setUniform(3, view);
		
		model.identity();
		model.translate(0.6f * (float) -1 + 0.15f, h, 0.15f);
		program.setUniform(2, model.getMat());
		inverter.draw();
		
		model.identity();
		model.translate(0.6f * (float) 0 + 0.15f, h, 0.15f);
		program.setUniform(2, model.getMat());
		blotter.draw();
		
		model.identity();
		model.translate(0.6f * (float) 1 + 0.15f, h, 0.15f);
		program.setUniform(2, model.getMat());
		peg.draw();
	}
}

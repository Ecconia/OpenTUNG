package de.ecconia.java.opentung;

import de.ecconia.java.opentung.crapinterface.RenderPlane2D;
import de.ecconia.java.opentung.inputs.InputProcessor;
import de.ecconia.java.opentung.libwrap.SWindowWrapper;
import org.lwjgl.Version;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.awt.*;

public class OpenTUNG
{
	private static int fps = 30;
	private static InputProcessor inputHandler;
	
	private static RenderPlane2D interactables;
	private static RenderPlane3D worldView;
	
	public static void main(String[] args)
	{
		try
		{
			System.out.println("LWJGL version: " + Version.getVersion());
			
			SWindowWrapper window = new SWindowWrapper(500, 500, "OpenTUNG FPS: ?");
			inputHandler = new InputProcessor(window.getID());
			window.place();
			window.setVsync(fps == 0);
			
			//OpenGL:
			GL.createCapabilities();
			System.out.println("OpenGL version: " + GL11.glGetString(GL11.GL_VERSION));
			
			init();
			
			long past = System.currentTimeMillis();
			int finishedRenderings = 0;
			
			long frameDuration = fps != 0 ? 1000L / (long) fps : 1;
			long lastFinishedRender = System.currentTimeMillis();
			
			while(!window.shouldClose())
			{
				Dimension newSize = window.getNewDimension();
				if(newSize != null)
				{
					GL30.glViewport(0, 0, newSize.width, newSize.height);
					interactables.newSize(newSize.width, newSize.height);
					worldView.newSize(newSize.width, newSize.height);
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
		catch(Exception e)
		{
			e.printStackTrace();
			inputHandler.stop();
			System.exit(1); //Throw 1;
		}
	}
	
	private static void init()
	{
		GL11.glClearColor(1f / 255f * 54f, 1f / 255f * 57f, 1f / 255f * 63f, 0.0f);
		GL30.glEnable(GL30.GL_DEPTH_TEST);
		
		Quaternion q = new Quaternion();
		interactables = new RenderPlane2D(inputHandler, q);
		interactables.setup();
		worldView = new RenderPlane3D(inputHandler, q);
		worldView.setup();
	}
	
	private static void render()
	{
		worldView.render();
		interactables.render();
	}
}

package de.ecconia.java.opentung;

import de.ecconia.java.opentung.libwrap.InputHandler;
import de.ecconia.java.opentung.libwrap.Location;
import de.ecconia.java.opentung.libwrap.Matrix;
import de.ecconia.java.opentung.libwrap.SWindowWrapper;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.VBOWrapper;
import org.lwjgl.Version;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

public class OpenTUNG
{
	private static final Matrix projection = new Matrix();
	private static final Matrix view = new Matrix();
	private static final Matrix model = new Matrix();
	
	public static void main(String[] args)
	{
		System.out.println("Version: " + Version.getVersion());
		
		SWindowWrapper window = new SWindowWrapper(500, 500, "OpenTUNG FPS: ?");
		InputHandler handler = new InputHandler(window.getID());
		window.place();
		
		//OpenGL:
		GL.createCapabilities();
		System.out.println("OpenGL version: " + GL11.glGetString(GL11.GL_VERSION));
		
		ShaderProgram program = new ShaderProgram("basicShader");
		program.use();
		
		VBOWrapper vbo = new VBOWrapper(program);
		
		projection.perspective(45f, (float) 500 / (float) 500, 0.1f, 100000f);
		
		GL11.glClearColor(0.2f, 0.2f, 0.2f, 0.0f);
		
		long past = System.currentTimeMillis();
		int finishedRenderings = 0;
		
		while(!window.shouldClose())
		{
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
			
			Location loc = handler.getCurrentPosition();
			loc.print();
			//Setting up view matrix:
			view.identity();
			view.rotate(loc.getNeck(), 1, 0, 0); //Neck
			view.rotate(loc.getRotation(), 0, 1, 0); //Rotation
			view.translate(loc.getX(), -loc.getY(), loc.getZ());
			
			program.use();
			program.setUniform(0, projection.getMat());
			program.setUniform(1, view.getMat());
			
			model.identity();
			model.translate(0, 0, 0);
			
			program.setUniform(2, model.getMat());
			
			vbo.use();
			vbo.draw();
			
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
		}
		
		handler.stop();
	}
}

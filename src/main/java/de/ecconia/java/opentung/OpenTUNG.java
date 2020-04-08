package de.ecconia.java.opentung;

import de.ecconia.java.opentung.libwrap.InputHandler;
import de.ecconia.java.opentung.libwrap.Location;
import de.ecconia.java.opentung.libwrap.Matrix;
import de.ecconia.java.opentung.libwrap.SWindowWrapper;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.scomponents.SimpleBlotterModel;
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
		
		SimpleInverterModel inverter = new SimpleInverterModel();
		SimpleBlotterModel blotter = new SimpleBlotterModel();
		SimplePeg peg = new SimplePeg();
		
		projection.perspective(45f, (float) 500 / (float) 500, 0.1f, 100000f);
		
		GL11.glClearColor(1f / 255f * 54f, 1f / 255f * 57f, 1f / 255f * 63f, 0.0f);
		GL30.glEnable(GL30.GL_DEPTH_TEST);
		
		long past = System.currentTimeMillis();
		int finishedRenderings = 0;
		
		while(!window.shouldClose())
		{
			Dimension newSize = window.getNewDimension();
			if(newSize != null)
			{
				GL30.glViewport(0, 0, newSize.width, newSize.height);
				projection.perspective(45f, (float) newSize.width / (float) newSize.height, 0.1f, 100000f);
			}
			
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT);
			
			Location loc = handler.getCurrentPosition();
//			loc.print();
			//Setting up view matrix:
			view.identity();
			view.rotate(loc.getNeck(), 1, 0, 0); //Neck
			view.rotate(loc.getRotation(), 0, 1, 0); //Rotation
			view.translate(loc.getX(), -loc.getY(), loc.getZ());
			
			program.use();
			program.setUniform(0, projection.getMat());
			program.setUniform(1, view.getMat());
			program.setUniform(3, view.getMat());
			
			model.identity();
			model.translate(0.4f * (float) 0, 0, 0);
			program.setUniform(2, model.getMat());
			inverter.draw();
			
			model.identity();
			model.translate(0.4f * (float) 1, 0, 0);
			program.setUniform(2, model.getMat());
			blotter.draw();
			
			model.identity();
			model.translate(0.4f * (float) 2, 0, 0);
			program.setUniform(2, model.getMat());
			peg.draw();
			
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

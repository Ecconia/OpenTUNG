package de.ecconia.java.opentung;

import de.ecconia.java.opentung.libwrap.KeyboardHandler;
import de.ecconia.java.opentung.libwrap.SWindowWrapper;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.VBOWrapper;
import org.lwjgl.Version;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

public class OpenTUNG
{
	public static void main(String[] args)
	{
		System.out.println("Version: " + Version.getVersion());
		
		SWindowWrapper window = new SWindowWrapper(500, 500, "OpenTUNG FPS: ?");
		KeyboardHandler handler = new KeyboardHandler(window.getID());
		window.place();
		
		//OpenGL:
		GL.createCapabilities();
		System.out.println("OpenGL version: " + GL11.glGetString(GL11.GL_VERSION));

		ShaderProgram program = new ShaderProgram("basicShader");
		program.use();

		VBOWrapper vbo = new VBOWrapper(program);

		GL11.glClearColor(0.2f, 0.2f, 0.2f, 0.0f);

		long past = System.currentTimeMillis();
		int finishedRenderings = 0;

		while(!window.shouldClose())
		{
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

			program.use();
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

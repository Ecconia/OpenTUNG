package de.ecconia.java.opentung.settings.keybinds.manager;

import de.ecconia.java.opentung.settings.keybinds.GLFWKeyMapper;
import de.ecconia.java.opentung.util.logging.LogStreamHandler;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.system.MemoryUtil;

public class KeyCodeGrabber
{
	private final long windowID;
	
	public KeyCodeGrabber(KeybindingManager manager)
	{
		GLFWErrorCallback.createPrint(LogStreamHandler.outputStreamBypass).set();
		if(!GLFW.glfwInit())
		{
			throw new IllegalStateException("Unable to initialize GLFW");
		}
		
		GLFW.glfwDefaultWindowHints();
		GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
		GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
		
		GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
		GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
		GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
		
		windowID = GLFW.glfwCreateWindow(100, 100, "Grabbing-Window", MemoryUtil.NULL, MemoryUtil.NULL);
		if(windowID == MemoryUtil.NULL)
		{
			throw new RuntimeException("Failed to create the GLFW window");
		}
		
		GLFW.glfwShowWindow(windowID);
		
		GLFW.glfwSetKeyCallback(windowID, (windowIDC, key, scancode, action, mods) -> {
			if(action == GLFW.GLFW_PRESS)
			{
				String glfwName = null;
				if(key != GLFW.GLFW_KEY_UNKNOWN)
				{
					glfwName = GLFWKeyMapper.keyMapping[key];
				}
				String osCharacter = GLFW.glfwGetKeyName(GLFW.GLFW_KEY_UNKNOWN, scancode);
				manager.keyboardInput(scancode, glfwName, osCharacter);
			}
		});
		
		GLFW.glfwSetWindowFocusCallback(windowID, (unused, focused) -> {
			manager.inputActive(focused);
		});
	}
	
	public boolean run()
	{
		GLFW.glfwPollEvents();
		return GLFW.glfwWindowShouldClose(windowID);
	}
	
	public void grabFocus()
	{
		GLFW.glfwFocusWindow(windowID);
	}
	
	public void close()
	{
		GLFW.glfwSetWindowShouldClose(windowID, true);
	}
}

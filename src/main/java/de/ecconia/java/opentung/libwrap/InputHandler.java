package de.ecconia.java.opentung.libwrap;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;

public class InputHandler
{
	private final Thread inputThread;
	
	private float x, y, z;
	private float rotation;
	private float neck;
	
	private Location currentPosition;
	
	private boolean w = false;
	private boolean a = false;
	private boolean s = false;
	private boolean d = false;
	private boolean shift = false;
	private boolean space = false;
	
	private int mouseXLast;
	private int mouseYLast;
	private int mouseXChange;
	private int mouseYChange;
	
	private boolean captured = false;
	
	//TODO: Poll the WASD** keys manually, only calc the mouse delta, once the movement needs to be calcualted.
	public InputHandler(long windowID)
	{
		z -= 3;
		
		inputThread = new Thread(() -> {
			
			while(!Thread.currentThread().isInterrupted())
			{
				GLFW.glfwPollEvents();
				
				doMovement();
				currentPosition = new Location(x, y, z, rotation, neck);
				
				//Remove the processed delta
				mouseXChange = 0;
				mouseYChange = 0;
				
				try
				{
					Thread.sleep(10);
				}
				catch(InterruptedException e)
				{
					break;
				}
			}
			
			System.out.println("Keybaord thread shutted down.");
		}, "KeybaordThread");
		
		GLFW.glfwSetCursorPosCallback(windowID, (windowIDC, x, y) -> {
			if(captured)
			{
				int nx = (int) x;
				int ny = (int) y;
				
				mouseXChange = mouseXLast - nx;
				mouseYChange = mouseYLast - ny;
				
				mouseXLast = nx;
				mouseYLast = ny;
			}
			else
			{
				mouseXLast = (int) x;
				mouseYLast = (int) y;
			}
		});
		
		GLFW.glfwSetMouseButtonCallback(windowID, (windowIDC, button, action, mods) -> {
			if(button == GLFW.GLFW_MOUSE_BUTTON_1)
			{
				if(action == GLFW.GLFW_RELEASE)
				{
					captured = true;
					GLFW.glfwSetInputMode(windowID, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
				}
			}
		});
		
		GLFW.glfwSetKeyCallback(windowID, (window1, key, scancode, action, mods) -> {
			if(action == GLFW.GLFW_REPEAT)
			{
				return;
			}
			
			if(key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_RELEASE)
			{
				if(captured)
				{
					GLFW.glfwSetInputMode(windowID, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
					captured = false;
				}
				else
				{
					System.out.println("Should quit...");
					GLFW.glfwSetWindowShouldClose(window1, true);
				}
			}
			
			if(action == GLFW.GLFW_PRESS)
			{
				switch(key)
				{
					case GLFW.GLFW_KEY_W:
						w = true;
						break;
					case GLFW.GLFW_KEY_A:
						a = true;
						break;
					case GLFW.GLFW_KEY_S:
						s = true;
						break;
					case GLFW.GLFW_KEY_D:
						d = true;
						break;
					case GLFW.GLFW_KEY_LEFT_SHIFT:
						shift = true;
						break;
					case GLFW.GLFW_KEY_SPACE:
						space = true;
						break;
				}
			}
			else if(action == GLFW.GLFW_RELEASE)
			{
				switch(key)
				{
					case GLFW.GLFW_KEY_W:
						w = false;
						break;
					case GLFW.GLFW_KEY_A:
						a = false;
						break;
					case GLFW.GLFW_KEY_S:
						s = false;
						break;
					case GLFW.GLFW_KEY_D:
						d = false;
						break;
					case GLFW.GLFW_KEY_LEFT_SHIFT:
						shift = false;
						break;
					case GLFW.GLFW_KEY_SPACE:
						space = false;
						break;
				}
			}
		});
		
		inputThread.start();
	}
	
	private void doMovement()
	{
		//Mouse:
		
		this.rotation -= (float) mouseXChange * 0.333f;
		while(this.rotation > 360)
		{
			this.rotation -= 360;
		}
		while(this.rotation < 0)
		{
			this.rotation += 360;
		}
		this.neck -= (float) mouseYChange * 0.333f;
		while(this.neck > 90)
		{
			this.neck = 90;
		}
		while(this.neck < -90)
		{
			this.neck = -90;
		}
		
		//Keyboard:
		
		int direction = 0;
		boolean l = this.a;
		boolean r = this.d;
		if(l && r)
		{
			l = r = false;
		}
		boolean f = this.w;
		boolean b = this.s;
		if(f && b)
		{
			f = b = false;
		}
		boolean u = this.space;
		boolean d = this.shift;
		if(u && d)
		{
			u = d = false;
		}
		
		if(f)
		{
			direction += 180;
		}
		
		if(r)
		{
			if(f)
			{
				direction += 45;
			}
			else if(b)
			{
				direction -= 45;
			}
			else
			{
				direction -= 90;
			}
		}
		else if(l)
		{
			if(f)
			{
				direction -= 45;
			}
			else if(b)
			{
				direction += 45;
			}
			else
			{
				direction += 90;
			}
		}
		
		final float distance = 0.05f;
		if(l || r || f || b)
		{
			walkInDirection(direction, distance);
		}
		
		if(u)
		{
			levitate(distance);
		}
		else if(d)
		{
			levitate(-distance);
		}
	}
	
	public Location getCurrentPosition()
	{
		return currentPosition;
	}
	
	private void levitate(float amount)
	{
		this.y += amount;
	}
	
	private void walkInDirection(int direction, float distance)
	{
		float dir = (float) ((this.rotation + direction) * Math.PI / 180D);
		this.x += distance * Math.sin(dir);
		this.z -= distance * Math.cos(dir);
	}
	
	public void stop()
	{
		inputThread.interrupt();
	}
}

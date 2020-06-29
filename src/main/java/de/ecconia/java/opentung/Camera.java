package de.ecconia.java.opentung;

import de.ecconia.java.opentung.inputs.InputConsumer;
import de.ecconia.java.opentung.inputs.InputProcessor;
import de.ecconia.java.opentung.libwrap.Location;
import de.ecconia.java.opentung.libwrap.Matrix;
import org.lwjgl.glfw.GLFW;

public class Camera implements InputConsumer
{
	private float x, y, z;
	private float rotation;
	private float neck;
	
	private boolean mousedown;
	
	//Thread-safe cause only one accessor and one consumer:
	private Location currentPosition;
	
	private final Matrix view = new Matrix();
	private final InputProcessor handler;
	private final RightClickReceiver rightClickReceiver;
	
	public Camera(InputProcessor handler, RightClickReceiver rightClickReceiver)
	{
		this.handler = handler;
		this.rightClickReceiver = rightClickReceiver;
		
		handler.registerClickConsumer(this);
		
		x += Settings.playerSpawnX;
		y += Settings.playerSpawnY;
		z += Settings.playerSpawnZ;
		
		currentPosition = new Location(x, y, z, rotation, neck);
	}
	
	@Override
	public boolean up(int type, int x, int y)
	{
		if(type == GLFW.GLFW_MOUSE_BUTTON_1)
		{
			if(!handler.isCaptured(this))
			{
				//Canvas got clicked, capture mode.
				handler.captureMode(this);
				return true;
			}
		}
		else if(type == GLFW.GLFW_MOUSE_BUTTON_2)
		{
			if(handler.isCaptured(this))
			{
				checkMouse(false);
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public boolean down(int type, int x, int y)
	{
		if(type == GLFW.GLFW_MOUSE_BUTTON_2)
		{
			if(handler.isCaptured(this))
			{
				checkMouse(true);
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public boolean escapeIssued()
	{
		if(handler.isCaptured(this))
		{
			handler.captureMode(null);
			checkMouse(false);
			return true;
		}
		return false;
	}
	
	public float[] getMatrix()
	{
		Location loc = currentPosition;
		if(loc != null)
		{
			view.identity();
			view.rotate(loc.getNeck(), 1, 0, 0); //Neck
			view.rotate(loc.getRotation(), 0, 1, 0); //Rotation
			view.translate(-loc.getX(), -loc.getY(), -loc.getZ());
		}
		
		return view.getMat();
	}
	
	@Override
	public void unfocus()
	{
		if(handler.isCaptured(this))
		{
			handler.captureMode(null);
			checkMouse(false);
		}
	}
	
	private void checkMouse(boolean target)
	{
		if(!target)
		{
			if(mousedown)
			{
				mousedown = false;
				rightClickReceiver.rightUp();
			}
		}
		else
		{
			if(mousedown)
			{
				System.out.println("Right click already marked down 3D-Pane, but got downed again.");
			}
			else
			{
				mousedown = true;
				rightClickReceiver.rightDown();
			}
		}
	}
	
	@Override
	public void movement(float mx, float my, boolean l, boolean r, boolean f, boolean b, boolean u, boolean d, boolean control)
	{
		float rotationSpeed = Settings.playerRotationSpeed;
		float flySpeed = Settings.playerFlySpeed;
		if(control)
		{
			flySpeed = Settings.playerFastFlySpeed;
			rotationSpeed = Settings.playerFastRotationSpeed;
		}
		
		//Mouse:
		this.rotation += (float) mx * rotationSpeed;
		while(this.rotation > 360)
		{
			this.rotation -= 360;
		}
		while(this.rotation < 0)
		{
			this.rotation += 360;
		}
		this.neck += (float) my * rotationSpeed;
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
		if(l && r)
		{
			l = r = false;
		}
		if(f && b)
		{
			f = b = false;
		}
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
		else if(l)
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
		
		if(l || r || f || b)
		{
			walkInDirection(direction, flySpeed);
		}
		
		if(u)
		{
			levitate(flySpeed);
		}
		else if(d)
		{
			levitate(-flySpeed);
		}
		
		currentPosition = new Location(x, y, z, rotation, neck);
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
	
	public interface RightClickReceiver
	{
		void rightUp();
		
		void rightDown();
	}
}

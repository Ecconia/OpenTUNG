package de.ecconia.java.opentung;

import de.ecconia.java.opentung.libwrap.Location;
import de.ecconia.java.opentung.libwrap.Matrix;
import de.ecconia.java.opentung.math.Vector3;
import de.ecconia.java.opentung.settings.Settings;

public class Camera
{
	private float x, y, z;
	private float rotation;
	private float neck;
	
	//Thread-safe cause only one accessor and one consumer:
	private Location currentPosition;
	//Location on the graphic thread:
	private Location currentPositionLock;
	
	private final Matrix view = new Matrix();
	
	public Camera()
	{
		x += Settings.playerSpawnX;
		y += Settings.playerSpawnY;
		z += Settings.playerSpawnZ;
		
		currentPosition = new Location(x, y, z, rotation, neck);
	}
	
	public float[] getMatrix()
	{
		if(currentPositionLock != null)
		{
			view.identity();
			view.rotate(currentPositionLock.getNeck(), 1, 0, 0); //Neck
			view.rotate(currentPositionLock.getRotation(), 0, 1, 0); //Rotation
			view.translate(-currentPositionLock.getX(), -currentPositionLock.getY(), -currentPositionLock.getZ());
		}
		
		return view.getMat();
	}
	
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
	
	public Vector3 getPosition()
	{
		return new Vector3(currentPositionLock.getX(), currentPositionLock.getY(), currentPositionLock.getZ());
	}
	
	public float getNeck()
	{
		return currentPositionLock.getNeck();
	}
	
	public float getRotation()
	{
		return currentPositionLock.getRotation();
	}
	
	public void lockLocation()
	{
		currentPositionLock = currentPosition;
	}
}

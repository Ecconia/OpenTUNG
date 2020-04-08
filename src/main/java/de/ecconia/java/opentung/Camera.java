package de.ecconia.java.opentung;

import de.ecconia.java.opentung.libwrap.InputHandler;
import de.ecconia.java.opentung.libwrap.Location;
import de.ecconia.java.opentung.libwrap.Matrix;

public class Camera
{
	private final Matrix view = new Matrix();
	private final InputHandler handler;
	
	public Camera(InputHandler handler)
	{
		this.handler = handler;
	}
	
	public float[] getMatrix()
	{
		Location loc = handler.getCurrentPosition();
		if(loc != null)
		{
			view.identity();
			view.rotate(loc.getNeck(), 1, 0, 0); //Neck
			view.rotate(loc.getRotation(), 0, 1, 0); //Rotation
			view.translate(loc.getX(), -loc.getY(), loc.getZ());
		}
		
		return view.getMat();
	}
}

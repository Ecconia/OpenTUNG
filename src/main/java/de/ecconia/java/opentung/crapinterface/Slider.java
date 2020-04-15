package de.ecconia.java.opentung.crapinterface;

import de.ecconia.java.opentung.libwrap.ColorVec;
import de.ecconia.java.opentung.libwrap.Matrix;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;
import de.ecconia.java.opentung.libwrap.vaos.InterfaceVAO;

public class Slider
{
	private GenericVAO panel;
	private GenericVAO head;
	
	private final float width;
	private float value;
	
	public Slider(float x, float y, float w, float h, ColorVec baseColor, ColorVec sliderColor, float value)
	{
		this.width = w;
		this.value = value;
		panel = generateBox(x, y, w, h, baseColor);
		head = generateBox(x - h / 2f, y - h / 2, h, h * 2f, sliderColor);
	}
	
	private InterfaceVAO generateBox(float x, float y, float w, float h, ColorVec c)
	{
		float[] vertices = {
				x, y + h, c.getR(), c.getG(), c.getB(),
				x + w, y + h, c.getR(), c.getG(), c.getB(),
				x + w, y, c.getR(), c.getG(), c.getB(),
				x, y, c.getR(), c.getG(), c.getB(),
		};
		short[] indices = {
				0, 1, 2,
				0, 3, 2,
		};
		
		return new InterfaceVAO(vertices, indices);
	}
	
	public void drawMain()
	{
		panel.use();
		panel.draw();
	}
	
	public Matrix getPlacementMatrix()
	{
		Matrix m = new Matrix();
		m.identity();
		m.translate(width * value, 0, -0.1f);
		return m;
	}
	
	public void drawHead()
	{
		head.use();
		head.draw();
	}
}

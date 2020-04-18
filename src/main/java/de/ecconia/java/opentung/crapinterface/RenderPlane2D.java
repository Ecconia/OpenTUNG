package de.ecconia.java.opentung.crapinterface;

import de.ecconia.java.opentung.RenderPlane;
import de.ecconia.java.opentung.inputs.InputConsumer;
import de.ecconia.java.opentung.inputs.InputProcessor;
import de.ecconia.java.opentung.libwrap.Matrix;
import de.ecconia.java.opentung.libwrap.ShaderProgram;

public class RenderPlane2D implements RenderPlane, InputConsumer
{
	private final InputProcessor inputHandler;

	private final Matrix projectionMatrix = new Matrix();

	private ShaderProgram interfaceShader;
//	private final Slider[] sliders = new Slider[4];

//	private Slider activeSlider = null;
//	private int activeSliderIndex = 0;

	public RenderPlane2D(InputProcessor inputHandler)
	{
		this.inputHandler = inputHandler;
		inputHandler.registerClickConsumer(this);
	}

	@Override
	public void setup()
	{
		interfaceShader = new ShaderProgram("interfaceShader");

//		int magicHeight = 10;
//		for(int i = 0; i < 4; i++)
//		{
//			sliders[i] = new Slider(10, 10 + i * (magicHeight * 2 + 5), 200, magicHeight, new ColorVec(0.5f, 0.0f, 0.0f), new ColorVec(1.0f, 0.1f, 0.1f), 1.0f);
//		}

//		sliders[0].setValue(q.getX() / 2f + 0.5f);
//		sliders[1].setValue(q.getY() / 2f + 0.5f);
//		sliders[2].setValue(q.getZ() / 2f + 0.5f);
//		sliders[3].setValue(q.getW() / 2f + 0.5f);

		newSize(500, 500);
	}

	@Override
	public boolean down(int type, int x, int y)
	{
//		if(type == GLFW.GLFW_MOUSE_BUTTON_1)
//		{
//			activeSliderIndex = 0;
//			for(Slider slider : sliders)
//			{
//				if(slider.hitPanel(x, y))
//				{
//					activeSlider = slider;
//					return true;
//				}
//				activeSliderIndex++;
//			}
//		}
		return false;
	}

	@Override
	public boolean move(int xAbs, int yAbs, int xRel, int yRel)
	{
//		if(activeSlider != null)
//		{
//			activeSlider.update(xAbs);
//
//			q.setValues(sliders[3].getValue() * (float) Math.PI * 2f, sliders[0].getValue() * 2f - 1f, sliders[1].getValue() * 2f - 1f, sliders[2].getValue() * 2f - 1f);
//
//			q.normalize();
//
////			sliders[0].setValue(q.getX() / 2f + 0.5f);
////			sliders[1].setValue(q.getY() / 2f + 0.5f);
////			sliders[2].setValue(q.getZ() / 2f + 0.5f);
////			sliders[3].setValue(q.getW() / 2f + 0.5f);
//
//			return true;
//		}
		return false;
	}

	@Override
	public boolean up(int type, int x, int y)
	{
//		if(type == GLFW.GLFW_MOUSE_BUTTON_1)
//		{
//			if(activeSlider != null)
//			{
//				activeSlider = null;
//				return true;
//			}
//		}
		return false;
	}

	@Override
	public void render()
	{
		if(inputHandler.isCaptured())
		{
			return;
		}

		interfaceShader.use();
		interfaceShader.setUniform(0, projectionMatrix.getMat());
		Matrix mat = new Matrix();
		mat.identity();
		interfaceShader.setUniform(1, mat.getMat());

//		for(Slider slider : sliders)
//		{
//			slider.drawMain();
//		}
//		for(Slider slider : sliders)
//		{
//			interfaceShader.setUniform(1, slider.getPlacementMatrix().getMat());
//			slider.drawHead();
//		}
	}

	@Override
	public void newSize(int width, int height)
	{
		projectionMatrix.interfaceMatrix(width, height);
	}
}

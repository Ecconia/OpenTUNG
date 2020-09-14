package de.ecconia.java.opentung.interfaces.windows;

import de.ecconia.java.opentung.interfaces.GUIColors;
import de.ecconia.java.opentung.interfaces.MeshText;
import de.ecconia.java.opentung.interfaces.RenderPlane2D;
import de.ecconia.java.opentung.interfaces.Shapes;
import de.ecconia.java.opentung.interfaces.elements.TextButton;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;
import de.ecconia.java.opentung.settings.Settings;
import org.lwjgl.nanovg.NanoVG;

public class PauseMenu
{
	private static final String labelTextSave = "Save";
	private static final String labelTextSaveAs = "Save as";
	private static final String labelTextExit = "Exit";
	
	private static final float windowWidth = 500;
	private static final float windowHeight = 600;
	private static final int buttonWidth = 400;
	private static final int buttonHeight = 50;
	private static final int padding = 20;
	
	private TextButton buttonSave;
	private TextButton buttonSaveAs;
	private TextButton buttonExit;
	
	private final RenderPlane2D renderPlane2D;
	
	public PauseMenu(RenderPlane2D renderPlane2D)
	{
		this.renderPlane2D = renderPlane2D;
		
		MeshText text = renderPlane2D.getText();
		text.addLetters(labelTextSave);
		text.addLetters(labelTextSaveAs);
		text.addLetters(labelTextExit);
	}
	
	public void setup()
	{
		MeshText fontUnit = renderPlane2D.getText();
		float y = 300 - buttonHeight / 2f - 18;
		buttonExit = new TextButton(fontUnit, labelTextExit, 0, y, buttonWidth, buttonHeight);
		y -= padding + buttonHeight + padding;
		buttonSaveAs = new TextButton(fontUnit, labelTextSaveAs, 0, y, buttonWidth, buttonHeight);
		y -= padding + buttonHeight;
		buttonSave = new TextButton(fontUnit, labelTextSave, 0, y, buttonWidth, buttonHeight);
	}
	
	private float windowStartX;
	private float windowStartY;
	
	public void renderFrame()
	{
		float scale = Settings.guiScale;
		long nvg = renderPlane2D.vg;
		
		NanoVG.nvgBeginPath(nvg);
		
		windowStartX = (renderPlane2D.realWidth(scale) - windowWidth) / 2f;
		windowStartY = (renderPlane2D.realHeight(scale) - windowHeight) / 2f;
		
		float middleX = windowStartX + windowWidth / 2f;
		float middleY = windowStartY + windowHeight / 2f;
		Shapes.drawBox(nvg, middleX, middleY, windowWidth, windowHeight, GUIColors.background, GUIColors.outline);
		buttonSave.renderFrame(nvg, middleX, middleY);
		buttonSaveAs.renderFrame(nvg, middleX, middleY);
		buttonExit.renderFrame(nvg, middleX, middleY);
	}
	
	public void renderDecor(ShaderProgram iconShader, GenericVAO iconPlane)
	{
		float scale = Settings.guiScale;
		renderPlane2D.getLogo().activate();
		
		iconShader.use();
		iconShader.setUniformV2(1, new float[]{(180) * scale, (180) * scale});
		iconShader.setUniformV2(2, new float[]{(windowStartX + windowWidth / 1.8f) * scale, (windowStartY + windowHeight / 3.4f) * scale});
		iconPlane.use();
		iconPlane.draw();
		
		float middleX = windowStartX + windowWidth / 2f;
		float middleY = windowStartY + windowHeight / 2f;
		MeshText fontUnit = renderPlane2D.getText();
		fontUnit.activate();
		ShaderProgram labelShader = renderPlane2D.getLabelShader();
		labelShader.use();
		labelShader.setUniformV3(2, new float[]{0, 0, 0});
		buttonSave.renderText(labelShader, middleX, middleY);
		buttonSaveAs.renderText(labelShader, middleX, middleY);
		buttonExit.renderText(labelShader, middleX, middleY);
	}
	
	public boolean leftMouseUp(int x, int y)
	{
		float scale = Settings.guiScale;
		float sx = (float) x / scale;
		float sy = (float) y / scale;
		
		if(downInside(sx, sy))
		{
			float middleX = windowStartX + windowWidth / 2f;
			float middleY = windowStartY + windowHeight / 2f;
			float xx = sx - middleX;
			float yy = sy - middleY;
			
			if(buttonSave.inside(xx, yy))
			{
				save();
			}
			else if(buttonSaveAs.inside(xx, yy))
			{
				save();
			}
			else if(buttonExit.inside(xx, yy))
			{
				renderPlane2D.issueShutdown();
			}
			
			return true;
		}
		
		return false;
	}
	
	public void save()
	{
		if(!renderPlane2D.prepareSaving())
		{
			return;
		}
		
		buttonSave.setDisabled(true);
		buttonSaveAs.setDisabled(true);
		
		Thread saveThread = new Thread(() -> {
			System.out.println("Saving...");
			//TODO: Real save call.
			try
			{
				Thread.sleep(5000);
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
			//Unlock
			System.out.println("Done.");
			
			renderPlane2D.postSave();
			buttonSave.setDisabled(false);
			buttonSaveAs.setDisabled(false);
		}, "SaveThread");
		saveThread.setDaemon(false); //Yes it should finish saving first! Thus no daemon.
		saveThread.start();
	}
	
	private boolean downInside(float x, float y)
	{
		return windowStartX < x && x < (windowStartX + windowWidth) && windowStartY < y && y < (windowStartY + windowHeight);
	}
	
	public void mouseMoved(int x, int y)
	{
		float scale = Settings.guiScale;
		float sx = (float) x / scale;
		float sy = (float) y / scale;
		
		float middleX = windowStartX + windowWidth / 2f;
		float middleY = windowStartY + windowHeight / 2f;
		float xx = sx - middleX;
		float yy = sy - middleY;
		buttonSave.testHover(xx, yy);
		buttonSaveAs.testHover(xx, yy);
		buttonExit.testHover(xx, yy);
	}
	
	public void close()
	{
		buttonSave.resetHover();
		buttonSaveAs.resetHover();
		buttonExit.resetHover();
	}
}

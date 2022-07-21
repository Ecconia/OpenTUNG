package de.ecconia.java.opentung.interfaces.windows;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import de.ecconia.java.opentung.OpenTUNG;
import de.ecconia.java.opentung.OpenTUNGBootstrap;
import de.ecconia.java.opentung.core.data.ShaderStorage;
import de.ecconia.java.opentung.core.data.SharedData;
import de.ecconia.java.opentung.interfaces.GUIColors;
import de.ecconia.java.opentung.interfaces.MeshText;
import de.ecconia.java.opentung.interfaces.RenderPlane2D;
import de.ecconia.java.opentung.interfaces.Shapes;
import de.ecconia.java.opentung.interfaces.Window;
import de.ecconia.java.opentung.interfaces.elements.TextButton;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;
import de.ecconia.java.opentung.savefile.SavePrepareUnit;
import de.ecconia.java.opentung.settings.Settings;
import org.lwjgl.nanovg.NanoVG;

public class PauseMenu extends Window
{
	private static final String labelTextSave = "Save";
	private static final String labelTextSaveAs = "Save as";
	private static final String labelTextKeybindings = "Keybindings";
	private static final String labelTextExit = "Exit";
	
	private static final int buttonWidth = 400;
	private static final int buttonHeight = 50;
	private static final int padding = 20;
	private static final float windowWidth = 500;
	private static final float windowHeight = 600 + buttonHeight + padding;
	
	private TextButton buttonSave;
	private TextButton buttonSaveAs;
	private TextButton buttonKeybindings;
	private TextButton buttonExit;
	
	private final RenderPlane2D renderPlane2D;
	
	public PauseMenu(RenderPlane2D renderPlane2D)
	{
		this.renderPlane2D = renderPlane2D;
		
		MeshText text = renderPlane2D.getText();
		text.addLetters(labelTextSave);
		text.addLetters(labelTextSaveAs);
		text.addLetters(labelTextKeybindings);
		text.addLetters(labelTextExit);
	}
	
	public void activate()
	{
		isVisible = true;
	}
	
	public void update(SharedData data)
	{
		buttonSave.setDisabled(!(data.isSimulationLoaded() && data.getCurrentBoardFile() != null));
		buttonSaveAs.setDisabled(!data.isSimulationLoaded());
	}
	
	@Override
	public void setup()
	{
		MeshText fontUnit = renderPlane2D.getText();
		float y = windowHeight / 2f - buttonHeight / 2f - 18;
		buttonExit = new TextButton(fontUnit, labelTextExit, 0, y, buttonWidth, buttonHeight);
		y -= padding + buttonHeight + padding;
		buttonKeybindings = new TextButton(fontUnit, labelTextKeybindings, 0, y, buttonWidth, buttonHeight);
		buttonKeybindings.setDisabled(!isRunningFromJar());
		y -= padding + buttonHeight;
		buttonSaveAs = new TextButton(fontUnit, labelTextSaveAs, 0, y, buttonWidth, buttonHeight);
		y -= padding + buttonHeight;
		buttonSave = new TextButton(fontUnit, labelTextSave, 0, y, buttonWidth, buttonHeight);
	}
	
	private float windowStartX;
	private float windowStartY;
	
	@Override
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
		buttonKeybindings.renderFrame(nvg, middleX, middleY);
		buttonExit.renderFrame(nvg, middleX, middleY);
	}
	
	@Override
	public void renderDecor(ShaderStorage shaderStorage)
	{
		float scale = Settings.guiScale;
		renderPlane2D.getLogo().activate();
		
		ShaderProgram textureShader = shaderStorage.getFlatTextureShader();
		textureShader.use();
		textureShader.setUniformV2(1, new float[]{(180) * scale, (180) * scale});
		textureShader.setUniformV2(2, new float[]{(windowStartX + windowWidth / 1.8f) * scale, (windowStartY + windowHeight / 3.8f) * scale});
		GenericVAO texturePlane = shaderStorage.getFlatTexturePlane();
		texturePlane.use();
		texturePlane.draw();
		
		float middleX = windowStartX + windowWidth / 2f;
		float middleY = windowStartY + windowHeight / 2f;
		MeshText fontUnit = renderPlane2D.getText();
		fontUnit.activate();
		ShaderProgram textShader = shaderStorage.getFlatTextShader();
		textShader.use();
		textShader.setUniformV3(2, new float[]{0, 0, 0}); //Color
		buttonSave.renderText(textShader, middleX, middleY);
		buttonSaveAs.renderText(textShader, middleX, middleY);
		buttonKeybindings.renderText(textShader, middleX, middleY);
		buttonExit.renderText(textShader, middleX, middleY);
	}
	
	@Override
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
				new SavePrepareUnit(this, renderPlane2D.getSharedData(), false);
			}
			else if(buttonSaveAs.inside(xx, yy))
			{
				new SavePrepareUnit(this, renderPlane2D.getSharedData(), true);
			}
			else if(buttonKeybindings.inside(xx, yy))
			{
				openKeybindings();
			}
			else if(buttonExit.inside(xx, yy))
			{
				renderPlane2D.issueShutdown();
			}
			
			return true;
		}
		
		return false;
	}
	
	private void openKeybindings()
	{
		String path;
		try
		{
			path = new File(PauseMenu.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toPath().toString();
		}
		catch(URISyntaxException e)
		{
			System.out.println("Could not acquire jar file location.");
			e.printStackTrace(System.out);
			return;
		}
		if(!path.endsWith(".jar"))
		{
			System.out.println("[OpenKeybindingsManager] ERROR: Not running from a jar file! Cannot construct run-command for keybinding manager.");
			return;
		}
		System.out.println("[OpenKeybindingsManager] Jar file path: " + path);
		
		String jvm_location;
		if (System.getProperty("os.name").startsWith("Win")) {
			jvm_location = System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator + "java.exe";
		} else {
			jvm_location = System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator + "java";
		}
		System.out.println("[OpenKeybindingsManager] Java executeable path: " + jvm_location);
		
		ProcessBuilder pb = new ProcessBuilder(jvm_location, "-jar", path, OpenTUNGBootstrap.argKeybindings);
		pb.directory(OpenTUNG.bootstrap.getDataFolder().getParent().toFile());
		try
		{
			pb.start();
		}
		catch(IOException e)
		{
			e.printStackTrace(System.out);
		}
	}
	
	private boolean isRunningFromJar()
	{
		return PauseMenu.class.getProtectionDomain().getCodeSource().getLocation().getPath().endsWith(".jar");
	}
	
	public void setSaveButtonsDisabled(boolean pauseButtonsEnabled)
	{
		buttonSave.setDisabled(pauseButtonsEnabled);
		buttonSaveAs.setDisabled(pauseButtonsEnabled);
	}
	
	private boolean downInside(float x, float y)
	{
		return windowStartX < x && x < (windowStartX + windowWidth) && windowStartY < y && y < (windowStartY + windowHeight);
	}
	
	@Override
	public boolean mouseMoved(int x, int y, boolean leftDown)
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
		buttonKeybindings.testHover(xx, yy);
		buttonExit.testHover(xx, yy);
		return true;
	}
	
	@Override
	public void close()
	{
		super.close();
		
		buttonSave.resetHover();
		buttonSaveAs.resetHover();
		buttonKeybindings.resetHover();
		buttonExit.resetHover();
	}
}

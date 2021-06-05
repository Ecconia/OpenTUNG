package de.ecconia.java.opentung.interfaces.windows;

import de.ecconia.java.opentung.OpenTUNG;
import de.ecconia.java.opentung.core.ShaderStorage;
import de.ecconia.java.opentung.core.SharedData;
import de.ecconia.java.opentung.interfaces.GUIColors;
import de.ecconia.java.opentung.interfaces.MeshText;
import de.ecconia.java.opentung.interfaces.RenderPlane2D;
import de.ecconia.java.opentung.interfaces.Shapes;
import de.ecconia.java.opentung.interfaces.elements.TextButton;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;
import de.ecconia.java.opentung.settings.Settings;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.lwjgl.nanovg.NanoVG;

public class PauseMenu
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
	
	public void update(SharedData data)
	{
		buttonSave.setDisabled(!(data.isSimulationLoaded() && data.getCurrentBoardFile() != null));
		buttonSaveAs.setDisabled(!data.isSimulationLoaded());
	}
	
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
				save(false);
			}
			else if(buttonSaveAs.inside(xx, yy))
			{
				save(true);
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
		String path = PauseMenu.class.getProtectionDomain().getCodeSource().getLocation().getPath();
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
		
		ProcessBuilder pb = new ProcessBuilder(jvm_location, "-jar", path, "key");
		pb.directory(OpenTUNG.dataFolder.getParent().toFile());
		try
		{
			pb.start();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private boolean isRunningFromJar()
	{
		return PauseMenu.class.getProtectionDomain().getCodeSource().getLocation().getPath().endsWith(".jar");
	}
	
	public void save(boolean chooser)
	{
		Thread t = new Thread(() -> {
			{
				SharedData sd = renderPlane2D.getSharedData();
				Path currentSavePath = sd.getCurrentBoardFile();
				if(chooser)
				{
					if(currentSavePath == null)
					{
						currentSavePath = OpenTUNG.boardFolder;
					}
					else
					{
						currentSavePath = currentSavePath.getParent();
					}
					JFileChooser fileChooser = new JFileChooser(currentSavePath.toFile());
					int result = fileChooser.showSaveDialog(null);
					if(result != JFileChooser.APPROVE_OPTION)
					{
						return;
					}
					currentSavePath = fileChooser.getSelectedFile().toPath();
					String fileName = currentSavePath.getFileName().toString();
					int endingIndex = fileName.lastIndexOf('.');
					if(endingIndex < 0)
					{
						currentSavePath = currentSavePath.resolveSibling(fileName + ".opentung");
					}
					else
					{
						String ending = fileName.substring(endingIndex + 1);
						if(!ending.equals("opentung"))
						{
							JOptionPane.showMessageDialog(null, "File-ending must be '.opentung', change or leave blank.", "Can only save .opentung files.", JOptionPane.ERROR_MESSAGE, null);
							return;
						}
					}
				}
				else
				{
					//Button disabled if currentSavePath is null.
					String fileName = currentSavePath.getFileName().toString();
					int endingIndex = fileName.lastIndexOf('.');
					if(fileName.substring(endingIndex + 1).equals("tungboard")) //Assumes file has always ending.
					{
						int result = JOptionPane.showOptionDialog(null, "You loaded a .tungboard file, save as .opentung file?", "Save as OpenTUNG-Save?", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
						if(result != JOptionPane.OK_OPTION)
						{
							return;
						}
						currentSavePath = currentSavePath.getParent().resolve(fileName.substring(0, endingIndex + 1) + "opentung");
						//TODO: Add check that the file does not exist...
					}
				}
				
				sd.setCurrentBoardFile(currentSavePath);
			}
			
			//Lock
			if(!renderPlane2D.prepareSaving())
			{
				return;
			}
			buttonSave.setDisabled(true);
			buttonSaveAs.setDisabled(true);
			
			Thread saveThread = new Thread(() -> {
				System.out.println("Saving...");
				long startTime = System.currentTimeMillis();
				renderPlane2D.performSave();
				System.out.println("Done, took: " + (System.currentTimeMillis() - startTime) + "ms");
				//Unlock:
				renderPlane2D.postSave();
				buttonSave.setDisabled(false);
				buttonSaveAs.setDisabled(false);
			}, "SaveThread");
			saveThread.setDaemon(false); //Yes it should finish saving first! Thus no daemon.
			saveThread.start();
		}, "Save-Preparation-Thread");
		t.start();
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
		buttonKeybindings.testHover(xx, yy);
		buttonExit.testHover(xx, yy);
	}
	
	public void close()
	{
		buttonSave.resetHover();
		buttonSaveAs.resetHover();
		buttonKeybindings.resetHover();
		buttonExit.resetHover();
	}
}

package de.ecconia.java.opentung.settings.keybinds.manager;

import de.ecconia.java.opentung.OpenTUNG;
import de.ecconia.java.opentung.settings.keybinds.KeybindingsIO;
import java.nio.file.Path;

public class KeybindingManager
{
	private final KeyCodeGrabber keyCodeGrabber;
	private final KeybindingGUI keybindingGUI;
	private final KeybindingsIO keybindingsIO;
	
	private boolean focus;
	
	public KeybindingManager(Path keybindingFile)
	{
		//Needs to be first, else the keybindingIO cannot load...
		keyCodeGrabber = new KeyCodeGrabber(this);
		keybindingsIO = new KeybindingsIO(OpenTUNG.keybindPath, null);
		keybindingGUI = new KeybindingGUI(this, keybindingsIO.getKeys());
		
		while(true)
		{
			//Main thread idle:
			try
			{
				Thread.sleep(10);
			}
			catch(InterruptedException e)
			{
			}
			
			if(keyCodeGrabber.run())
			{
				break;
			}
			
			if(focus)
			{
				keyCodeGrabber.grabFocus();
				focus = false;
			}
		}
	}
	
	public void inputFocus()
	{
		focus = true;
	}
	
	public void inputActive(boolean focused)
	{
		keybindingGUI.inputFocused(focused);
	}
	
	public void keyboardInput(int scancode, String glfwName, String osCharacter)
	{
		keybindingGUI.keyboardInput(scancode, glfwName, osCharacter);
	}
	
	public void saveData()
	{
		keybindingsIO.overwriteFile(OpenTUNG.keybindPath);
	}
}
package de.ecconia.java.opentung.settings.keybinds.manager;

import de.ecconia.java.opentung.settings.keybinds.KeybindingsIO;
import java.nio.file.Path;

public class KeybindingManager
{
	private final Path keybindingFile;
	private final KeybindingGUI keybindingGUI;
	private final KeybindingsIO keybindingsIO;
	
	private boolean focus;
	
	public KeybindingManager(Path keybindingFile)
	{
		this.keybindingFile = keybindingFile;
		//Needs to be first, else the keybindingIO cannot load...
		KeyCodeGrabber keyCodeGrabber = new KeyCodeGrabber(this);
		keybindingsIO = new KeybindingsIO(keybindingFile, null);
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
				//Not supposed to be interrupted.
				e.printStackTrace(System.out);
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
		keybindingsIO.overwriteFile(keybindingFile);
	}
}
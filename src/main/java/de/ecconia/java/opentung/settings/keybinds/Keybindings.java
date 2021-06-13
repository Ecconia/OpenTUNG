package de.ecconia.java.opentung.settings.keybinds;

public class Keybindings
{
	//TODO: Escape will always be bound to "pause-menu", allow secondary button.
	//TODO: Disable cluster highlight. Somehow Q, but which priority?
	
	//Global:
	@KeybindingDefaults(key = "GUI-ToggleComponentsList", defaultValue = "TAB", comment = "" +
			"In this file you can find the keybindings which OpenTUNG uses/supports.\n" +
			" Not all functions can be changed, but what can be changed you will find here.\n" +
			"However this is a technical file, not intended to be changed by humans.\n" +
			" To edit it, you can use the built-in helper tool: 'java -jar <OpenTUNG.jar> -keyhelper'.\n" +
			"Format: <ReadableDescriptiveKey>: <ScanCode or GLFW-KeyCode>, <Optional localized letter>\n" +
			" The <GLFW-KeyCode> is assuming you are using the US keyboard layout, thus there is a localized letter which tells you which key it is on your keyboard.\n" +
			"\n" +
			"Global functions:"
	)
	public static int KeyToggleComponentsList; //TAB
	@KeybindingDefaults(key = "GUI-UnlockMouseCursor", defaultValue = "F1")
	public static int KeyUnlockMouseCursor; //F1
	
	//Only 3D world:
	
	//Movement:
	@KeybindingDefaults(key = "Fly-Forward", defaultValue = "W", comment = "" +
			"\n" +
			"Player movement keys:\n"
	)
	public static int KeyFlyForward; //W
	@KeybindingDefaults(key = "Fly-Backward", defaultValue = "S")
	public static int KeyFlyBackward; //S
	@KeybindingDefaults(key = "Fly-Left", defaultValue = "A")
	public static int KeyFlyLeft; //A
	@KeybindingDefaults(key = "Fly-Right", defaultValue = "D")
	public static int KeyFlyRight; //D
	@KeybindingDefaults(key = "Fly-Down", defaultValue = "LEFT_SHIFT")
	public static int KeyFlyDown; //SHIFT
	@KeybindingDefaults(key = "Fly-Up", defaultValue = "SPACE")
	public static int KeyFlyUp; //SPACE
	@KeybindingDefaults(key = "Fly-Boost", defaultValue = "LEFT_CONTROL")
	public static int KeyFlyBoost; //CONTROL
	
	//When grabbing:
	@KeybindingDefaults(key = "Grab-Abort", defaultValue = "Q", comment = "" +
			"\n" +
			"Functions activated, while grabbing:\n" +
			"\n" +
			"Grab-Delete also triggers on ESCAPE."
	)
	public static int KeyGrabAbort; //Q (+ESC)
	@KeybindingDefaults(key = "Grab-Delete", defaultValue = "E")
	public static int KeyGrabDelete; //E
	@KeybindingDefaults(key = "Grab-Rotate", defaultValue = "R")
	public static int KeyGrabRotate; //R
	@KeybindingDefaults(key = "Grab-BoardRotateY", defaultValue = "R")
	public static int KeyGrabRotateY; //R
	@KeybindingDefaults(key = "Grab-BoardRotateX", defaultValue = "F")
	public static int KeyGrabRotateX; //F
	@KeybindingDefaults(key = "Grab-BoardRotateZ", defaultValue = "G")
	public static int KeyGrabRotateZ; //G
	
	@KeybindingDefaults(key = "Hotbar-Drop", defaultValue = "Q", comment = "" +
			"\n" +
			"General functions used for building:\n")
	public static int KeyHotbarDrop; //Q //TBI: Collides?
	@KeybindingDefaults(key = "Build-Delete", defaultValue = "E")
	public static int KeyDelete; //E
	@KeybindingDefaults(key = "Build-Grab", defaultValue = "F")
	public static int KeyGrab; //F
	@KeybindingDefaults(key = "Build-Rotate", defaultValue = "R")
	public static int KeyRotate; //R
	@KeybindingDefaults(key = "Build-Resize", defaultValue = "C")
	public static int KeyResize;
	
	@KeybindingDefaults(key = "Simulation-Pause", defaultValue = "P")
	public static int KeyPauseSimulation; //R
	@KeybindingDefaults(key = "Simulation-Tick", defaultValue = "T")
	public static int KeyTickSimulation;
}

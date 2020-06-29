package de.ecconia.java.opentung;

public class Settings
{
	//### PLAYER CONTROL ###
	
	//Spawn location of the player:
	public static float playerSpawnX = 0.0f;
	public static float playerSpawnY = 0.0f;
	public static float playerSpawnZ = 0.0f;
	//Rotation speed. 1 means 1 pixel is 1 degree:
	public static float playerRotationSpeed = 0.333f;
	public static float playerFastRotationSpeed = 0.4f; //With control
	//Fly speed:
	public static float playerFlySpeed = 0.05f;
	public static float playerFastFlySpeed = 0.15f; //With control
	
	//### BOARD IMPORT ###
	
	//The rotation of the root board, axis apply order: XZY
	public static float rootBoardAngleX = 180f;
	public static float rootBoardAngleY = -90f;
	public static float rootBoardAngleZ = 0f;
	//Where the center of the root board will be placed in the world:
	public static double rootBoardOffsetX = -16 * 0.3;
	public static double rootBoardOffsetY = -5;
	public static double rootBoardOffsetZ = 13;
	
	//If a component can't be imported for various reasons (modding), this can prevent the load process from aborting.
	public static boolean importMaintenanceMode = false;
	
	//### SIMULATION ###
	
	//There is no target TPS yet, instead a delay of x milliseconds:
	//0 means no waiting.
	public static int delayBetweenTicks = 10;
	
	//### HIGHLIGHTING & DRAWING ###
	
	//Raycasting is done to detect what you are looking at.
	public static boolean doRaycasting = true;
	
	//Select if to highlight these things:
	public static boolean highlightBoards = false;
	public static boolean highlightWires = true;
	public static boolean highlightComponents = true;
	
	public static float highlightColorR = 0;
	public static float highlightColorG = 0;
	public static float highlightColorB = 1.0f;
	public static float highlightColorA = 0.3f;
	
	public static float highlightClusterColorR = 0f;
	public static float highlightClusterColorG = 1.0f;
	public static float highlightClusterColorB = 0f;
	public static float highlightClusterColorA = 0.6f;
	
	//Skip drawing the world:
	public static boolean drawWorld = true;
	//Gives a nice see-thru to turn them off.
	public static boolean drawBoards = true;
	//The white stuff in components is their material.
	public static boolean drawMaterial = true;
	
	//Used for some debugging every now and then.
	public static boolean drawComponentPositionIndicator = false;
	
	//Canvas background color
	public static float backgroundColorR = 1f / 255f * 54f;
	public static float backgroundColorG = 1f / 255f * 57f;
	public static float backgroundColorB = 1f / 255f * 63f;
	
	//### Other ###
	
	//The FPS which it tries to archive, 0 means VSync.
	public static int targetFPS = 0;
	//FOV used in the 3D world.
	public static float fov = 45f;
	
	//Time in ms, which interaction-presses will be considered long.
	//If that is the case, one can cancel interactions, by not looking at the initial component.
	public static int longMousePressDuration = 1000;
}

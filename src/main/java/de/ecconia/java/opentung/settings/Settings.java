package de.ecconia.java.opentung.settings;

public class Settings
{
	//### CONTROL ###
	
	@SettingsSectionStart(key = "settings.movement.speed.rotation", comment = "Rotation speed. 1 means 1 pixel is 1 degree.")
	@SettingInfo(key = "settings.movement.speed.rotation.slow")
	public static float playerRotationSpeed = 0.333f;
	@SettingInfo(key = "settings.movement.speed.rotation.fast", comment = "Speed when MOD key is pressed.")
	public static float playerFastRotationSpeed = 0.4f;
	
	@SettingsSectionStart(key = "settings.movement.speed.fly", comment = "Fly speed.")
	@SettingInfo(key = "settings.movement.speed.fly.slow")
	public static float playerFlySpeed = 0.05f;
	@SettingInfo(key = "settings.movement.speed.fly.fast", comment = "Speed when MOD key is pressed.")
	public static float playerFastFlySpeed = 0.15f;
	
	@SettingInfo(key = "settings.input.mouse.longPressDuration", comment = "Time in ms, which interaction-presses will be considered long.\nIf that is the case, one can cancel interactions, by not looking at the initial component.")
	public static int longMousePressDuration = 1000;
	
	//### BOARD IMPORT ###
	
	@SettingsSectionStart(key = "settings.initialLoading.boardRotation", comment = "The rotation of the .tungboard root-board, axis apply order: XZY")
	@SettingInfo(key = "settings.initialLoading.boardRotation.x")
	public static float rootBoardAngleX = 180f;
	@SettingInfo(key = "settings.initialLoading.boardRotation.y")
	public static float rootBoardAngleY = -90f;
	@SettingInfo(key = "settings.initialLoading.boardRotation.z")
	public static float rootBoardAngleZ = 0f;
	
	@SettingsSectionStart(key = "settings.initialLoading.boardOffset", comment = "Where the center of the .tungboard root-board will be placed in the world:")
	@SettingInfo(key = "settings.initialLoading.boardOffset.x")
	public static double rootBoardOffsetX = -16 * 0.3;
	@SettingInfo(key = "settings.initialLoading.boardOffset.y")
	public static double rootBoardOffsetY = -5;
	@SettingInfo(key = "settings.initialLoading.boardOffset.z")
	public static double rootBoardOffsetZ = 13;
	
	@SettingsSectionStart(key = "settings.initialLoading.playerSpawn", comment = "Spawn location of the player:")
	@SettingInfo(key = "settings.initialLoading.playerSpawn.x")
	public static float playerSpawnX = 0.0f;
	@SettingInfo(key = "settings.initialLoading.playerSpawn.y")
	public static float playerSpawnY = 0.6f;
	@SettingInfo(key = "settings.initialLoading.playerSpawn.z")
	public static float playerSpawnZ = 0.0f;
	
	@SettingInfo(key = "settings.import.maintenanceMode", comment = "If a component can't be imported for various reasons (modding), this can prevent the load process from aborting.")
	public static boolean importMaintenanceMode = false;
	
	//### SIMULATION ###
	
	@SettingInfo(key = "settings.simulation.targetTPS", comment = "TPS which the game will try to archive.\n0 is unlimited.")
	public static int targetTPS = 100;
	
	//### HIGHLIGHTING & DRAWING ###
	
	@SettingInfo(key = "settings.graphic.raycasting", comment = "Raycasting is done to detect what you are looking at.")
	public static boolean doRaycasting = true;
	
	@SettingsSectionStart(key = "settings.graphic.highlight.drawToggles", comment = "Select if to highlight these things:")
	@SettingInfo(key = "settings.graphic.highlight.drawToggles.boards")
	public static boolean highlightBoards = false;
	@SettingInfo(key = "settings.graphic.highlight.drawToggles.wires")
	public static boolean highlightWires = true;
	@SettingInfo(key = "settings.graphic.highlight.drawToggles.components")
	public static boolean highlightComponents = true;
	
	@SettingsSectionStart(key = "settings.graphic.highlight.color", comment = "Color of the thing you are looking at.")
	@SettingInfo(key = "settings.graphic.highlight.color.r")
	public static float highlightColorR = 0;
	@SettingInfo(key = "settings.graphic.highlight.color.g")
	public static float highlightColorG = 0;
	@SettingInfo(key = "settings.graphic.highlight.color.b")
	public static float highlightColorB = 1.0f;
	@SettingInfo(key = "settings.graphic.highlight.color.a")
	public static float highlightColorA = 0.3f;
	
	@SettingsSectionStart(key = "settings.graphic.highlight.clusterColor", comment = "Color currently highlighted cluster.")
	@SettingInfo(key = "settings.graphic.highlight.clusterColor.r")
	public static float highlightClusterColorR = 0f;
	@SettingInfo(key = "settings.graphic.highlight.clusterColor.g")
	public static float highlightClusterColorG = 1.0f;
	@SettingInfo(key = "settings.graphic.highlight.clusterColor.b")
	public static float highlightClusterColorB = 0f;
	@SettingInfo(key = "settings.graphic.highlight.clusterColor.a")
	public static float highlightClusterColorA = 0.6f;
	
	@SettingsSectionStart(key = "settings.graphic.placementIndicator.color", comment = "Color placement indicator, only updates on startup.")
	@SettingInfo(key = "settings.graphic.placementIndicator.color.r")
	public static float placementIndicatorColorR = 0.2f;
	@SettingInfo(key = "settings.graphic.placementIndicator.color.g")
	public static float placementIndicatorColorG = 0.2f;
	@SettingInfo(key = "settings.graphic.placementIndicator.color.b")
	public static float placementIndicatorColorB = 1.0f;
	
	@SettingInfo(key = "settings.graphic.drawToggles.world", comment = "Skip drawing the world.")
	public static boolean drawWorld = true;
	@SettingInfo(key = "settings.graphic.drawToggles.boards", comment = "Gives a nice see-thru to turn them off.")
	public static boolean drawBoards = true;
	@SettingInfo(key = "settings.graphic.drawToggles.material", comment = "The white stuff in components is their material.")
	public static boolean drawMaterial = true;
	
	//Used for some debugging every now and then.
	@SettingInfo(key = "settings.graphic.drawToggles.componentPositionIndicator")
	public static boolean drawComponentPositionIndicator = false;
	@SettingInfo(key = "settings.graphic.drawToggles.worldAxisIndicator")
	public static boolean drawWorldAxisIndicator = false;
	
	@SettingsSectionStart(key = "settings.graphic.backgroundColor", comment = "Canvas background color")
	@SettingInfo(key = "settings.graphic.backgroundColor.r")
	public static float backgroundColorR = 1f / 255f * 54f;
	@SettingInfo(key = "settings.graphic.backgroundColor.g")
	public static float backgroundColorG = 1f / 255f * 57f;
	@SettingInfo(key = "settings.graphic.backgroundColor.b")
	public static float backgroundColorB = 1f / 255f * 63f;
	
	//### Other ###
	
	@SettingInfo(key = "settings.graphic.fps", comment = "The FPS which it tries to archive, 0 means VSync.")
	public static int targetFPS = 0;
	@SettingInfo(key = "settings.graphic.fov", comment = "FOV used in the 3D world.")
	public static float fov = 45f;
	
	@SettingInfo(key = "settings.graphic.text.labelTexturePixelResolution", comment = "Label-Textures will be generate with this resolution², you may increase this.\nThe labels get generated in background, so you might see more \"Loading...\" textures.")
	public static int labelTexturePixelResolution = 1024;
	@SettingInfo(key = "settings.graphic.text.labelSDFTexturePixelResolution", comment = "The resolution uploaded to the GPU. Be sure that the label resolution can be divided by the SDF resolution.\nHint: You might break the loading-texture, its 1024²\nMUST BE a multiple of 4!")
	public static int labelSDFTexturePixelResolution = 256;
	
	@SettingInfo(key = "settings.graphic.gui.component.icon.resolution", comment = "The resolution of component icons.\nMust be a multiple of 4!")
	public static int componentIconResolution = 200;
	
	@SettingInfo(key = "settings.graphic.gui.global.scale", comment = "Factor for any size related number in the GUI.")
	public static float guiScale = 0.5f;
	@SettingInfo(key = "settings.graphic.gui.global.scrollSwapped", comment = "Horizontal scroll direction, default (non-swapped): Scroll-UP -> Move-Right & Scroll-DOWN -> Move-Left\nIf this is different for your OS, please report it!")
	public static boolean horizontalSwapped = false;
}

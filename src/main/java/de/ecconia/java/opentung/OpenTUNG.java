package de.ecconia.java.opentung;

import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.core.BoardUniverse;
import de.ecconia.java.opentung.core.OpenTUNGVersion;
import de.ecconia.java.opentung.core.RenderPlane3D;
import de.ecconia.java.opentung.core.ShaderStorage;
import de.ecconia.java.opentung.core.SharedData;
import de.ecconia.java.opentung.inputs.InputProcessor;
import de.ecconia.java.opentung.interfaces.RenderPlane2D;
import de.ecconia.java.opentung.libwrap.SWindowWrapper;
import de.ecconia.java.opentung.savefile.Loader;
import de.ecconia.java.opentung.settings.DataFolderWatcher;
import de.ecconia.java.opentung.settings.Settings;
import de.ecconia.java.opentung.settings.SettingsIO;
import de.ecconia.java.opentung.tungboard.TungBoardLoader;
import de.ecconia.java.opentung.util.math.Quaternion;
import de.ecconia.java.opentung.util.math.Vector3;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import org.lwjgl.Version;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL30;

public class OpenTUNG
{
	public static Path dataFolder;
	public static Path boardFolder;
	public static Path settingsPath;
	
	//TODO: Load from settings:
	private static final int initialWidth = 800;
	private static final int initialHeight = 600;
	
	private static InputProcessor inputHandler;
	
	private static RenderPlane2D interactables;
	private static RenderPlane3D worldView;
	
	private static BoardUniverse boardUniverse;
	
	private static ShaderStorage shaderStorage;
	
	public static void main(String[] args)
	{
		//Catch if any thread shuts down unexpectedly. Print on output stream to get the exact time.
		Thread.setDefaultUncaughtExceptionHandler((Thread t, Throwable e) -> {
			String threadCrashMessage = "Thread " + t.getName() + " crashed.";
			System.out.println(threadCrashMessage);
			e.printStackTrace(System.out);
			JOptionPane.showMessageDialog(null, new JLabel(threadCrashMessage + "\nSee console for stacktrace, please report it."));
		});
		
		//Load version information:
		OpenTUNGVersion version = new OpenTUNGVersion();
		System.out.println("Running OpenTUNG Version: git-" + version.getGitCommitHash() + (version.isGitDirty() ? "-dirty" : "") + (!version.getGitBranch().equals("master") ? " (" + version.getGitBranch() + ")" : ""));
		
		parsePreSetupArguments(args);
		setupDataFolder();
		Path toLoadFile = parsePostSetupArguments(args);
		
		//Create DataFolderWatcher, used for generic callbacks on file change.
		DataFolderWatcher watcher = new DataFolderWatcher(dataFolder);
		//Create the initial Settings loader, it uses the watcher to keep the settings up to date.
		new SettingsIO(settingsPath, watcher, Settings.class);
		
		String fileName;
		if(toLoadFile == null)
		{
			fileName = "<unsaved>";
			boardUniverse = new BoardUniverse(generateStartingBoard());
		}
		else
		{
			fileName = toLoadFile.getFileName().toString();
			if(fileName.endsWith(".opentung"))
			{
				boardUniverse = new BoardUniverse(Loader.load(toLoadFile));
			}
			else
			{
				boardUniverse = new BoardUniverse(TungBoardLoader.importTungBoard(toLoadFile));
			}
		}
		
		SharedData sharedData = new SharedData(boardUniverse, toLoadFile);
		
		System.out.println("Starting GUI...");
		try
		{
			System.out.println("LWJGL version: " + Version.getVersion());
			
			SWindowWrapper window = new SWindowWrapper(initialWidth, initialHeight, "OpenTUNG FPS: ? | TPS: ? | avg. UPT: ? | " + fileName);
			inputHandler = new InputProcessor(window.getID());
			
			Thread graphicsThread = new Thread(() -> {
				try
				{
					//Grab the graphic context for OpenGL on this thread.
					window.grabContext();
					window.setVsync(Settings.targetFPS == 0);
					window.place();
					
					//OpenGL:
					GL.createCapabilities();
					System.out.println("OpenGL version: " + GL30.glGetString(GL30.GL_VERSION));
					System.out.println("Amount of connection-groups per mesh: " + GL30.glGetInteger(GL30.GL_MAX_VERTEX_UNIFORM_COMPONENTS));
					
					//TODO: Load a placeholder texture here... Just the logo with some background.
					//Run initially, to reduce weird visible crap.
					window.update();
					
					init(sharedData);
					
					long past = System.currentTimeMillis();
					int finishedRenderings = 0;
					
					long frameDuration = Settings.targetFPS != 0 ? 1000L / (long) Settings.targetFPS : 1;
					long lastFinishedRender = System.currentTimeMillis();
					
					while(!window.shouldClose())
					{
						Dimension newSize = window.getNewDimension();
						if(newSize != null)
						{
							GL30.glViewport(0, 0, newSize.width, newSize.height);
							shaderStorage.newSize(newSize.width, newSize.height);
							interactables.newSize(newSize.width, newSize.height);
							worldView.newSize(newSize.width, newSize.height);
						}
						
						//TODO: Fancy policy for clearing the scene...
						render();
						
						window.update();
						
						//FPS counting:
						finishedRenderings++;
						long now = System.currentTimeMillis();
						if(now - past > 1000)
						{
							past = now;
							sharedData.setFPS(finishedRenderings);
							finishedRenderings = 0;
						}
						
						//FPS limiting:
						if(Settings.targetFPS != 0)
						{
							long currentTime = System.currentTimeMillis();
							long timeToWait = frameDuration - (currentTime - lastFinishedRender);
							if(timeToWait > 0)
							{
								try
								{
									Thread.sleep(timeToWait);
								}
								catch(InterruptedException e)
								{
									e.printStackTrace(); //Should never happen though.
								}
							}
							lastFinishedRender = System.currentTimeMillis();
						}
					}
					
					boardUniverse.getSimulation().interrupt();
					inputHandler.stop();
					System.out.println("Graphic thread has turned off.");
				}
				catch(Exception e)
				{
					e.printStackTrace();
					boardUniverse.getSimulation().interrupt();
					inputHandler.stop();
					System.exit(1); //Throw 1;
				}
			}, "GraphicThread");
			graphicsThread.start();
			
			//Let main-thread execute the input handler:
			Thread.currentThread().setName("Main/Input");
			inputHandler.eventPollEntry(() -> {
				Path savePath = sharedData.getCurrentBoardFile();
				window.setTitle("OpenTUNG FPS: " + sharedData.getFPS() + " | TPS: " + boardUniverse.getSimulation().getTPS() + " | avg. UPT: " + boardUniverse.getSimulation().getLoad() + " | " + (savePath == null ? "<unsaved>" : savePath.getFileName().toString()));
			});
			System.out.println("Main/Input thread has turned off.");
		}
		catch(Exception e)
		{
			e.printStackTrace();
			inputHandler.stop();
			System.exit(1); //Throw 1;
		}
	}
	
	private static void setupDataFolder()
	{
		try
		{
			dataFolder = Paths.get("OpenTUNG").toRealPath(LinkOption.NOFOLLOW_LINKS);
			boardFolder = dataFolder.resolve("boards");
			System.out.println("[FilesInit] Using data folder at: " + dataFolder);
			
			if(!Files.exists(dataFolder))
			{
				System.out.println("[FilesInit] Data folder does not exist, creating.");
				Files.createDirectories(dataFolder);
			}
			else if(!Files.isDirectory(dataFolder))
			{
				System.out.println("[FilesInit] ERROR: Data folder is a file, thus cannot be used. Please remove data file: '" + dataFolder + "'.");
				System.exit(1);
			}
			
			if(!Files.exists(boardFolder))
			{
				System.out.println("[FilesInit] Board folder does not exist, creating.");
				Files.createDirectory(boardFolder);
			}
			else if(!Files.isDirectory(boardFolder))
			{
				System.out.println("[FilesInit] [ERROR] Board folder is a file, thus cannot be used. Please remove board file: '" + boardFolder + "'.");
				System.exit(1);
			}
			
			settingsPath = dataFolder.resolve("settings.txt");
			if(Files.isDirectory(settingsPath))
			{
				System.out.println("[FilesInit] Settings file is a directory, thus cannot be used. Please remove settings folder: '" + settingsPath + "'.");
				System.exit(1);
			}
		}
		catch(IOException e)
		{
			System.out.println("[FilesInit] Failed to create data folder. Please report stacktrace, if you have no clue why:");
			e.printStackTrace(System.out);
			JOptionPane.showMessageDialog(null, new JLabel("Could not create data folders. Please see console for error details. Report this issue, if it does not make sense to you."));
			System.exit(1);
		}
	}
	
	private static void parsePreSetupArguments(String... arguments)
	{
		if(arguments.length > 1)
		{
			System.out.println("[ArgumentParser] OpenTUNG allows at most 1 argument. If you supplied an argument with spaces wrap it up in quotes: \"<your argument with spaces>\"");
			System.exit(1);
		}
		else if(arguments.length == 1)
		{
			String argument = arguments[0];
			if(argument.toLowerCase().matches("(--?)?(help|\\?|h)"))
			{
				printHelp();
				System.exit(0);
			}
		}
	}
	
	private static Path parsePostSetupArguments(String... arguments)
	{
		Path toLoadFile = null;
		if(arguments.length == 1)
		{
			String argument = arguments[0];
			//Well multiple arguments for the same stuff and even Regex, not cool. Going to be obsolete anyway.
			if(argument.toLowerCase().matches("(--?)?(load|window|gui)"))
			{
				File file = loadFileGUI();
				if(file == null)
				{
					System.exit(1);
					return null; //Satisfy compiler.
				}
				toLoadFile = file.toPath();
			}
			else
			{
				//Attempt to parse as filepath
				if(canBeLoaded(argument))
				{
					try
					{
						toLoadFile = FileSystems.getDefault().getPath(argument);
					}
					catch(InvalidPathException e)
					{
						System.out.println("[ArgumentParser] Failed parse file to load. Supplied: '" + argument + "'.");
						e.printStackTrace(System.out);
						System.exit(1);
						return null; //Satisfy compiler.
					}
					
					if(!toLoadFile.isAbsolute())
					{
						//Try to use from root and from the board folder.
						Path withRelativePrefix = boardFolder.resolve(toLoadFile);
						if(Files.exists(withRelativePrefix))
						{
							toLoadFile = withRelativePrefix;
						}
					}
					
					if(!Files.exists(toLoadFile))
					{
						System.out.println("[ArgumentParser] Could not find file at: '" + toLoadFile + "'.");
						System.exit(1);
						return null; //Satisfy compiler.
					}
					try
					{
						toLoadFile = toLoadFile.toRealPath(LinkOption.NOFOLLOW_LINKS);
					}
					catch(IOException e)
					{
						System.out.println("[ArgumentParser] Failed to resolve provided file path to full path. File path: '" + toLoadFile + "'.");
						e.printStackTrace(System.out);
						System.exit(1);
						return null; //Satisfy compiler.
					}
				}
				else
				{
					System.out.println("[ArgumentParser] Your argument is not a known command, nor a file OpenTUNG can open. Supplied: '" + argument + "'.");
					printHelp();
					System.exit(1);
				}
			}
		}
		//else - No input file or other instruction.
		return toLoadFile;
	}
	
	private static void printHelp()
	{
		System.out.println(
				"[HELP] Following commands are supported:\n"
						+ "[HELP] - You can use 'load', 'window', 'gui' to open a window for choosing a file to load.\n"
						+ "[HELP] - You can supply one file to load, but it must end on '.tungboard' or '.opentung'.");
	}
	
	private static File loadFileGUI()
	{
		JFileChooser fileChooser = new JFileChooser(boardFolder.toFile());
		int result = fileChooser.showSaveDialog(null);
		if(result != JFileChooser.APPROVE_OPTION)
		{
			System.out.println("Nothing chosen, terminating.");
			return null;
		}
		File currentSaveFile = fileChooser.getSelectedFile();
		
		if(canBeLoaded(currentSaveFile.getName()))
		{
			return currentSaveFile;
		}
		else
		{
			System.out.println("Choose '" + currentSaveFile.getAbsolutePath() + "', missing or incorrect file ending.");
			JOptionPane.showMessageDialog(null, "File-ending must be '.opentung' or '.tungboard'.", "Can only load TUNG and OpenTUNG files.", JOptionPane.ERROR_MESSAGE, null);
			return null;
		}
	}
	
	private static boolean canBeLoaded(String filename)
	{
		int endingIndex = filename.lastIndexOf('.');
		if(endingIndex < 0)
		{
			//No dot in name.
			return false;
		}
		String ending = filename.substring(endingIndex + 1).toLowerCase();
		return ending.equals("opentung") || ending.equals("tungboard");
	}
	
	private static CompBoard generateStartingBoard()
	{
		CompBoard board = new CompBoard(null, 10, 10);
		board.setPosition(Vector3.zero);
		board.setRotation(Quaternion.zero);
		return board;
	}
	
	private static void init(SharedData sharedData)
	{
		setBackgroundColor();
		
		setOpenGLMode();
		
		System.out.println("[Init] Loading shaders and static models.");
		shaderStorage = new ShaderStorage();
		shaderStorage.newSize(initialWidth, initialHeight);
		sharedData.setShaderStorage(shaderStorage);
		System.out.println("[Init] Loaded shaders and static models.");
		
		worldView = new RenderPlane3D(inputHandler, boardUniverse, sharedData);
		interactables = new RenderPlane2D(inputHandler, sharedData);
		worldView.setup();
		interactables.setup();
		worldView.newSize(initialWidth, initialHeight);
		interactables.newSize(initialWidth, initialHeight);
	}
	
	public static void setOpenGLMode()
	{
		GL30.glEnable(GL30.GL_CULL_FACE);
		GL30.glFrontFace(GL30.GL_CCW);
		GL30.glCullFace(GL30.GL_BACK);
		
		GL30.glEnable(GL30.GL_DEPTH_TEST);
		GL30.glDepthFunc(GL30.GL_LESS);
		
		GL30.glEnable(GL30.GL_BLEND);
		GL30.glBlendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);
		
		GL30.glEnable(GL30.GL_STENCIL_TEST);
		GL30.glStencilFunc(GL30.GL_NOTEQUAL, 1, 0xFF); //Set this default, just for the case its different.
		GL30.glStencilOp(GL30.GL_REPLACE, GL30.GL_REPLACE, GL30.GL_REPLACE); //Only used once - currently. So it can be global.
		//First time initialization of buffer, it might be wrong on startup:
		GL30.glStencilMask(0x11);
		GL30.glClear(GL30.GL_STENCIL_BUFFER_BIT);
		GL30.glStencilMask(0x00);
	}
	
	private static void render()
	{
		worldView.render();
		interactables.render();
	}
	
	public static void setBackgroundColor()
	{
		GL30.glClearColor(Settings.backgroundColorR, Settings.backgroundColorG, Settings.backgroundColorB, 0.0f);
	}
	
	public static void clear()
	{
		GL30.glClear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT);
	}
}

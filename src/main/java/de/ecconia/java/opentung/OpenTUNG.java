package de.ecconia.java.opentung;

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
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import org.lwjgl.Version;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL30;

public class OpenTUNG
{
	public static final File dataFolder = new File("OpenTUNG");
	public static final File boardFolder = new File(dataFolder, "boards");
	
	private static final int initialWidth = 800;
	private static final int initialHeight = 600;
	
	private static InputProcessor inputHandler;
	
	private static RenderPlane2D interactables;
	private static RenderPlane3D worldView;
	
	private static File boardFile;
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
		
		try
		{
			System.out.println("Using data folder at: " + dataFolder.getCanonicalPath());
			Files.createDirectories(dataFolder.toPath());
		}
		catch(IOException e)
		{
			throw new RuntimeException("Could not create data folder.", e);
		}
		
		//Create DataFolderWatcher, used for generic callbacks on file change.
		DataFolderWatcher watcher = new DataFolderWatcher(dataFolder);
		//Create the initial Settings loader, it uses the watcher to keep the settings up to date.
		new SettingsIO(new File(dataFolder, "settings.txt"), watcher, Settings.class);
		
		try
		{
			parseArguments(args);
		}
		catch(IOException e)
		{
			System.out.println("Issues accessing/using data folder, please report stacktrace.");
			e.printStackTrace();
		}
		
		if(boardFile.getName().endsWith(".opentung"))
		{
			boardUniverse = new BoardUniverse(Loader.load(boardFile));
		}
		else
		{
			boardUniverse = new BoardUniverse(TungBoardLoader.importTungBoard(boardFile));
		}
		
		SharedData sharedData = new SharedData(boardUniverse, boardFile);
		
		System.out.println("Starting GUI...");
		try
		{
			System.out.println("LWJGL version: " + Version.getVersion());
			
			SWindowWrapper window = new SWindowWrapper(initialWidth, initialHeight, "OpenTUNG FPS: ? | TPS: ? | avg. UPT: ? | " + boardFile.getName());
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
				window.setTitle("OpenTUNG FPS: " + sharedData.getFPS() + " | TPS: " + boardUniverse.getSimulation().getTPS() + " | avg. UPT: " + boardUniverse.getSimulation().getLoad() + " | " + sharedData.getCurrentBoardFile().getName());
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
	
	private static void parseArguments(String[] args) throws IOException
	{
		if(!boardFolder.exists())
		{
			Files.createDirectory(boardFolder.toPath());
		}
		if(!boardFolder.isDirectory())
		{
			System.out.println("Could not create 'boards' folder inside data folder. Please remove board file: " + boardFolder.getAbsolutePath());
			System.exit(1);
		}
		
		String defaultBoardName = null;
		if(args.length != 0)
		{
			if(args.length != 1)
			{
				System.out.println("If you have multiple board files in your 'boards' folder, only supply the filename of one.");
				System.out.println(" It mustn't contain spaces and must end on '.tungboard' or '.opentung'. You may not provide relative/absolute paths."); //Cause I am too lazy to add a command parsing framework or write one myself - rn.
				System.out.println();
				System.exit(1);
			}
			else
			{
				String argument = args[0];
				if(argument.endsWith(".tungboard") || argument.endsWith(".opentung"))
				{
					defaultBoardName = argument;
				}
			}
		}
		
		if(defaultBoardName != null)
		{
			File boardFile = new File(boardFolder, defaultBoardName);
			if(!boardFile.exists())
			{
				System.out.println("TungBoard/OpenTUNG file " + boardFile.getAbsolutePath() + " cannot be found.");
				System.exit(1);
			}
			OpenTUNG.boardFile = boardFile;
		}
		else
		{
			List<File> tungboardFiles = Arrays.stream(boardFolder.listFiles())
					.filter((File file) ->
							file.getName().endsWith(".tungboard") | file.getName().endsWith(".opentung"))
					.collect(Collectors.toList());
			if(tungboardFiles.isEmpty())
			{
				String defaultFileName = "emptyBoard.opentung";
				File target = new File(boardFolder, defaultFileName);
				InputStream link = (OpenTUNG.class.getClassLoader().getResourceAsStream(defaultFileName));
				Files.copy(link, target.getAbsoluteFile().toPath());
				System.out.println("Created and using default board: " + defaultFileName);
				boardFile = target;
			}
			else if(tungboardFiles.size() == 1)
			{
				boardFile = tungboardFiles.get(0);
			}
			else
			{
				System.out.println("Found more than one board file in the 'boards' folder.");
				System.out.println("Rename others or supply the filename of the desired '.tungboard'/'.opentung' file as argument.");
				System.exit(1);
			}
		}
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

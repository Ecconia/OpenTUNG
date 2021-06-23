package de.ecconia.java.opentung;

import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.core.BoardUniverse;
import de.ecconia.java.opentung.core.OpenTUNGVersion;
import de.ecconia.java.opentung.core.RenderPlane3D;
import de.ecconia.java.opentung.core.data.ShaderStorage;
import de.ecconia.java.opentung.core.data.SharedData;
import de.ecconia.java.opentung.inputs.InputProcessor;
import de.ecconia.java.opentung.interfaces.RenderPlane2D;
import de.ecconia.java.opentung.libwrap.SWindowWrapper;
import de.ecconia.java.opentung.savefile.Loader;
import de.ecconia.java.opentung.settings.DataFolderWatcher;
import de.ecconia.java.opentung.settings.Settings;
import de.ecconia.java.opentung.settings.SettingsIO;
import de.ecconia.java.opentung.settings.keybinds.KeybindingsIO;
import de.ecconia.java.opentung.settings.keybinds.manager.KeybindingManager;
import de.ecconia.java.opentung.tungboard.TungBoardLoader;
import de.ecconia.java.opentung.util.logging.LogStreamHandler;
import de.ecconia.java.opentung.util.math.Quaternion;
import de.ecconia.java.opentung.util.math.Vector3;
import java.awt.Dimension;
import java.io.IOException;
import java.nio.file.Path;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.lwjgl.Version;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL30;

public class OpenTUNG
{
	//TODO: Load from settings:
	private static final int initialWidth = 800;
	private static final int initialHeight = 600;
	
	private static InputProcessor inputHandler;
	private static RenderPlane2D interactables;
	private static RenderPlane3D worldView;
	private static BoardUniverse boardUniverse;
	private static ShaderStorage shaderStorage;
	
	//Currently stored here, until X11 is workarounded (as in AWT removed):
	private static SharedData sharedData;
	private static String fileName = "<unknown>";
	
	public static OpenTUNGBootstrap bootstrap;
	
	public static void main(String[] args)
	{
		//Catch if any thread shuts down unexpectedly. Print on output stream to get the exact time.
		Thread.setDefaultUncaughtExceptionHandler((Thread t, Throwable e) -> {
			String threadCrashMessage = "Thread " + t.getName() + " crashed.";
			System.out.println(threadCrashMessage);
			e.printStackTrace(System.out);
			JOptionPane.showMessageDialog(null, threadCrashMessage + " See console for stacktrace, please report it.");
		});
		
		bootstrap = new OpenTUNGBootstrap(args);
		if(bootstrap.getFinalTarget() == OpenTUNGBootstrap.FinalTarget.Version)
		{
			OpenTUNGVersion version = new OpenTUNGVersion();
			System.out.println("[Version] Printing OpenTUNG version:");
			System.out.println("[Version]  Current Git branch: " + version.getGitBranch());
			System.out.println("[Version]  Last Git commit hash: " + version.getGitCommitHash());
			System.out.println("[Version]  Last Git commit title: " + version.getGitCommitTitle());
			System.out.println("[Version]  Git got modified: " + version.isGitDirty());
			System.out.println("[Version]  Build date/time: " + version.getBuildDateTime());
			String versionString = "git-" + version.getGitCommitHash() + (version.isGitDirty() ? "-dirty" : "") + (!version.getGitBranch().equals("master") ? " (" + version.getGitBranch() + ")" : "");
			System.out.println("[Version]  - Final version code: " + versionString);
			return;
		}
		
		//Enables/manages the output stream handling and logging to file:
		LogStreamHandler logHandler = new LogStreamHandler();
		{
			//Print the run arguments to file. Although this one will not have a prefix. It can be skipped easily.
			if(args.length != 0)
			{
				logHandler.justAddToFile("Arguments (" + args.length + "): '" + String.join("', '", args) + "'");
			}
			else
			{
				logHandler.justAddToFile("Arguments (0).");
			}
		}
		
		//Load version information:
		OpenTUNGVersion version = new OpenTUNGVersion();
		String versionString = "Running OpenTUNG Version: git-" + version.getGitCommitHash() + (version.isGitDirty() ? "-dirty" : "") + (!version.getGitBranch().equals("master") ? " (" + version.getGitBranch() + ")" : "");
		System.out.println(versionString);
		System.out.println("Running OpenTUNG in folder: " + bootstrap.getDataFolder());
		
		{
			//TODO: Practically the arming can now be done directly. Since setting up the folder is no longer logged.
			//Folders are a thing, enable logging to file:
			String logFileName = LogStreamHandler.claimDefaultLogFileName(bootstrap.getLogsFolder());
			if(logFileName == null)
			{
				System.exit(1); //No do not allow this.
			}
			System.out.println("[Logging] Claimed logfile name: " + logFileName);
			try
			{
				logHandler.armFileLogger(bootstrap.getLogsFolder(), logFileName);
			}
			catch(IOException e)
			{
				System.out.println("[Logging] Exception, while enabling file-logger:");
				e.printStackTrace(System.out);
				System.exit(1);
			}
		}
		
		if(bootstrap.getFinalTarget() == OpenTUNGBootstrap.FinalTarget.Keybindings)
		{
			new KeybindingManager(bootstrap.getKeybindingFile());
			return;
		}
		
		Path toLoadFile = bootstrap.getFile();
		final boolean x11LoadLaterFix; //X11 has a bug combined with OpenGL, which lets OpenTUNG crash later on. Thus if on Linux, do not open the chooser before the main window exists.
		if(bootstrap.getFinalTarget() == OpenTUNGBootstrap.FinalTarget.Chooser)
		{
			String osName = System.getProperty("os.name").toLowerCase();
			System.out.println("[Debug] Os-Name: " + osName);
			if(osName.contains("nix") || osName.contains("nux") || osName.indexOf("aix") > 0) //Detect Linux
			{
				x11LoadLaterFix = true;
			}
			else
			{
				x11LoadLaterFix = false;
				toLoadFile = loadFileGUI();
				if(toLoadFile == null)
				{
					System.exit(1);
				}
			}
		}
		else
		{
			x11LoadLaterFix = false;
		}
		
		//Create DataFolderWatcher, used for generic callbacks on file change.
		DataFolderWatcher watcher = new DataFolderWatcher(bootstrap.getDataFolder());
		//Create the initial Settings loader, it uses the watcher to keep the settings up to date.
		new SettingsIO(bootstrap.getSettingsFile(), watcher, Settings.class);
		
		if(!x11LoadLaterFix)
		{
			populateBoard(toLoadFile);
		}
		
		System.out.println("Starting GUI...");
		try
		{
			System.out.println("LWJGL version: " + Version.getVersion());
			
			SWindowWrapper window = new SWindowWrapper(initialWidth, initialHeight, "OpenTUNG FPS: ? | TPS: ? | avg. UPT: ? | " + fileName);
			inputHandler = new InputProcessor(window.getID());
			
			//Has to be done now, since before here scancode resolving does not work.
			new KeybindingsIO(bootstrap.getKeybindingFile(), watcher);
			
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
					
					if(x11LoadLaterFix)
					{
						//Since AWT makes OpenGL crash if you use it before the call of GL.createCapabilities, move it to here.
						Path path = loadFileGUI();
						if(path == null)
						{
							System.exit(1);
							return; //Satisfy compiler.
						}
						populateBoard(path);
					}
					
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
									e.printStackTrace(System.out); //Should never happen though.
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
					e.printStackTrace(System.out);
					JOptionPane.showMessageDialog(null, "Graphic thread crashed. Stopping OpenTUNG. See console for the cause and report it.");
					boardUniverse.getSimulation().interrupt();
					inputHandler.stop();
					System.exit(1); //Throw 1;
				}
			}, "GraphicThread");
			graphicsThread.start();
			
			//Let main-thread execute the input handler:
			Thread.currentThread().setName("Main/Input");
			inputHandler.eventPollEntry(() -> {
				if(sharedData == null)
				{
					//If the window is not yet ready, this would cause issues.
					return;
				}
				Path savePath = sharedData.getCurrentBoardFile();
				window.setTitle("OpenTUNG FPS: " + sharedData.getFPS() + " | TPS: " + boardUniverse.getSimulation().getTPS() + " | avg. UPT: " + boardUniverse.getSimulation().getLoad() + " | " + (savePath == null ? "<unsaved>" : savePath.getFileName().toString()));
			});
			System.out.println("Main/Input thread has turned off.");
		}
		catch(Exception e)
		{
			e.printStackTrace(System.out);
			inputHandler.stop();
			System.exit(1); //Throw 1;
		}
	}
	
	private static void populateBoard(Path toLoadFile)
	{
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
		
		sharedData = new SharedData(boardUniverse, toLoadFile);
	}
	
	private static Path loadFileGUI()
	{
		JFileChooser fileChooser = new JFileChooser(bootstrap.getBoardFolder().toFile());
		int result = fileChooser.showSaveDialog(null);
		if(result != JFileChooser.APPROVE_OPTION)
		{
			System.out.println("[SaveFileChooser] Nothing chosen, terminating.");
			return null;
		}
		Path currentSaveFile = fileChooser.getSelectedFile().toPath();
		
		if(hasCorrectFileEnding(currentSaveFile.getFileName().toString()))
		{
			return currentSaveFile;
		}
		else
		{
			System.out.println("[SaveFileChooser] Choose '" + currentSaveFile + "', missing or incorrect file ending.");
			JOptionPane.showMessageDialog(null, "File-ending must be '.opentung' or '.tungboard'.", "Can only load TUNG and OpenTUNG files.", JOptionPane.ERROR_MESSAGE, null);
			return null;
		}
	}
	
	private static boolean hasCorrectFileEnding(String filename)
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
	
	private static boolean failed = false;
	private static boolean doNotShowPopupAgain = false;
	
	private static void render() throws InterruptedException
	{
		Throwable throwable = null;
		try
		{
			worldView.render();
		}
		catch(Throwable t)
		{
			throwable = t;
			System.out.println("World rendering failed! Skipping this cycle. Stacktrace:");
			t.printStackTrace(System.out);
		}
		try
		{
			interactables.render();
		}
		catch(Throwable t)
		{
			throwable = t;
			System.out.println("Interface rendering failed! Skipping this cycle. Stacktrace:");
			t.printStackTrace(System.out);
		}
		
		if(throwable == null)
		{
			failed = false;
		}
		else
		{
			if(!failed)
			{
				failed = true;
				if(!doNotShowPopupAgain)
				{
					doNotShowPopupAgain = true;
					JOptionPane.showMessageDialog(null, "Could not render current frame, stacktrace happened. Please check console and report the issue! This message will not appear again.");
				}
			}
			else
			{
				Thread.sleep(400); //Throttling the amount of spam in console (and thus the framerate too...)
			}
		}
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

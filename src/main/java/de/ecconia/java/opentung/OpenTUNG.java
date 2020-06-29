package de.ecconia.java.opentung;

import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.crapinterface.RenderPlane2D;
import de.ecconia.java.opentung.inputs.InputProcessor;
import de.ecconia.java.opentung.libwrap.SWindowWrapper;
import de.ecconia.java.opentung.tungboard.TungBoardLoader;
import java.awt.Dimension;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.lwjgl.Version;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL30;

public class OpenTUNG
{
	private static int fps = 0;
	private static InputProcessor inputHandler;
	
	private static RenderPlane2D interactables;
	private static RenderPlane3D worldView;
	
	private static File boardFile;
	private static CompBoard board;
	private static BoardUniverse boardUniverse;
	
	public static void main(String[] args)
	{
		parseArguments(args);
		
		//Catch if any thread shuts down unexpectedly. Print on output stream to get the exact time.
		Thread.setDefaultUncaughtExceptionHandler((Thread t, Throwable e) -> {
			System.out.println("Thread " + t.getName() + " crashed.");
			e.printStackTrace(System.out);
		});
		
		board = TungBoardLoader.importTungBoard(boardFile);
		boardUniverse = new BoardUniverse(board);
		
		System.out.println("Starting GUI...");
		try
		{
			System.out.println("LWJGL version: " + Version.getVersion());
			
			SWindowWrapper window = new SWindowWrapper(500, 500, "OpenTUNG FPS: ? TPS: ? SimLoad: ?");
			inputHandler = new InputProcessor(window.getID());
			
			Thread graphicsThread = new Thread(() -> {
				try
				{
					//Grab the graphic context for OpenGL on this thread.
					window.grabContext();
					window.setVsync(fps == 0);
					window.place();
					
					//OpenGL:
					GL.createCapabilities();
					System.out.println("OpenGL version: " + GL30.glGetString(GL30.GL_VERSION));
					System.out.println("Amount of connection-groups per mesh: " + GL30.glGetInteger(GL30.GL_MAX_VERTEX_UNIFORM_COMPONENTS));
					
					init();
					
					long past = System.currentTimeMillis();
					int finishedRenderings = 0;
					
					long frameDuration = fps != 0 ? 1000L / (long) fps : 1;
					long lastFinishedRender = System.currentTimeMillis();
					
					while(!window.shouldClose())
					{
						Dimension newSize = window.getNewDimension();
						if(newSize != null)
						{
							GL30.glViewport(0, 0, newSize.width, newSize.height);
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
							window.setTitle("OpenTUNG FPS: " + finishedRenderings + " TPS: " + boardUniverse.getSimulation().getTPS() + " SimLoad: " + boardUniverse.getSimulation().getLoad());
							finishedRenderings = 0;
						}
						
						//FPS limiting:
						if(fps != 0)
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
			inputHandler.eventPollEntry();
			System.out.println("Main/Input thread has turned off.");
		}
		catch(Exception e)
		{
			e.printStackTrace();
			inputHandler.stop();
			System.exit(1); //Throw 1;
		}
	}
	
	private static void parseArguments(String[] args)
	{
		String downloadLink = "https://cdn.discordapp.com/attachments/428658408510455810/725623552161611786/16Bit-Parallel-CLA-ALU-6-ticks.tungboard";
		String messageLink = "https://discordapp.com/channels/401255675264761866/428658408510455810/725623552358875208";
		
		File boardFolder = new File("boards");
		if(!boardFolder.exists() || !boardFolder.isDirectory())
		{
			System.out.println("Please create folder " + boardFolder.getAbsolutePath() + " and insert a .tungboard file.");
			System.out.println();
			System.out.println("-> Recommended '.tungboard' file to use can be downloaded here: " + downloadLink);
			System.out.println("-> If you want to confirm the source, use this link: " + messageLink);
			System.exit(1);
		}
		
		String defaultBoardName = null;
		out:
		if(args.length != 0)
		{
			if(args.length == 1)
			{
				String argument = args[0];
				if(argument.endsWith(".tungboard"))
				{
					defaultBoardName = argument;
					break out;
				}
			}
			System.out.println("If you have multiple tungboard files in your 'boards' folder, only supply the filename of one.");
			System.out.println(" It mustn't contain spaces and must end on '.tungboard'. You may not provide relative/absolute paths."); //Cause I am too lazy to add a command parsing framework or write one myself - rn.
			System.out.println();
			System.out.println("-> Recommended '.tungboard' file to use can be downloaded here: " + downloadLink);
			System.out.println("-> If you want to confirm the source, use this link: " + messageLink);
			System.exit(1);
		}
		
		if(defaultBoardName != null)
		{
			File tungboardFile = new File(boardFolder, defaultBoardName);
			if(!tungboardFile.exists())
			{
				System.out.println("TungBoard file " + tungboardFile.getAbsolutePath() + " cannot be found.");
				System.exit(1);
			}
			boardFile = tungboardFile;
		}
		else
		{
			List<File> tungboardFiles = Arrays.stream(boardFolder.listFiles()).filter((File file) -> {
				return file.getName().endsWith(".tungboard");
			}).collect(Collectors.toList());
			
			if(tungboardFiles.isEmpty())
			{
				System.out.println("No '.tungboard' file in the 'boards' folder, please insert one.");
				System.out.println();
				System.out.println("-> Recommended '.tungboard' file to use can be downloaded here: " + downloadLink);
				System.out.println("-> If you want to confirm the source, use this link: " + messageLink);
				System.exit(1);
			}
			else if(tungboardFiles.size() == 1)
			{
				boardFile = tungboardFiles.get(0);
			}
			else
			{
				System.out.println("Found more than one tungboard file in the 'boards' folder.");
				System.out.println("Rename others or supply the filename of the desired '.tungboard' file as argument.");
				System.exit(1);
			}
		}
	}
	
	private static void init()
	{
		setBackgroundColor();
		
		GL30.glEnable(GL30.GL_CULL_FACE);
		GL30.glFrontFace(GL30.GL_CCW);
		GL30.glCullFace(GL30.GL_BACK);
		
		GL30.glEnable(GL30.GL_DEPTH_TEST);
		GL30.glDepthFunc(GL30.GL_LESS);
		
		GL30.glEnable(GL30.GL_BLEND);
		GL30.glBlendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);
		
		GL30.glEnable(GL30.GL_STENCIL_TEST);
		GL30.glStencilFunc(GL30.GL_NOTEQUAL, 1, 0xFF);
		GL30.glStencilOp(GL30.GL_KEEP, GL30.GL_KEEP, GL30.GL_REPLACE);
		
		interactables = new RenderPlane2D(inputHandler);
		interactables.setup();
		interactables.newSize(500, 500);
		worldView = new RenderPlane3D(inputHandler, boardUniverse);
		worldView.setup();
		worldView.newSize(500, 500);
	}
	
	private static void render()
	{
		worldView.render();
		interactables.render();
	}
	
	public static void setBackgroundColor()
	{
		GL30.glClearColor(1f / 255f * 54f, 1f / 255f * 57f, 1f / 255f * 63f, 0.0f); //Discord
//		GL30.glClearColor(1f / 255f * 150f, 1f / 255f * 150f, 1f / 255f * 150f, 0.0f); //Bright
	}
	
	public static void clear()
	{
		GL30.glStencilMask(0xFF); //Set stencil mode, to properly clear the buffer.
		GL30.glClear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT | GL30.GL_STENCIL_BUFFER_BIT);
		GL30.glStencilMask(0x00); //Unset stencil mode
	}
}

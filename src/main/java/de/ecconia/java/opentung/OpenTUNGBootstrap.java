package de.ecconia.java.opentung;

import de.ecconia.java.opentung.core.systems.Skybox;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

public class OpenTUNGBootstrap
{
	public static final String argKeybindings = "--keybindings";
	private static final String argChooser = "--chooser";
	private static final String argVersion = "--version";
	private static final String argHelp = "--help";
	private static final String argFolder = "--folder";
	private static final String argLoad = "--load";
	
	public enum FinalTarget
	{
		Help,
		Version,
		Keybindings,
		Chooser,
		Load,
		New,
	}
	
	private FinalTarget finalTarget;
	
	private boolean isKeybindings;
	private boolean isChooser;
	private boolean isVersion;
	private boolean isHelp;
	private Path folder;
	private Path file;
	
	private Path logsFolder;
	private Path boardFolder;
	private Path settingsFile;
	private Path keybindingFile;
	
	public OpenTUNGBootstrap(String[] arguments)
	{
		if(parseArguments(arguments))
		{
			printHelp();
			System.exit(1);
			return;
		}
		
		determineIntention();
		if(finalTarget == FinalTarget.Help)
		{
			printHelp();
			System.exit(0);
			return;
		}
		if(finalTarget == FinalTarget.Version)
		{
			return; //The calling code will take this one over. It is a terminating argument, so no further validation.
		}
		
		//From here on all other targets use the provided data folder, thus validate it:
		prepareFolder();
		
		if(finalTarget == FinalTarget.Load)
		{
			checkSaveFile();
		}
	}
	
	private boolean parseArguments(String[] arguments)
	{
		boolean isInvalid = false;
		for(int i = 0; i < arguments.length; i++)
		{
			boolean isLastArgument = (i + 1) == arguments.length;
			String argument = arguments[i].toLowerCase();
			if(argKeybindings.equals(argument))
			{
				if(isKeybindings)
				{
					//Warn/Ignore: Redundant.
					parserError("Warning: Already provided '" + argKeybindings + "' argument.");
				}
				isKeybindings = true;
			}
			else if(argChooser.equals(argument))
			{
				if(isChooser)
				{
					//Warn/Ignore: Redundant.
					parserError("Warning: Already provided '" + argChooser + "' argument.");
				}
				isChooser = true;
			}
			else if(argVersion.equals(argument))
			{
				if(isVersion)
				{
					//Warn/Ignore: Redundant.
					parserError("Warning: Already provided '" + argVersion + "' argument.");
				}
				isVersion = true;
			}
			else if(argHelp.equals(argument))
			{
				if(isHelp)
				{
					//Warn/Ignore: Redundant.
					parserError("Warning: Already provided '" + argHelp + "' argument.");
				}
				isHelp = true;
			}
			else if(argFolder.equals(argument))
			{
				if(folder != null)
				{
					//Error: Already supplied folder path.
					parserError("Syntax error: Already provided '" + argFolder + "' argument.");
					isInvalid = true;
					break;
				}
				//Next argument must be the folder path:
				if(isLastArgument)
				{
					//Error: Expected another argument.
					parserError("Syntax error: Expected another argument after '" + argFolder + "'.");
					isInvalid = true;
					break;
				}
				argument = arguments[++i];
				try
				{
					folder = Paths.get(argument);
				}
				catch(InvalidPathException e)
				{
					//Error: Cannot parse the argument.
					parserError("Error: Cannot parse supplied folder path: '" + argument + "'");
					isInvalid = true;
				}
			}
			else if(argLoad.equals(argument))
			{
				if(file != null)
				{
					//Error: Already supplied folder path.
					parserError("Syntax error: Already provided '" + argLoad + "' argument.");
					isInvalid = true;
				}
				//Next argument must be the folder path:
				if(isLastArgument)
				{
					//Error: Expected another argument.
					parserError("Syntax error: Expected another argument after '" + argLoad + "'.");
					isInvalid = true;
					break;
				}
				argument = arguments[++i];
				try
				{
					file = Paths.get(argument);
				}
				catch(InvalidPathException e)
				{
					//Error: Cannot parse the argument.
					parserError("Error: Cannot parse supplied file path: '" + argument + "'");
					isInvalid = true;
				}
			}
			else
			{
				argument = arguments[i]; //Do not apply lowercase.
				if(!isLastArgument)
				{
					//Error: Got an unknown argument as not last argument.
					parserError("Error: Unknown argument '" + argument + "'.");
					isInvalid = true;
					continue;
				}
				if(file != null)
				{
					parserError("Syntax error: Already provided '" + argLoad + "' path.");
					isInvalid = true;
					continue;
				}
				try
				{
					file = Paths.get(argument);
				}
				catch(InvalidPathException e)
				{
					//Error: Cannot parse the argument.
					parserError("Error: Cannot parse supplied file path: '" + argument + "'");
					isInvalid = true;
				}
			}
		}
		
		return isInvalid;
	}
	
	private void determineIntention()
	{
		boolean isFolder = folder != null;
		boolean isFile = file != null;
		if(isHelp)
		{
			finalTarget = FinalTarget.Help;
			if(isChooser || isVersion || isKeybindings || isFolder || isFile)
			{
				parserError("Warning: '" + argHelp + "' cannot be combined with other arguments.");
			}
		}
		else if(isVersion)
		{
			finalTarget = FinalTarget.Version;
			if(isChooser || isKeybindings || isFolder || isFile)
			{
				parserError("Warning: '" + argVersion + "' cannot be combined with other arguments.");
			}
		}
		else if(isKeybindings)
		{
			finalTarget = FinalTarget.Keybindings;
			if(isChooser || isFile)
			{
				parserError("Warning: '" + argKeybindings + "' cannot be combined " + argChooser + " and " + argLoad + ".");
			}
		}
		else if(isChooser)
		{
			finalTarget = FinalTarget.Chooser;
			if(isFile)
			{
				parserError("Warning: '" + argChooser + "' cannot be combined with " + argLoad + ".");
			}
		}
		else if(isFile)
		{
			finalTarget = FinalTarget.Load;
		}
		else
		{
			finalTarget = FinalTarget.New;
		}
	}
	
	private void printHelp()
	{
		System.out.println("[Help/Usage] Usage: java -jar OpenTUNG.jar [--arguments] [save-file to load]");
		System.out.println("[Help/Usage] Arguments:");
		System.out.println("[Help/Usage]  --help          : Prints this help.");
		System.out.println("[Help/Usage]  --version       : Print detailed version.");
		System.out.println("[Help/Usage]  --folder <path> : Change the data folder from <here>/OpenTUNG to a different existing folder. This will be respected by the following arguments.");
		System.out.println("[Help/Usage]  --keybindings   : Opens the keybindings manager.");
		System.out.println("[Help/Usage]  --chooser       : Opens the a window to select a save-file to load.");
		System.out.println("[Help/Usage]  --load <path>   : Set path to a save-file to load.");
		System.out.println("[Help/Usage] Not providing any argument (or only the --folder argument) will create a new empty board.");
		System.out.println("[Help/Usage]  Arguments cannot be combined, the only exception is the --folder argument.");
		System.out.println("[Help/Usage] If your <path> arguments contain spaces you have to wrap them with quotes.");
		System.out.println("[Help/Usage] You can omit the file-ending of your save-file to load.");
		System.out.println("[Help/Usage]  OpenTUNG will attempt the file ending '.opentung' and then '.tungboard'.");
		System.out.println("[Help/Usage]  OpenTUNG will attempt to find the save files in the board folder first and then in the data folder.");
	}
	
	private void prepareFolder()
	{
		if(folder != null)
		{
			if(!Files.exists(folder))
			{
				parserError("[DataFolderCreation] Error: Provided data folder does not exist. You have to create the custom data folder yourself, to prevent misunderstandings.");
				parserError("[DataFolderCreation]  Provided data folder path was: " + folder);
				parserError("[DataFolderCreation]  Execution directory is: " + System.getProperty("user.dir"));
				System.exit(1);
			}
		}
		else
		{
			folder = Paths.get("OpenTUNG");
			if(!Files.exists(folder))
			{
				try
				{
					System.out.println("[DataFolderCreation] Default data folder 'OpenTUNG' not found in '" + System.getProperty("user.dir") + "', creating.");
					Files.createDirectory(folder);
				}
				catch(IOException e)
				{
					System.out.println("[DataFolderCreation] Could not create folder, cause an exception occurred:");
					e.printStackTrace(System.out);
					System.exit(1);
				}
			}
			else if(!Files.isDirectory(folder))
			{
				try
				{
					folder = folder.toRealPath(LinkOption.NOFOLLOW_LINKS);
				}
				catch(IOException e)
				{
					System.out.println("[DataFolderCreation] Attempted to print real/absolute path, but got an exception (main error after stacktrace):");
					e.printStackTrace(System.out);
				}
				System.out.println("[DataFolderCreation] Error: Data folder is a file, thus cannot be used. Please remove data file: '" + folder + "'.");
				System.exit(1);
			}
		}
		
		//Now that we have the folder, make its path absolute:
		try
		{
			folder = folder.toRealPath(LinkOption.NOFOLLOW_LINKS);
		}
		catch(IOException e)
		{
			System.out.println("[DataFolderCreation] Could not resolve data folders real/absolute path for '" + folder + "', cause an exception occurred:");
			e.printStackTrace(System.out);
			System.exit(1);
		}
		
		//Create sub-folders:
		logsFolder = createSubFolder("logs");
		boardFolder = createSubFolder("boards");
		settingsFile = ensureNotFolder("settings.txt");
		keybindingFile = ensureNotFolder("keybindings.txt");
		try
		{
			Skybox.prepareDataFolder(folder);
		}
		catch(IOException e)
		{
			System.out.println("[DataFolderCreation] Could not create skybox folder or could not copy default skybox because of an exception:");
			e.printStackTrace(System.out);
			System.exit(1);
		}
	}
	
	private Path createSubFolder(String name)
	{
		Path path = folder.resolve(name);
		if(!Files.exists(path))
		{
			try
			{
				System.out.println("[DataFolderCreation] Folder '" + name + "' does not exist, creating.");
				Files.createDirectory(path);
			}
			catch(IOException e)
			{
				System.out.println("[DataFolderCreation] Exception while creating folder '" + path + "':");
				e.printStackTrace(System.out);
				System.exit(1);
			}
		}
		else if(!Files.isDirectory(path))
		{
			System.out.println("[DataFolderCreation] Folder '" + path + "', cannot be created because it is a file. Delete or move that file.");
			System.exit(1);
		}
		return path;
	}
	
	private Path ensureNotFolder(String name)
	{
		Path path = folder.resolve(name);
		if(Files.isDirectory(path))
		{
			System.out.println("[DataFolderCreation] File '" + path + "' is a directory, thus cannot be used. Please remove/rename/move it.");
			System.exit(1);
		}
		return path;
	}
	
	private void checkSaveFile()
	{
		//Check if the file has a known file-ending, if not it might be omitted.
		boolean hasFileType = false;
		String fileName = file.getFileName().toString();
		int endingIndex = fileName.lastIndexOf('.');
		if(endingIndex >= 0)
		{
			String ending = fileName.substring(endingIndex + 1).toLowerCase();
			hasFileType = ending.equals("opentung") || ending.equals("tungboard");
		}
		
		//Attempt to find the file, adding file-extensions and checking data and boards folder:
		Path ret;
		if(hasFileType)
		{
			ret = findSavefile(file);
		}
		else
		{
			ret = findSavefile(file.resolveSibling(fileName + ".opentung"));
			if(ret == null)
			{
				ret = findSavefile(file.resolveSibling(fileName + ".tungboard"));
			}
		}
		
		//Did not find the file, print error message:
		if(ret == null)
		{
			//Cannot find file.
			if(hasFileType)
			{
				System.out.println("[SaveFileResolver] Cannot find provided file to load: '" + file + "'.");
			}
			else
			{
				System.out.println("[SaveFileResolver] Cannot find provided file to load '" + file + "', attempted to use '.opentung' and '.tungboard' file-extensions.");
			}
			System.out.println("[SaveFileResolver]  Looked in '" + boardFolder + "'");
			System.out.println("[SaveFileResolver]        and '" + System.getProperty("user.dir") + "'");
			System.exit(1);
		}
		//Found the file, apply the fixed path:
		file = ret;
		
		//Now make the path of the file absolut, if it is not already:
		try
		{
			file = file.toRealPath(LinkOption.NOFOLLOW_LINKS);
		}
		catch(IOException e)
		{
			System.out.println("[SaveFileResolver] Failed to resolve save-file path to full/absolute path. File path: '" + file + "'.");
			e.printStackTrace(System.out);
			System.exit(1);
		}
	}
	
	private Path findSavefile(Path path)
	{
		if(path.isAbsolute())
		{
			if(Files.exists(path))
			{
				return path;
			}
		}
		else
		{
			//First check the boards folder.
			Path boardRelative = boardFolder.resolve(path);
			if(Files.exists(boardRelative))
			{
				return boardRelative;
			}
			//If not in the boards folder, it could be relative to the executing location.
			if(Files.exists(path))
			{
				//It exists, make its path absolute.
				try
				{
					path = path.toRealPath(LinkOption.NOFOLLOW_LINKS);
				}
				catch(IOException e)
				{
					System.out.println("[DataFolderCreation] Attempted to print real/absolute path of save-file '" + path + "', but got an exception (main error after stacktrace):");
					e.printStackTrace(System.out);
				}
				return path;
			}
		}
		return null; //Does not exist.
	}
	
	private void parserError(String message)
	{
		System.out.println("[Argument parser] " + message);
	}
	
	//### Getters: ###
	
	public FinalTarget getFinalTarget()
	{
		return finalTarget;
	}
	
	public Path getFile()
	{
		return file;
	}
	
	public Path getDataFolder()
	{
		return folder;
	}
	
	public Path getBoardFolder()
	{
		return boardFolder;
	}
	
	public Path getLogsFolder()
	{
		return logsFolder;
	}
	
	public Path getKeybindingFile()
	{
		return keybindingFile;
	}
	
	public Path getSettingsFile()
	{
		return settingsFile;
	}
}

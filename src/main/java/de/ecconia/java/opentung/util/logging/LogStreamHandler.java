package de.ecconia.java.opentung.util.logging;

import de.ecconia.java.opentung.util.logging.stream.CachingFileLogger;
import de.ecconia.java.opentung.util.logging.stream.PrintLineEnforcer;
import de.ecconia.java.opentung.util.logging.stream.PrintLineFormatter;
import de.ecconia.java.opentung.util.logging.stream.SplitStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class LogStreamHandler
{
	private final CachingFileLogger fileLogger;
	
	public LogStreamHandler()
	{
		//First do what has to be called last in the chain. So lower priority first here. Higher priority last. It is a stack.
		
		//Store the original output stream, to use it when something goes horribly wrong with the file-writer. (When it throws exceptions).
		PrintStream originalStream = System.out;
		//Create the file-writer which gonna cache output and then store it in a file.
		fileLogger = new CachingFileLogger(originalStream);
		//Split the stream to console and file-logger.
		System.setOut(new SplitStream(System.out, fileLogger));
		//Format the lines arriving here. Adds a nice prefix to them:
		System.setOut(new PrintLineFormatter(System.out));
		//Stops every output which is not a line with an exception, these are not tollerated in this project:
		System.setOut(new PrintLineEnforcer(System.out));
		//Finally map every error stream output onto the normal stream output.
		// That way they will be included in the logging at a thread-safe position.
		System.setErr(System.out);
	}
	
	public void justAddToFile(String message)
	{
		fileLogger.println(message);
	}
	
	public void armFileLogger(Path folder, String name) throws IOException
	{
		Path logFile = folder.resolve(name + ".log");
		Path zipFile = folder.resolve(name + ".zip");
		FileWriter fileWriter = new FileWriter(logFile.toFile());
		fileLogger.arm(fileWriter, logFile, zipFile);
	}
	
	public static String claimDefaultLogFileName(Path folder)
	{
		ZonedDateTime zonedDateTimeCurrent = ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault());
		DateTimeFormatter dateConverter = DateTimeFormatter.ofPattern("uuuu-MM-dd");
		String name = dateConverter.format(zonedDateTimeCurrent) + "-";
		
		int index = 0;
		while(true)
		{
			try
			{
				String fileName = name + index++;
				Path logPath = folder.resolve(fileName + ".log");
				Files.createFile(logPath); //Won this one.
				if(Files.exists(folder.resolve(fileName + ".zip")))
				{
					//Oh no a zip already exists for this one, continue... (The rare case which may happen within 1 second!!)
					Files.delete(logPath); //Undo the creation of this log.
					continue;
				}
				return fileName;
			}
			catch(FileAlreadyExistsException e)
			{
				//Do nothing, this exception is expected.
			}
			catch(Exception e)
			{
				e.printStackTrace(System.out);
				return null;
			}
		}
	}
}

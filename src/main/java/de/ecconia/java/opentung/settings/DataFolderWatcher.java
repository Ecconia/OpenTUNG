package de.ecconia.java.opentung.settings;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

public class DataFolderWatcher
{
	private final boolean watcherActive;
	private final Map<String, Callback> callbacks = new HashMap<>();
	
	public DataFolderWatcher(Path dataFolder)
	{
		boolean javaIsBadBoi = false;
		try
		{
			//Create a canonical copy, to set the parents etc.
			WatchService watcher = FileSystems.getDefault().newWatchService();
			dataFolder.register(watcher, ENTRY_MODIFY);
			
			Thread watchThread = new Thread(() -> {
				while(true)
				{
					WatchKey key;
					try
					{
						key = watcher.take();
					}
					catch(InterruptedException e)
					{
						throw new RuntimeException(e);
					}
					
					for(WatchEvent<?> event : key.pollEvents())
					{
						WatchEvent.Kind<?> kind = event.kind();
						if(kind == OVERFLOW)
						{
							continue;
						}
						
						WatchEvent<Path> ev = (WatchEvent<Path>) event;
						Path filename = ev.context();
						Callback callback = callbacks.get(filename.toString());
						if(callback != null)
						{
							callback.update();
						}
					}
					
					boolean valid = key.reset();
					if(!valid)
					{
						break;
					}
				}
				System.out.println("DataFolderWatcher thread has turned off.");
			}, "FileWatchThread:" + dataFolder.getFileName());
			watchThread.setDaemon(true);
			watchThread.start();
			javaIsBadBoi = true;
		}
		catch(IOException x)
		{
			System.out.println("File watcher could not be started, file changes go unnoticed.");
			x.printStackTrace(System.out);
		}
		watcherActive = javaIsBadBoi;
	}
	
	public boolean register(String filename, Callback callback)
	{
		callbacks.put(filename, callback);
		return watcherActive;
	}
	
	public interface Callback
	{
		void update();
	}
}

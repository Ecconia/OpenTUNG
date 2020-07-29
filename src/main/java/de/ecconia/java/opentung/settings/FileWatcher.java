package de.ecconia.java.opentung.settings;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

public class FileWatcher
{
	public FileWatcher(File file, Callback callback)
	{
		Thread watchThread = new Thread(() -> {
			try
			{
				File copy = file.getCanonicalFile();
				
				Path dir = copy.getParentFile().toPath();
				
				WatchService watcher = FileSystems.getDefault().newWatchService();
				WatchKey key = dir.register(watcher, ENTRY_MODIFY);
				
				while(true)
				{
					WatchKey key2;
					try
					{
						key2 = watcher.take();
					}
					catch(InterruptedException e)
					{
						e.printStackTrace();
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
						
						if(filename.toString().equals(file.getName()))
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
			}
			catch(IOException x)
			{
				System.out.println("Settings watching is not available.");
				x.printStackTrace();
			}
			System.out.println("File watcher shutted off.");
		}, "FileWatchThread:" + file.getName());
		watchThread.setDaemon(true);
		watchThread.start();
	}
	
	public interface Callback
	{
		void update();
	}
}

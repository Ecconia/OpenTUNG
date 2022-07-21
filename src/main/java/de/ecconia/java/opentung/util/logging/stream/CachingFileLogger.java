package de.ecconia.java.opentung.util.logging.stream;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.JOptionPane;

public class CachingFileLogger extends PrintStream
{
	private final RuntimeException unsupportedException = new RuntimeException("Called non-newline method on the system output stream. Cannot add prefix to non-newline calls.");
	
	private final LinkedList<String> cache = new LinkedList<>();
	private final PrintStream errorStream;
	
	private boolean healthy = true;
	
	private FileWriter fw;
	
	public CachingFileLogger(PrintStream errorStream)
	{
		//Supply anything, it is not used anyway.
		super(new DeadEndStream());
		this.errorStream = errorStream;
	}
	
	private void store(String content)
	{
		synchronized(this)
		{
			if(fw != null)
			{
				try
				{
					fw.write(content);
					fw.write('\n');
					fw.flush(); //In case of a crash this data should be there. And the JVM itself does not flush it at all.
				}
				catch(IOException e)
				{
					//Uff. Not good.
					errorStream.println("File-logging crashed! Cannot log to file anymore.");
					e.printStackTrace(errorStream);
					
					//Notify user once:
					if(healthy)
					{
						healthy = false;
						JOptionPane.showMessageDialog(null, "The file-logger had an exception. Stacktrace is not logged in file! It can be found in console though.");
					}
				}
			}
			else
			{
				cache.add(content);
			}
		}
	}
	
	public void arm(FileWriter fw, Path root, Path dest) throws IOException
	{
		synchronized(this)
		{
			for(String message : cache)
			{
				fw.write(message);
				fw.write('\n');
			}
			cache.clear();
			this.fw = fw;
		}
		
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			CachingFileLogger.this.flush();
			CachingFileLogger.this.close();
			
			try
			{
				//Zip the logfile:
				ZipOutputStream out = new ZipOutputStream(new FileOutputStream(dest.toFile()));
				ZipEntry e = new ZipEntry(root.getFileName().toString());
				out.putNextEntry(e);
				
				byte[] data = Files.readAllBytes(root);
				out.write(data, 0, data.length);
				out.closeEntry();
				
				out.close();
				
				//Final step delete .log file:
				Files.delete(root);
			}
			catch(Exception e)
			{
				//Uff. Not good.
				errorStream.println("Exception while zipping the log file:");
				e.printStackTrace(errorStream);
			}
		}));
	}
	
	//Output Stream:
	
	@Override
	public void write(int b)
	{
		throw unsupportedException;
	}
	
	@Override
	public void write(byte[] b)
	{
		throw unsupportedException;
	}
	
	@Override
	public void write(byte[] buf, int off, int len)
	{
		throw unsupportedException;
	}
	
	@Override
	public void flush()
	{
		try
		{
			fw.flush();
		}
		catch(IOException e)
		{
			//Probably not ever read or parsed by anything...
			errorStream.println("Could not flush logfile writer:");
			e.printStackTrace(errorStream);
		}
	}
	
	@Override
	public void close()
	{
		try
		{
			fw.close();
		}
		catch(IOException e)
		{
			//Probably not ever read or parsed by anything...
			errorStream.println("Could not close logfile writer:");
			e.printStackTrace(errorStream);
		}
	}
	
	//Print Stream:
	
	//Not line-terminated:
	
	@Override
	public void print(boolean v)
	{
		throw unsupportedException;
	}
	
	@Override
	public void print(char v)
	{
		throw unsupportedException;
	}
	
	@Override
	public void print(int v)
	{
		throw unsupportedException;
	}
	
	@Override
	public void print(long v)
	{
		throw unsupportedException;
	}
	
	@Override
	public void print(float v)
	{
		throw unsupportedException;
	}
	
	@Override
	public void print(double v)
	{
		throw unsupportedException;
	}
	
	@Override
	public void print(char[] v)
	{
		throw unsupportedException;
	}
	
	@Override
	public void print(String v)
	{
		throw unsupportedException;
	}
	
	@Override
	public void print(Object v)
	{
		throw unsupportedException;
	}
	
	//Line-terminated:
	
	@Override
	public void println()
	{
		store("");
	}
	
	@Override
	public void println(boolean v)
	{
		store(String.valueOf(v));
	}
	
	@Override
	public void println(char v)
	{
		store(String.valueOf(v));
	}
	
	@Override
	public void println(int v)
	{
		store(String.valueOf(v));
	}
	
	@Override
	public void println(long v)
	{
		store(String.valueOf(v));
	}
	
	@Override
	public void println(float v)
	{
		store(String.valueOf(v));
	}
	
	@Override
	public void println(double v)
	{
		store(String.valueOf(v));
	}
	
	@Override
	public void println(char[] v)
	{
		store(String.valueOf(v));
	}
	
	@Override
	public void println(String v)
	{
		store(String.valueOf(v));
	}
	
	@Override
	public void println(Object v)
	{
		store(String.valueOf(v));
	}
	
	//Appendable extension:
	
	@Override
	public PrintStream printf(String format, Object... args)
	{
		throw unsupportedException;
	}
	
	@Override
	public PrintStream printf(Locale l, String format, Object... args)
	{
		throw unsupportedException;
	}
	
	@Override
	public PrintStream format(String format, Object... args)
	{
		throw unsupportedException;
	}
	
	@Override
	public PrintStream format(Locale l, String format, Object... args)
	{
		throw unsupportedException;
	}
	
	@Override
	public PrintStream append(CharSequence v)
	{
		throw unsupportedException;
	}
	
	@Override
	public PrintStream append(CharSequence v, int start, int end)
	{
		throw unsupportedException;
	}
	
	@Override
	public PrintStream append(char c)
	{
		throw unsupportedException;
	}
}

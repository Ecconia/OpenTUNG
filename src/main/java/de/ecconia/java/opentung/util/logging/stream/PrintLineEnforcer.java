package de.ecconia.java.opentung.util.logging.stream;

import java.io.PrintStream;
import java.util.Locale;

public class PrintLineEnforcer extends PrintStream
{
	private final RuntimeException invalidCallException = new RuntimeException(
			"Outch, you or one of your mods attempted to print something to the system output stream using a method that does not add a newline by default.\n" +
					" Please construct the lines yourself and use one of the print-line functions.\n" +
					" This is a multi-threaded project, thus you should only push full lines to the output stream, to not produce spaghetti in the log files and console output.");
	
	private final PrintStream target;
	
	public PrintLineEnforcer(PrintStream target)
	{
		super(new DeadEndStream());
		this.target = target;
	}
	
	//Output Stream:
	
	@Override
	public void write(int b)
	{
		throw invalidCallException;
	}
	
	@Override
	public void write(byte[] b)
	{
		throw invalidCallException;
	}
	
	@Override
	public void write(byte[] buf, int off, int len)
	{
		throw invalidCallException;
	}
	
	@Override
	public void flush()
	{
		target.flush();
	}
	
	@Override
	public void close()
	{
		target.close();
	}
	
	//Print Stream:
	
	//Not line-terminated:
	
	@Override
	public void print(boolean v)
	{
		throw invalidCallException;
	}
	
	@Override
	public void print(char v)
	{
		throw invalidCallException;
	}
	
	@Override
	public void print(int v)
	{
		throw invalidCallException;
	}
	
	@Override
	public void print(long v)
	{
		throw invalidCallException;
	}
	
	@Override
	public void print(float v)
	{
		throw invalidCallException;
	}
	
	@Override
	public void print(double v)
	{
		throw invalidCallException;
	}
	
	@Override
	public void print(char[] v)
	{
		throw invalidCallException;
	}
	
	@Override
	public void print(String v)
	{
		throw invalidCallException;
	}
	
	@Override
	public void print(Object v)
	{
		throw invalidCallException;
	}
	
	//Line-terminated:
	
	@Override
	public void println()
	{
		target.println();
	}
	
	@Override
	public void println(boolean v)
	{
		target.println(v);
	}
	
	@Override
	public void println(char v)
	{
		target.println(v);
	}
	
	@Override
	public void println(int v)
	{
		target.println(v);
	}
	
	@Override
	public void println(long v)
	{
		target.println(v);
	}
	
	@Override
	public void println(float v)
	{
		target.println(v);
	}
	
	@Override
	public void println(double v)
	{
		target.println(v);
	}
	
	@Override
	public void println(char[] v)
	{
		target.println(v);
	}
	
	@Override
	public void println(String v)
	{
		target.println(v);
	}
	
	@Override
	public void println(Object v)
	{
		target.println(v);
	}
	
	//Appendable extension:
	
	@Override
	public PrintStream printf(String format, Object... args)
	{
		throw invalidCallException;
	}
	
	@Override
	public PrintStream printf(Locale l, String format, Object... args)
	{
		throw invalidCallException;
	}
	
	@Override
	public PrintStream format(String format, Object... args)
	{
		throw invalidCallException;
	}
	
	@Override
	public PrintStream format(Locale l, String format, Object... args)
	{
		throw invalidCallException;
	}
	
	@Override
	public PrintStream append(CharSequence v)
	{
		throw invalidCallException;
	}
	
	@Override
	public PrintStream append(CharSequence v, int start, int end)
	{
		throw invalidCallException;
	}
	
	@Override
	public PrintStream append(char c)
	{
		throw invalidCallException;
	}
}

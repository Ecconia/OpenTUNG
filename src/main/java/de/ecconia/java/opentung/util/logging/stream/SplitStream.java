package de.ecconia.java.opentung.util.logging.stream;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Locale;

public class SplitStream extends PrintStream
{
	private final PrintStream target;
	private final PrintStream copy;
	
	public SplitStream(PrintStream target, PrintStream copy)
	{
		super(new DeadEndStream());
		this.target = target;
		this.copy = copy;
	}
	
	//Output Stream:
	
	@Override
	public void write(int b)
	{
		target.write(b);
		copy.write(b);
	}
	
	@Override
	public void write(byte[] b) throws IOException
	{
		target.write(b);
		copy.write(b);
	}
	
	@Override
	public void write(byte[] buf, int off, int len)
	{
		target.write(buf, off, len);
		copy.write(buf, off, len);
	}
	
	@Override
	public void flush()
	{
		target.flush();
		copy.flush();
	}
	
	@Override
	public void close()
	{
		target.close();
		copy.close();
	}
	
	//Print Stream:
	
	//Not line-terminated:
	
	@Override
	public void print(boolean v)
	{
		target.print(v);
		copy.print(v);
	}
	
	@Override
	public void print(char v)
	{
		target.print(v);
		copy.print(v);
	}
	
	@Override
	public void print(int v)
	{
		target.print(v);
		copy.print(v);
	}
	
	@Override
	public void print(long v)
	{
		target.print(v);
		copy.print(v);
	}
	
	@Override
	public void print(float v)
	{
		target.print(v);
		copy.print(v);
	}
	
	@Override
	public void print(double v)
	{
		target.print(v);
		copy.print(v);
	}
	
	@Override
	public void print(char[] v)
	{
		target.print(v);
		copy.print(v);
	}
	
	@Override
	public void print(String v)
	{
		target.print(v);
		copy.print(v);
	}
	
	@Override
	public void print(Object v)
	{
		target.print(v);
		copy.print(v);
	}
	
	//Line-terminated:
	
	@Override
	public void println()
	{
		target.println();
		copy.println();
	}
	
	@Override
	public void println(boolean v)
	{
		target.println(v);
		copy.println(v);
	}
	
	@Override
	public void println(char v)
	{
		target.println(v);
		copy.println(v);
	}
	
	@Override
	public void println(int v)
	{
		target.println(v);
		copy.println(v);
	}
	
	@Override
	public void println(long v)
	{
		target.println(v);
		copy.println(v);
	}
	
	@Override
	public void println(float v)
	{
		target.println(v);
		copy.println(v);
	}
	
	@Override
	public void println(double v)
	{
		target.println(v);
		copy.println(v);
	}
	
	@Override
	public void println(char[] v)
	{
		target.println(v);
		copy.println(v);
	}
	
	@Override
	public void println(String v)
	{
		target.println(v);
		copy.println(v);
	}
	
	@Override
	public void println(Object v)
	{
		target.println(v);
		copy.println(v);
	}
	
	//Appendable extension:
	
	@Override
	public PrintStream printf(String format, Object... args)
	{
		target.printf(format, args);
		copy.printf(format, args);
		return this;
	}
	
	@Override
	public PrintStream printf(Locale l, String format, Object... args)
	{
		target.printf(l, format, args);
		copy.printf(l, format, args);
		return this;
	}
	
	@Override
	public PrintStream format(String format, Object... args)
	{
		target.format(format, args);
		copy.format(format, args);
		return this;
	}
	
	@Override
	public PrintStream format(Locale l, String format, Object... args)
	{
		target.format(l, format, args);
		copy.format(l, format, args);
		return this;
	}
	
	@Override
	public PrintStream append(CharSequence v)
	{
		target.append(v);
		copy.append(v);
		return this;
	}
	
	@Override
	public PrintStream append(CharSequence v, int start, int end)
	{
		target.append(v, start, end);
		copy.append(v, start, end);
		return this;
	}
	
	@Override
	public PrintStream append(char c)
	{
		target.print(c);
		copy.print(c);
		return this;
	}
}

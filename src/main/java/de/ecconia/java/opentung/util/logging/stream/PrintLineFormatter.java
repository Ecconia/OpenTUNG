package de.ecconia.java.opentung.util.logging.stream;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;

public class PrintLineFormatter extends PrintStream
{
	private final RuntimeException unsupportedException = new RuntimeException("Called non-newline method on the system output stream. Cannot add prefix to non-newline calls.");
	private final DateTimeFormatter dateConverter = DateTimeFormatter.ofPattern("uuuu.MM.dd/HH:mm:ssX");
	
	private final PrintStream target;
	
	public PrintLineFormatter(PrintStream target)
	{
		super(new DeadEndStream());
		this.target = target;
	}
	
	private void format(String content)
	{
		ZonedDateTime zonedDateTimeCurrent = ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault());
		String prefix = "[" + dateConverter.format(zonedDateTimeCurrent) + "] [" + Thread.currentThread().getName() + "] ";
		String padding = "";
		int startIndex = 0;
		char[] chars = content.toCharArray();
		boolean encounteredNewline = false;
		for(int i = 0; i < content.length(); i++)
		{
			if(chars[i] == '\n')
			{
				if(encounteredNewline)
				{
					target.println(padding + content.substring(startIndex, i));
				}
				else
				{
					int paddingLength = prefix.length();
					char[] padArray = new char[paddingLength];
					Arrays.fill(padArray, ' ');
					padding = new String(padArray);
					target.println(prefix + content.substring(startIndex, i));
				}
				encounteredNewline = true;
				startIndex = i + 1;
			}
		}
		if(encounteredNewline)
		{
			target.println(padding + content.substring(startIndex));
		}
		else
		{
			target.println(prefix + content.substring(startIndex));
		}
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
		format("");
	}
	
	@Override
	public void println(boolean v)
	{
		format(String.valueOf(v));
	}
	
	@Override
	public void println(char v)
	{
		format(String.valueOf(v));
	}
	
	@Override
	public void println(int v)
	{
		format(String.valueOf(v));
	}
	
	@Override
	public void println(long v)
	{
		format(String.valueOf(v));
	}
	
	@Override
	public void println(float v)
	{
		format(String.valueOf(v));
	}
	
	@Override
	public void println(double v)
	{
		format(String.valueOf(v));
	}
	
	@Override
	public void println(char[] v)
	{
		format(String.valueOf(v));
	}
	
	@Override
	public void println(String v)
	{
		format(String.valueOf(v));
	}
	
	@Override
	public void println(Object v)
	{
		format(String.valueOf(v));
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

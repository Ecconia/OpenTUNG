package de.ecconia.java.opentung.util.logging.stream;

import java.io.OutputStream;

public class DeadEndStream extends OutputStream
{
	@Override
	public void write(int b)
	{
		//Do nothing.
	}
}

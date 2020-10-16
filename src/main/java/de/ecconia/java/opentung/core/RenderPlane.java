package de.ecconia.java.opentung.core;

public interface RenderPlane
{
	void setup();
	
	void render();
	
	void newSize(int width, int height);
}

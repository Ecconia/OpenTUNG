package de.ecconia.java.opentung.core.structs;

public interface RenderPlane
{
	void setup();
	
	void render();
	
	void newSize(int width, int height);
}

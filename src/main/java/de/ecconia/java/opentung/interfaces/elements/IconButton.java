package de.ecconia.java.opentung.interfaces.elements;

import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.TextureWrapper;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;
import de.ecconia.java.opentung.settings.Settings;

public class IconButton extends AbstractButton
{
	private final TextureWrapper texture;
	
	public IconButton(TextureWrapper texture, float relX, float relY, float width, float height)
	{
		super(relX, relY, width, height);
		
		this.texture = texture;
	}
	
	public void renderIcon(ShaderProgram iconShader, GenericVAO iconPlane, float x, float y)
	{
		float scale = Settings.guiScale;
		texture.activate();
		x += relX;
		y += relY;
		//Warning, doesn't set scale yet. Maybe make scale an object which defaults to null.
		iconShader.setUniformV2(2, new float[]{x * scale, y * scale});
		iconPlane.draw();
	}
}

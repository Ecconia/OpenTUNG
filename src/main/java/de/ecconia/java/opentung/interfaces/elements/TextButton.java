package de.ecconia.java.opentung.interfaces.elements;

import de.ecconia.java.opentung.interfaces.MeshText;
import de.ecconia.java.opentung.libwrap.FloatShortArraysInt;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;
import de.ecconia.java.opentung.settings.Settings;
import org.lwjgl.opengl.GL30;

public class TextButton extends AbstractButton
{
	private final int textWidth;
	private final GenericVAO textMesh;
	
	public TextButton(MeshText fontUnit, String text, float relX, float relY, int width, int height)
	{
		super(relX, relY, width, height);
		
		FloatShortArraysInt r = fontUnit.fillArray(text, 50);
		textMesh = new GenericVAO(r.getFloats(), r.getShorts())
		{
			@Override
			protected void init()
			{
				//Position:
				GL30.glVertexAttribPointer(0, 2, GL30.GL_FLOAT, false, 4 * Float.BYTES, 0);
				GL30.glEnableVertexAttribArray(0);
				//TextureCoord:
				GL30.glVertexAttribPointer(1, 2, GL30.GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
				GL30.glEnableVertexAttribArray(1);
			}
		};
		textWidth = r.getInteger();
	}
	
	public void renderText(ShaderProgram labelShader, float x, float y)
	{
		float scale = Settings.guiScale;
		labelShader.setUniformV3(1, new float[]{
				(x + relX - textWidth / 2f) * scale,
				(y + relY) * scale,
				scale
		});
		textMesh.use();
		textMesh.draw();
	}
}

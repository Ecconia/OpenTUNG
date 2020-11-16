package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.Direction;
import de.ecconia.java.opentung.components.fragments.TexturedFace;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.CustomData;
import de.ecconia.java.opentung.components.meta.ModelBuilder;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.components.meta.PlaceableInfo;
import de.ecconia.java.opentung.libwrap.TextureWrapper;
import de.ecconia.java.opentung.util.io.ByteLevelHelper;
import de.ecconia.java.opentung.util.io.ByteReader;
import de.ecconia.java.opentung.util.math.Vector3;
import java.nio.charset.StandardCharsets;

public class CompLabel extends Component implements CustomData
{
	public static final ModelHolder modelHolder = new ModelBuilder()
			.setPlacementOffset(new Vector3(0.0, 0.15 + 0.075, 0.0))
			.addSolid(new CubeFull(new Vector3(0.0, 0.0, 0.0), new Vector3(0.3, 0.3, 0.3), Color.material))
			.addTexture(new TexturedFace(new Vector3(0.0, 0.0, 0.0), new Vector3(0.3, 0.3, 0.3), Direction.YPos))
			.build();
	public static final PlaceableInfo info = new PlaceableInfo(modelHolder, "TUNG-Label", "0.2.6", CompLabel.class, CompLabel::new);
	
	public static void initGL()
	{
		modelHolder.generateTextureVAO();
	}
	
	@Override
	public ModelHolder getModelHolder()
	{
		return modelHolder;
	}
	
	@Override
	public PlaceableInfo getInfo()
	{
		return info;
	}
	
	//### Non-Static ###
	
	private String text;
	private float fontSize;
	
	private TextureWrapper texture;
	
	public CompLabel(CompContainer parent)
	{
		super(parent);
	}
	
	public void setText(String text)
	{
		this.text = text;
	}
	
	public void setFontSize(float fontSize)
	{
		this.fontSize = fontSize;
	}
	
	public String getText()
	{
		return text;
	}
	
	public float getFontSize()
	{
		return fontSize;
	}
	
	public void activate()
	{
		texture.activate();
	}
	
	public void setTexture(TextureWrapper texture)
	{
		this.texture = texture;
	}
	
	public void updateTexture(TextureWrapper texture)
	{
		if(this.texture == null)
		{
			//Has already been removed by user!
			//TODO: Do more properly. It might have not been uploaded at this point.
			texture.unload();
			return;
		}
		
		this.texture = texture;
	}
	
	public void unload()
	{
		if(texture == null)
		{
			System.out.println("[Debug] Texture of label is null, might be cause its placed without text.");
			return;
		}
		texture.unload();
		this.texture = null; //Reset texture.
	}
	
	public boolean hasTexture()
	{
		return text != null;
	}
	
	//### Save/Load ###
	
	@Override
	public byte[] getCustomData()
	{
		byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
		int textSize = textBytes.length;
		int textOffset = ByteLevelHelper.sizeOfUnsignedInt(textSize) + 4;
		byte[] bytes = new byte[textOffset + textSize];
		ByteLevelHelper.writeFloat(fontSize, bytes, 0);
		ByteLevelHelper.writeUnsignedInt(textSize, bytes, 4);
		System.arraycopy(textBytes, 0, bytes, textOffset, textSize);
		return bytes;
	}
	
	@Override
	public void setCustomData(byte[] data)
	{
		ByteReader reader = new ByteReader(data);
		fontSize = reader.readFloatLE();
		text = new String(reader.readBytes(reader.readVariableInt()), StandardCharsets.UTF_8);
	}
}

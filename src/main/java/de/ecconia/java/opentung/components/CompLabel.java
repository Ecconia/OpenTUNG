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
import de.ecconia.java.opentung.components.meta.Part;
import de.ecconia.java.opentung.components.meta.PlaceableInfo;
import de.ecconia.java.opentung.components.meta.PlacementSettingBoardSide;
import de.ecconia.java.opentung.components.meta.PlacementSettingBoardSquare;
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
			.setMountPlaceable(true)
			.setBoardSidePlacementOption(PlacementSettingBoardSide.None)
			.setBoardPlacementOption(PlacementSettingBoardSquare.Middle)
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
	
	//Initialize these values, needed when a new component is created.
	private String text = ""; //TODO: Default text is still wrong/missing.
	private float fontSize = 3.0f; //TODO: Default is unknown right now.
	
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
	
	//### Texture management: ###
	//All methods in this section are render thread exclusive!
	
	public void activate()
	{
		if(texture != null) //If this gets called, something went horribly wrong.
		{
			texture.activate();
		}
	}
	
	public void setTexture(Component rootBoard, TextureWrapper texture)
	{
		//Unload any previous texture, cause it will be overwritten now:
		unload();
		//Check if this component got deleted, if so, do not apply this texture:
		Part rootParent = getRootParent();
		if(rootParent != rootBoard)
		{
			//Label is deleted: Do nothing more.
			texture.unload(); //Texture is no longer used here, decrement its internal counter (if enabled).
			return;
		}
		
		this.texture = texture;
	}
	
	public void unload()
	{
		if(texture == null)
		{
			return;
		}
		texture.unload();
		texture = null; //Reset texture.
	}
	
	public boolean hasTexture()
	{
		return !text.isEmpty();
	}
	
	public boolean hasText()
	{
		return !text.isEmpty();
	}
	
	//### Save/Load ###
	
	@Override
	public byte[] getCustomData()
	{
		String textCopy = text;
		if(textCopy == null)
		{
			//TODO: Once Label editing is implemented, this will not be required anymore.
			//Cannot initialize the variable with it, since then there still is not texture generated for it.
			textCopy = "";
		}
		byte[] textBytes = textCopy.getBytes(StandardCharsets.UTF_8);
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
	
	@Override
	public Component copy()
	{
		CompLabel copy = (CompLabel) super.copy();
		copy.setText(text);
		copy.setFontSize(fontSize);
		TextureWrapper texture = this.texture; //Make a copy, to prevent NPE by thread fighting.
		if(texture != null) //Labels without any text, have no texture (to render).
		{
			copy.texture = texture.copy();
		}
		return copy;
	}
}

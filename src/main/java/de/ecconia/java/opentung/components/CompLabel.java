package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.libwrap.TextureWrapper;
import de.ecconia.java.opentung.models.GenericModel;
import de.ecconia.java.opentung.models.LabelModel;
import de.ecconia.java.opentung.models.LabelModelTex;

public class CompLabel extends CompGeneric
{
	public static LabelModel model;
	public static LabelModelTex modelTex;
	
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
	
	public void initialize()
	{
		texture = modelTex.generateUploadTexture(text, fontSize);
	}
	
	public void activate()
	{
		texture.activate();
	}
	
	@Override
	public GenericModel getModel()
	{
		return model;
	}
	
	public void drawLabel()
	{
		modelTex.draw();
	}
}

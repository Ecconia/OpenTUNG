package de.ecconia.java.opentung.components;

import de.ecconia.java.opentung.libwrap.TextureWrapper;
import de.ecconia.java.opentung.models.GenericModel;
import de.ecconia.java.opentung.models.LabelModelTex;
import de.ecconia.java.opentung.models.PanelLabelModel;
import de.ecconia.java.opentung.models.PanelLabelModelTex;

public class CompPanelLabel extends CompLabel
{
	public static PanelLabelModel model;
	public static PanelLabelModelTex modelTex;
	
	private String text;
	private float fontSize;
	
	private TextureWrapper texture;
	
	public CompPanelLabel(CompContainer parent)
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
		texture = LabelModelTex.generateUploadTexture(text, fontSize);
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

package de.ecconia.java.opentung.components;

import java.util.ArrayList;
import java.util.List;

public abstract class CompContainer extends CompGeneric
{
	//Raw data:
	private final List<CompGeneric> children = new ArrayList<>();
	
	public CompContainer(CompContainer parent)
	{
		super(parent);
	}
	
	public List<CompGeneric> getChildren()
	{
		return children;
	}
	
	public void addChild(CompGeneric child)
	{
		children.add(child);
	}
}

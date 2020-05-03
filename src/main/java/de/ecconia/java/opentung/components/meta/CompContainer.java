package de.ecconia.java.opentung.components.meta;

import java.util.ArrayList;
import java.util.List;

public abstract class CompContainer extends Component
{
	//Raw data:
	private final List<Component> children = new ArrayList<>();
	
	public CompContainer(CompContainer parent)
	{
		super(parent);
	}
	
	public List<Component> getChildren()
	{
		return children;
	}
	
	public void addChild(Component child)
	{
		children.add(child);
	}
}

package de.ecconia.java.opentung.components.meta;

public class PlaceboParent extends Component
{
	private static final ModelHolder modelHolder = new ModelBuilder().build();
	
	public PlaceboParent()
	{
		super(null);
	}
	
	@Override
	public ModelHolder getModelHolder()
	{
		return modelHolder;
	}
}

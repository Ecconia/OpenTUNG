package de.ecconia.java.opentung.meshing;

import java.util.ArrayList;
import java.util.List;

import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;

public abstract class MeshBag
{
	protected final MeshBagContainer meshBagContainer;
	protected final List<Component> components;
	
	protected GenericVAO vao;
	
	protected int verticesAmount;
	
	public MeshBag(MeshBagContainer meshBagContainer)
	{
		this.meshBagContainer = meshBagContainer;
		
		components = new ArrayList<>();
	}
	
	public void addComponent(Component component, int verticesAmount)
	{
		components.add(component);
		this.verticesAmount += verticesAmount;
		meshBagContainer.setDirty(this);
	}
	
	public void removeComponent(Component component, int verticesAmount)
	{
		components.remove(component);
		this.verticesAmount -= verticesAmount;
		meshBagContainer.setDirty(this);
	}
	
	public int getVerticesAmount()
	{
		return verticesAmount;
	}
	
	public abstract void rebuild();
	
	public void draw()
	{
		vao.use();
		vao.draw();
	}
}

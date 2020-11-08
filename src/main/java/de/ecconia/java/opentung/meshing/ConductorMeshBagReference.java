package de.ecconia.java.opentung.meshing;

public class ConductorMeshBagReference
{
	private final ConductorMeshBag conductorMeshBag;
	private final int id;
	
	public ConductorMeshBagReference(ConductorMeshBag conductorMeshBag, int id)
	{
		this.conductorMeshBag = conductorMeshBag;
		this.id = id;
	}
	
	public void setActive(boolean active)
	{
		conductorMeshBag.setActive(id, active);
	}
}

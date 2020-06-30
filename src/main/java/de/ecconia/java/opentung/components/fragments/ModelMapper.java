package de.ecconia.java.opentung.components.fragments;

import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.math.Vector3;

public interface ModelMapper
{
	default Vector3 getMappedSize(Vector3 size, Component component)
	{
		return size; //No modification by default.
	}
}

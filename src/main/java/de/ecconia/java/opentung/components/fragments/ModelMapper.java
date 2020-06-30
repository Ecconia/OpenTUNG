package de.ecconia.java.opentung.components.fragments;

import de.ecconia.java.opentung.components.meta.Part;
import de.ecconia.java.opentung.math.Vector3;

public interface ModelMapper
{
	default Vector3 getMappedSize(Vector3 size, Part component)
	{
		return size; //No modification by default.
	}
}

package de.ecconia.java.opentung;

import de.ecconia.java.opentung.components.CompBlotter;
import de.ecconia.java.opentung.components.CompButton;
import de.ecconia.java.opentung.components.CompColorDisplay;
import de.ecconia.java.opentung.components.CompDelayer;
import de.ecconia.java.opentung.components.CompDisplay;
import de.ecconia.java.opentung.components.CompInverter;
import de.ecconia.java.opentung.components.CompLabel;
import de.ecconia.java.opentung.components.CompMount;
import de.ecconia.java.opentung.components.CompNoisemaker;
import de.ecconia.java.opentung.components.CompPanelButton;
import de.ecconia.java.opentung.components.CompPanelColorDisplay;
import de.ecconia.java.opentung.components.CompPanelDisplay;
import de.ecconia.java.opentung.components.CompPanelLabel;
import de.ecconia.java.opentung.components.CompPanelSwitch;
import de.ecconia.java.opentung.components.CompPeg;
import de.ecconia.java.opentung.components.CompSnappingPeg;
import de.ecconia.java.opentung.components.CompSwitch;
import de.ecconia.java.opentung.components.CompThroughBlotter;
import de.ecconia.java.opentung.components.CompThroughPeg;
import de.ecconia.java.opentung.components.meta.ModelHolder;

public class UglyComponentAwareness
{
	public static final int MAX = 19;
	
	public static ModelHolder getModelByIndex(int id)
	{
		switch(id)
		{
			case 0:
				return null;
			case 1:
				return CompPeg.modelHolder;
			case 2:
				return CompInverter.modelHolder;
			case 3:
				return CompBlotter.modelHolder;
			case 4:
				return CompDelayer.modelHolder;
			case 5:
				return CompSwitch.modelHolder;
			case 6:
				return CompButton.modelHolder;
			case 7:
				return CompPanelSwitch.modelHolder;
			case 8:
				return CompPanelButton.modelHolder;
			case 9:
				return CompNoisemaker.modelHolder;
			case 10:
				return CompMount.modelHolder;
			case 11:
				return CompSnappingPeg.modelHolder;
			case 12:
				return CompThroughBlotter.modelHolder;
			case 13:
				return CompThroughPeg.modelHolder;
			case 14:
				return CompDisplay.modelHolder;
			case 15:
				return CompPanelDisplay.modelHolder;
			case 16:
				return CompColorDisplay.modelHolder;
			case 17:
				return CompPanelColorDisplay.modelHolder;
			case 18:
				return CompLabel.modelHolder;
			case 19:
				return CompPanelLabel.modelHolder;
		}
		
		return null;
	}
}

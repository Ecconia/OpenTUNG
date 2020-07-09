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
import de.ecconia.java.opentung.components.meta.Part;

public class UglyComponentAwareness
{
	private static PlaceableInfo[] placeableInfos = new PlaceableInfo[]{
			null,
			CompPeg.info,
			CompInverter.info,
			CompBlotter.info,
			CompDelayer.info,
			CompSwitch.info,
			CompButton.info,
			CompPanelSwitch.info,
			CompPanelButton.info,
			CompNoisemaker.info,
			CompMount.info,
			CompSnappingPeg.info,
			CompThroughBlotter.info,
			CompThroughPeg.info,
			CompDisplay.info,
			CompPanelDisplay.info,
			CompColorDisplay.info,
			CompPanelColorDisplay.info,
			CompLabel.info,
			CompPanelLabel.info,
	};
	
	public static final int MAX = placeableInfos.length - 1;
	
	public static PlaceableInfo getModelByIndex(int id)
	{
		if(id > MAX)
		{
			return null;
		}
		else
		{
			return placeableInfos[id];
		}
	}
	
	static
	{
		//More ugly code:
		for(int i = 1; i <= MAX; i++)
		{
			placeableInfos[i].setIndex(i);
		}
	}
	
	public static PlaceableInfo getPlaceableInfoFrom(Part part)
	{
		return part.getInfo();
	}
}

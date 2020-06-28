package de.ecconia.java.opentung.components.meta;

import de.ecconia.java.opentung.components.CompBlotter;
import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.CompButton;
import de.ecconia.java.opentung.components.CompColorDisplay;
import de.ecconia.java.opentung.components.CompCrossyIndicator;
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
import de.ecconia.java.opentung.components.conductor.CompWireRaw;
import java.lang.reflect.InvocationTargetException;

public class ComponentLibrary
{
	private static final Class<? extends Component>[] componentClasses = new Class[]{
			//Official:
			CompPeg.class, //Wire
			CompInverter.class, //Solid + Wire
			CompBlotter.class, //Solid + Wire
			CompDelayer.class, //Solid + Wire
			CompSwitch.class, //Solid + Wire + Toggle
			CompButton.class, //Solid + Wire + Toggle
			CompPanelSwitch.class, //Solid + Wire + Toggle
			CompPanelButton.class, //Solid + Wire + Toggle
			CompNoisemaker.class, //Wire + Display
			CompMount.class, //Solid (+ Board)
			CompSnappingPeg.class, //Solid
			CompThroughBlotter.class, //Solid + Wire
			CompThroughPeg.class, //Solid + Wire
			CompDisplay.class, //Wire + Display
			CompPanelDisplay.class, //Solid + Wire + Display
			CompColorDisplay.class, //Solid + Wire + Display
			CompPanelColorDisplay.class, //Solid + Wire + Display
			CompLabel.class, //Solid + S:Label M:Texture
			CompPanelLabel.class, //Solid + S:Label M:Texture
			//Extra:
//			CompWire.class, //Wire
			CompWireRaw.class, //Wire
			CompBoard.class, //S:Board M:Texture
			//Custom:
			CompCrossyIndicator.class,
	};
	
	public static void initGL()
	{
		for(Class<? extends Component> clazz : componentClasses)
		{
			try
			{
				clazz.getDeclaredMethod("initGL").invoke(null);
			}
			catch(IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
			{
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
}

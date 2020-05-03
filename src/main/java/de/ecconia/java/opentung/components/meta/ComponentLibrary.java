package de.ecconia.java.opentung.components.meta;

import de.ecconia.java.opentung.components.CompBlotter;
import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.CompCrossyIndicator;
import de.ecconia.java.opentung.components.CompInverter;
import de.ecconia.java.opentung.components.CompLabel;
import de.ecconia.java.opentung.components.CompPanelLabel;
import de.ecconia.java.opentung.components.CompPeg;
import de.ecconia.java.opentung.components.CompSnappingPeg;
import de.ecconia.java.opentung.components.CompThroughPeg;
import de.ecconia.java.opentung.components.CompWireRaw;
import java.lang.reflect.InvocationTargetException;

public class ComponentLibrary
{
	private static final Class<? extends Component>[] componentClasses = new Class[] {
			//Official:
			CompPeg.class,
			CompInverter.class,
			CompBlotter.class,
			//- Delayer
			//- Switch
			//- Button
			//- PanelSwitch
			//- PanelButton
			//- NoiseMaker
			//- Mount
			CompSnappingPeg.class,
			//- ThroughBlotter
			CompThroughPeg.class,
			//- Display
			//- PanelDisplay
			//- ColorDisplay
			//- PanelColorDisplay
			CompLabel.class,
			CompPanelLabel.class,
			//Extra:
//			CompWire.class,
			CompWireRaw.class,
			CompBoard.class,
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

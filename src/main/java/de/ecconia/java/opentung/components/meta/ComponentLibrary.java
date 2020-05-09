package de.ecconia.java.opentung.components.meta;

import de.ecconia.java.opentung.components.CompBlotter;
import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.CompCrossyIndicator;
import de.ecconia.java.opentung.components.CompDisplay;
import de.ecconia.java.opentung.components.CompInverter;
import de.ecconia.java.opentung.components.CompLabel;
import de.ecconia.java.opentung.components.CompPanelLabel;
import de.ecconia.java.opentung.components.CompPeg;
import de.ecconia.java.opentung.components.CompSnappingPeg;
import de.ecconia.java.opentung.components.CompSwitch;
import de.ecconia.java.opentung.components.CompThroughPeg;
import de.ecconia.java.opentung.components.CompWireRaw;
import java.lang.reflect.InvocationTargetException;

public class ComponentLibrary
{
	private static final Class<? extends Component>[] componentClasses = new Class[] {
			//Official:
			CompPeg.class, //Wire
			CompInverter.class, //Solid + Wire
			CompBlotter.class, //Solid + Wire
			//- Delayer //Solid + Wire
			CompSwitch.class, //Solid + Wire + Toggle
			//- Button //Solid + Wire + Toggle
			//- PanelSwitch //Solid + Wire + Toggle
			//- PanelButton //Solid + Wire + Toggle
			//- NoiseMaker //Wire + Display
			//- Mount //Solid (+ Board)
			CompSnappingPeg.class, //Solid
			//- ThroughBlotter //Solid + Wire
			CompThroughPeg.class, //Solid + Wire
			CompDisplay.class, //Wire + Display
			//- PanelDisplay //Solid + Wire + Display
			//- ColorDisplay //Solid + Wire + Display
			//- PanelColorDisplay //Solid + Wire + Display
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

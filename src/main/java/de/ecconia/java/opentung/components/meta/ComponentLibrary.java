package de.ecconia.java.opentung.components.meta;

import de.ecconia.java.opentung.components.CompCrossyIndicator;
import de.ecconia.java.opentung.components.CompLabel;
import de.ecconia.java.opentung.components.CompPanelLabel;
import java.lang.reflect.InvocationTargetException;

public class ComponentLibrary
{
	private static final Class<? extends Component>[] componentClasses = new Class[]{
			CompLabel.class,
			CompPanelLabel.class,
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
				System.err.println("Class " + clazz.getSimpleName() + " could not be loaded:");
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
}

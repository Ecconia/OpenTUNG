package de.ecconia.java.opentung.core.tools.grabbing;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import de.ecconia.java.opentung.components.CompLabel;
import de.ecconia.java.opentung.components.CompSnappingPeg;
import de.ecconia.java.opentung.components.CompSnappingWire;
import de.ecconia.java.opentung.components.conductor.Blot;
import de.ecconia.java.opentung.components.conductor.Peg;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ConnectedComponent;
import de.ecconia.java.opentung.core.BoardUniverse;
import de.ecconia.java.opentung.core.data.Hitpoint;
import de.ecconia.java.opentung.core.data.SharedData;
import de.ecconia.java.opentung.core.tools.Tool;
import de.ecconia.java.opentung.core.tools.grabbing.data.GrabContainerData;
import de.ecconia.java.opentung.interfaces.RenderPlane2D;
import de.ecconia.java.opentung.interfaces.windows.ImportWindow;
import de.ecconia.java.opentung.savefile.BoardAndWires;
import de.ecconia.java.opentung.savefile.Loader;
import de.ecconia.java.opentung.settings.keybinds.Keybindings;
import de.ecconia.java.opentung.simulation.InitClusterHelper;
import de.ecconia.java.opentung.simulation.Powerable;
import de.ecconia.java.opentung.simulation.Wire;

public class ImportTool implements Tool
{
	private final SharedData sharedData;
	private final Grabbing grabbing;
	
	//Windows:
	private final ImportWindow importWindow;
	
	public ImportTool(SharedData sharedData, Grabbing grabbing)
	{
		this.sharedData = sharedData;
		this.grabbing = grabbing;
		
		RenderPlane2D interfaceRenderer = sharedData.getRenderPlane2D();
		importWindow = new ImportWindow(this, interfaceRenderer);
		interfaceRenderer.addWindow(importWindow);
	}
	
	@Override
	public Boolean activateKeyUp(Hitpoint hitpoint, int scancode, boolean control)
	{
		if(scancode == Keybindings.KeyGrabImport)
		{
			return true;
		}
		return null;
	}
	
	public void guiImportClosed(Path path)
	{
		if(path == null)
		{
			System.out.println("No file chosen.");
			abortTool();
			return;
		}
		
		BoardAndWires baw;
		try
		{
			baw = Loader.load(path);
		}
		catch(Exception e)
		{
			if(e.getClass() == RuntimeException.class) //Some lazy developer only sends this class instead of a specific one to catch, so lets only listen to this one.
			{
				System.out.println("Could not parse board file, error-message: " + e.getMessage());
				Throwable cause = e.getCause();
				if(cause != null)
				{
					System.out.println("Exception came along with a stacktrace. Can be used to report to the developer.");
					cause.printStackTrace(System.out);
				}
			}
			else //Its some unexpected exception and should be treated differently.
			{
				e.printStackTrace();
			}
			abortTool();
			return;
		}
		
		GrabContainerData grabData = postProcessBoard(baw);
		
		//We reached this point without being aborted, so deal with the consequences and allow the tool to be swapped.
		
		//No more input for a while.
		sharedData.getRenderPlane3D().switchTool(grabbing);
		
		grabbing.takeImportOver(grabData);
	}
	
	private GrabContainerData postProcessBoard(BoardAndWires baw)
	{
		GrabContainerData grabData = new GrabContainerData(null, baw.getBoard());
		grabData.setCopy();
		
		//Render stuff, as in Component handling:
		
		HashSet<CompSnappingPeg> unconnectedSnappingPegs = new HashSet<>(); //Just collect all snapping pegs
		{
			LinkedList<Component> queue = new LinkedList<>();
			queue.add(baw.getBoard());
			while(!queue.isEmpty())
			{
				Component component = queue.removeFirst();
				grabData.addComponent(component);
				
				if(component instanceof CompLabel)
				{
					if(((CompLabel) component).hasText())
					{
						grabData.addLabel((CompLabel) component);
					}
				}
				else if(component instanceof CompSnappingPeg)
				{
					unconnectedSnappingPegs.add((CompSnappingPeg) component);
				}
				
				if(component instanceof CompContainer)
				{
					for(Component child : ((CompContainer) component).getChildren())
					{
						queue.addLast(child);
					}
				}
			}
		}
		
		//Link snapping wires:
		LinkedList<CompSnappingWire> internalSnappingWires = new LinkedList<>();
		//The list cast in the next line is ugly. But each of the two caller locations has its own list type. So there is no way around casting here.
		BoardUniverse.linkSnappingPegs(grabData.getComponents(), grabData.getComponent(), (List<Component>) (List<?>) internalSnappingWires);
		grabData.setInternalSnappingWires(internalSnappingWires);
		//Remove the snapping pegs from the unconnected list, which by now are connected:
		for(CompSnappingWire wire : internalSnappingWires)
		{
			//Get the parent of each wire-side peg, which is the snapping peg, and remove it.
			unconnectedSnappingPegs.remove(wire.getConnectorA().getParent());
			unconnectedSnappingPegs.remove(wire.getConnectorB().getParent());
		}
		
		//Add wires to their known connectors. (Snapping wires are not included and its done already).
		for(Wire wire : baw.getWires())
		{
			wire.getConnectorA().addWire(wire);
			wire.getConnectorB().addWire(wire);
		}
		
		//Simulation stuff, as in wire handling:
		
		grabData.setInternalWires(Arrays.asList(baw.getWires()));
		grabData.setUnconnectedSnappingPegs(unconnectedSnappingPegs);
		
		for(Component comp : grabData.getComponents())
		{
			if(comp instanceof ConnectedComponent)
			{
				for(Blot blot : ((ConnectedComponent) comp).getBlots())
				{
					InitClusterHelper.createBlottyCluster(blot);
				}
			}
		}
		for(Component comp : grabData.getComponents())
		{
			if(comp instanceof ConnectedComponent)
			{
				for(Peg peg : ((ConnectedComponent) comp).getPegs())
				{
					if(!peg.hasCluster())
					{
						InitClusterHelper.createPeggyCluster(peg);
					}
				}
			}
		}
		
		for(Component component : grabData.getComponents())
		{
			if(component instanceof Powerable)
			{
				((Powerable) component).forceUpdateOutput();
			}
		}
		
		return grabData;
	}
	
	private void abortTool()
	{
		sharedData.getRenderPlane3D().toolStopInputs();
		sharedData.getGpuTasks().add((worldRenderer) -> {
			worldRenderer.toolDisable();
		});
	}
	
	@Override
	public void activateNow(Hitpoint hitpoint)
	{
		sharedData.getGpuTasks().add((worldRenderer) -> {
			importWindow.activate();
			worldRenderer.toolReady();
		});
	}
}

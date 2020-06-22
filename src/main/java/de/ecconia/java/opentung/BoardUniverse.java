package de.ecconia.java.opentung;

import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.CompLabel;
import de.ecconia.java.opentung.components.CompSnappingPeg;
import de.ecconia.java.opentung.components.CompSnappingWire;
import de.ecconia.java.opentung.components.conductor.Blot;
import de.ecconia.java.opentung.components.conductor.CompWireRaw;
import de.ecconia.java.opentung.components.conductor.Connector;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.math.Quaternion;
import de.ecconia.java.opentung.math.Vector3;
import de.ecconia.java.opentung.simulation.Cluster;
import de.ecconia.java.opentung.simulation.SourceCluster;
import java.util.ArrayList;
import java.util.List;

public class BoardUniverse
{
	private final List<CompBoard> boardsToRender = new ArrayList<>();
	private final List<CompWireRaw> wiresToRender = new ArrayList<>();
	private final List<Component> componentsToRender = new ArrayList<>();
	private final List<CompLabel> labelsToRender = new ArrayList<>();
	private final List<CompSnappingWire> snappingWires = new ArrayList<>();
	
	private int nextClusterID = 0;
	
	public BoardUniverse(CompBoard board)
	{
		//Sort Elements into Lists, for the 3D section to use:
		importComponent(board);
		
		//Connect snapping pegs:
		linkSnappingPegs(board);
		
		//Create clusters:
		for(CompWireRaw wire : wiresToRender)
		{
			wire.getConnectorA().addWire(wire);
			wire.getConnectorB().addWire(wire);
		}
		
		//Create blot clusters:
		for(Component comp : componentsToRender)
		{
			for(Blot blot : comp.getBlots())
			{
				createBlottyCluster(blot);
			}
		}
		
		System.out.println("Assigned cluster IDs: " + nextClusterID);
	}
	
	private void createBlottyCluster(Blot blot)
	{
		//Precondition: No blob can have a cluster at this point.
		Cluster cluster = new SourceCluster(nextClusterID++);
		cluster.addConnector(blot);
		blot.setCluster(cluster);
		if(blot.getWires().isEmpty())
		{
			return;
		}
		for(CompWireRaw wire : blot.getWires())
		{
			Connector otherSide = wire.getOtherSide(blot);
			if(otherSide instanceof Blot)
			{
				System.out.println("WARNING: Circuit contains Blot-Blot connection which is not allowed.");
				continue;
			}
			wire.setCluster(cluster);
			
		}
	}
	
	private void linkSnappingPegs(CompBoard board)
	{
		List<CompSnappingPeg> snappingPegs = new ArrayList<>();
		for(Component comp : componentsToRender)
		{
			if(comp instanceof CompSnappingPeg)
			{
				snappingPegs.add((CompSnappingPeg) comp);
			}
		}
		System.out.println("There are " + snappingPegs.size() + " SnappingPegs");
		List<CompSnappingPeg> collector = new ArrayList<>();
		for(CompSnappingPeg peg : snappingPegs)
		{
			if(peg.hasParner())
			{
				continue;
			}
			
			Vector3 connectionPoint = peg.getConnectionPoint();
			board.getSnappingPegsAt(connectionPoint, collector);
			collector.remove(peg); //A peg should always find itself.
			if(!collector.isEmpty())
			{
				double maxDist = 10;
				CompSnappingPeg other = null;
				for(CompSnappingPeg otherPeg : collector)
				{
					Vector3 otherConnectionPoint = otherPeg.getConnectionPoint();
					Vector3 diff = otherConnectionPoint.subtract(connectionPoint);
					double distance = Math.sqrt(diff.dot(diff));
					
					if(distance < maxDist)
					{
						other = otherPeg;
						maxDist = distance;
					}
				}
				
				if(other != null && maxDist < 0.21) //Leave some room for the 0.205 case for now...
				{
					if(other.hasParner())
					{
						System.out.println("!!!! Some snapping peg already has a partner!");
					}
					else
					{
						other.setPartner(peg);
						peg.setPartner(other);
						CompSnappingWire wire = new CompSnappingWire(peg.getParent());
						wire.setConnectorA(peg.getPegs().get(0));
						wire.setConnectorB(other.getPegs().get(0));
						wire.setLength((float) maxDist);
						Vector3 direction = other.getConnectionPoint().subtract(connectionPoint).divide(2); //Get half of it.
						wire.setPosition(connectionPoint.add(direction));
						wire.setRotation(Quaternion.angleAxis(0, direction));
						snappingWires.add(wire);
						componentsToRender.add(wire);
					}
				}
				
				collector.clear();
			}
		}
	}
	
	private void importComponent(Component component)
	{
		if(component instanceof CompBoard)
		{
			boardsToRender.add((CompBoard) component);
		}
		else if(component instanceof CompWireRaw)
		{
			wiresToRender.add((CompWireRaw) component);
		}
		else
		{
			componentsToRender.add(component);
		}
		
		if(component instanceof CompLabel)
		{
			labelsToRender.add((CompLabel) component);
			return;
		}
		
		if(component instanceof CompContainer)
		{
			for(Component child : ((CompContainer) component).getChildren())
			{
				importComponent(child);
			}
		}
	}
	
	public List<CompWireRaw> getWiresToRender()
	{
		return wiresToRender;
	}
	
	public List<Component> getComponentsToRender()
	{
		return componentsToRender;
	}
	
	public List<CompLabel> getLabelsToRender()
	{
		return labelsToRender;
	}
	
	public List<CompBoard> getBoardsToRender()
	{
		return boardsToRender;
	}
	
	public int getNextClusterID()
	{
		return nextClusterID;
	}
}

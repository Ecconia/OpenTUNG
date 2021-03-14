package de.ecconia.java.opentung.core;

import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.CompSnappingWire;
import de.ecconia.java.opentung.components.conductor.Connector;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.CubeOpenRotated;
import de.ecconia.java.opentung.components.fragments.Meshable;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.Part;
import de.ecconia.java.opentung.raycast.RayCastResult;
import de.ecconia.java.opentung.raycast.WireRayCaster;
import de.ecconia.java.opentung.util.MinMaxBox;
import de.ecconia.java.opentung.util.math.Quaternion;
import de.ecconia.java.opentung.util.math.Vector3;

public class CPURaycast
{
	private Part match;
	private double dist;
	
	public Part cpuRaycast(Camera camera, CompBoard rootBoard, boolean skipWireCollision, WireRayCaster wireRayCaster)
	{
		Vector3 cameraPosition = camera.getPosition();
		Vector3 cameraRay = Vector3.zp;
		cameraRay = Quaternion.angleAxis(camera.getNeck(), Vector3.xn).multiply(cameraRay);
		cameraRay = Quaternion.angleAxis(camera.getRotation(), Vector3.yn).multiply(cameraRay);
		
		match = null;
		dist = Double.MAX_VALUE;
		
		//Don't collide with wires, if one is about to be drawn.
		if(!skipWireCollision)
		{
			RayCastResult result = wireRayCaster.castRay(cameraPosition, cameraRay);
			if(result != null && result.getDistance() < dist)
			{
				match = result.getMatch();
				dist = result.getDistance();
			}
		}
		
		focusProbe(rootBoard, cameraPosition, cameraRay);
		
		return match;
	}
	
	private void focusProbe(Component component, Vector3 camPos, Vector3 camRay)
	{
		if(component instanceof CompSnappingWire)
		{
			return;
		}
		
		if(!component.getBounds().contains(camPos))
		{
			double distance = distance(component.getBounds(), camPos, camRay);
			if(distance < 0 || distance >= dist)
			{
				return; //We already found something closer bye.
			}
		}
		
		//Normal or board:
		testComponent(component, camPos, camRay);
		if(component instanceof CompContainer)
		{
			//Test children:
			for(Component child : ((CompContainer) component).getChildren())
			{
				focusProbe(child, camPos, camRay);
			}
		}
	}
	
	private void testComponent(Component component, Vector3 camPos, Vector3 camRay)
	{
		Quaternion componentRotation = component.getRotation();
		Vector3 cameraPositionComponentSpace = componentRotation.multiply(camPos.subtract(component.getPosition())).subtract(component.getModelHolder().getPlacementOffset());
		Vector3 cameraRayComponentSpace = componentRotation.multiply(camRay);
		
		for(Connector connector : component.getConnectors())
		{
			CubeFull shape = connector.getModel();
			Vector3 size = shape.getSize();
			Vector3 cameraRayPegSpace = cameraRayComponentSpace;
			Vector3 cameraPositionPeg = cameraPositionComponentSpace;
			if(shape instanceof CubeOpenRotated)
			{
				Quaternion rotation = ((CubeOpenRotated) shape).getRotation().inverse();
				cameraRayPegSpace = rotation.multiply(cameraRayPegSpace);
				cameraPositionPeg = rotation.multiply(cameraPositionPeg);
			}
			cameraPositionPeg = cameraPositionPeg.subtract(connector.getModel().getPosition());
			
			double distance = distance(size, cameraPositionPeg, cameraRayPegSpace);
			if(distance < 0 || distance >= dist)
			{
				continue;
			}
			
			match = connector;
			dist = distance;
		}
		
		for(Meshable meshable : component.getModelHolder().getSolid())
		{
			CubeFull shape = (CubeFull) meshable;
			Vector3 cameraPositionSolidSpace = cameraPositionComponentSpace.subtract(shape.getPosition());
			Vector3 size = shape.getSize();
			if(shape.getMapper() != null)
			{
				size = shape.getMapper().getMappedSize(size, component);
			}
			
			double distance = distance(size, cameraPositionSolidSpace, cameraRayComponentSpace);
			if(distance < 0 || distance >= dist)
			{
				continue;
			}
			
			match = component;
			dist = distance;
		}
		
		for(Meshable meshable : component.getModelHolder().getColorables())
		{
			CubeFull shape = (CubeFull) meshable;
			Vector3 cameraPositionColorSpace = cameraPositionComponentSpace.subtract(shape.getPosition());
			Vector3 size = shape.getSize();
			
			double distance = distance(size, cameraPositionColorSpace, cameraRayComponentSpace);
			if(distance < 0 || distance >= dist)
			{
				continue;
			}
			
			match = component;
			dist = distance;
		}
	}
	
	private double distance(Vector3 size, Vector3 camPos, Vector3 camRay)
	{
		double xA = (size.getX() - camPos.getX()) / camRay.getX();
		double xB = ((-size.getX()) - camPos.getX()) / camRay.getX();
		double yA = (size.getY() - camPos.getY()) / camRay.getY();
		double yB = ((-size.getY()) - camPos.getY()) / camRay.getY();
		double zA = (size.getZ() - camPos.getZ()) / camRay.getZ();
		double zB = ((-size.getZ()) - camPos.getZ()) / camRay.getZ();
		
		double tMin;
		double tMax;
		{
			if(xA < xB)
			{
				tMin = xA;
				tMax = xB;
			}
			else
			{
				tMin = xB;
				tMax = xA;
			}
			
			double min = yA;
			double max = yB;
			if(min > max)
			{
				min = yB;
				max = yA;
			}
			
			if(min > tMin)
			{
				tMin = min;
			}
			if(max < tMax)
			{
				tMax = max;
			}
			
			min = zA;
			max = zB;
			if(min > max)
			{
				min = zB;
				max = zA;
			}
			
			if(min > tMin)
			{
				tMin = min;
			}
			if(max < tMax)
			{
				tMax = max;
			}
		}
		
		if(tMax < 0)
		{
			return -1; //Behind camera.
		}
		
		if(tMin > tMax)
		{
			return -1; //No collision.
		}
		
		return tMin;
	}
	
	private double distance(MinMaxBox aabb, Vector3 camPos, Vector3 camRay)
	{
		Vector3 minV = aabb.getMin();
		Vector3 maxV = aabb.getMax();
		
		double xA = (maxV.getX() - camPos.getX()) / camRay.getX();
		double xB = (minV.getX() - camPos.getX()) / camRay.getX();
		double yA = (maxV.getY() - camPos.getY()) / camRay.getY();
		double yB = (minV.getY() - camPos.getY()) / camRay.getY();
		double zA = (maxV.getZ() - camPos.getZ()) / camRay.getZ();
		double zB = (minV.getZ() - camPos.getZ()) / camRay.getZ();
		
		double tMin;
		double tMax;
		{
			if(xA < xB)
			{
				tMin = xA;
				tMax = xB;
			}
			else
			{
				tMin = xB;
				tMax = xA;
			}
			
			double min = yA;
			double max = yB;
			if(min > max)
			{
				min = yB;
				max = yA;
			}
			
			if(min > tMin)
			{
				tMin = min;
			}
			if(max < tMax)
			{
				tMax = max;
			}
			
			min = zA;
			max = zB;
			if(min > max)
			{
				min = zB;
				max = zA;
			}
			
			if(min > tMin)
			{
				tMin = min;
			}
			if(max < tMax)
			{
				tMax = max;
			}
		}
		
		if(tMax < 0)
		{
			return -1; //Behind camera.
		}
		
		if(tMin > tMax)
		{
			return -1; //No collision.
		}
		
		return tMin;
	}
}

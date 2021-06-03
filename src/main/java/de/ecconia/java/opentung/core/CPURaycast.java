package de.ecconia.java.opentung.core;

import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.CompSnappingWire;
import de.ecconia.java.opentung.components.conductor.Connector;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.CubeOpenRotated;
import de.ecconia.java.opentung.components.fragments.Meshable;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ConnectedComponent;
import de.ecconia.java.opentung.components.meta.Part;
import de.ecconia.java.opentung.raycast.RayCastResult;
import de.ecconia.java.opentung.raycast.WireRayCaster;
import de.ecconia.java.opentung.util.math.Quaternion;
import de.ecconia.java.opentung.util.math.Vector3;

public class CPURaycast
{
	private Part match;
	private double dist;
	
	public RaycastResult cpuRaycast(Camera camera, CompBoard rootBoard, boolean skipWireCollision, WireRayCaster wireRayCaster)
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
		
		return new RaycastResult(match, dist);
	}
	
	public RayCastResult cpuRaycast(Vector3 position, Vector3 ray, CompBoard rootBoard)
	{
		match = null;
		dist = Double.MAX_VALUE;
		
		focusProbe(rootBoard, position, ray);
		
		return new RayCastResult(dist, match);
	}
	
	private void focusProbe(Component component, Vector3 camPos, Vector3 camRay)
	{
		if(component instanceof CompSnappingWire)
		{
			return;
		}
		
		if(!component.getBounds().contains(camPos))
		{
			double distance = distance(component.getBounds().getMax(), component.getBounds().getMin(), camPos, camRay);
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
		
		if(component instanceof ConnectedComponent)
		{
			for(Connector connector : ((ConnectedComponent) component).getConnectors())
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
				
				double distance = distance(size, size.invert(), cameraPositionPeg, cameraRayPegSpace);
				if(distance < 0 || distance >= dist)
				{
					continue;
				}
				
				match = connector;
				dist = distance;
			}
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
			
			double distance = distance(size, size.invert(), cameraPositionSolidSpace, cameraRayComponentSpace);
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
			
			double distance = distance(size, size.invert(), cameraPositionColorSpace, cameraRayComponentSpace);
			if(distance < 0 || distance >= dist)
			{
				continue;
			}
			
			match = component;
			dist = distance;
		}
	}
	
	private double distance(Vector3 pos, Vector3 neg, Vector3 camPos, Vector3 camRay)
	{
		double xA = (pos.getX() - camPos.getX()) / camRay.getX();
		double xB = (neg.getX() - camPos.getX()) / camRay.getX();
		double yA = (pos.getY() - camPos.getY()) / camRay.getY();
		double yB = (neg.getY() - camPos.getY()) / camRay.getY();
		double zA = (pos.getZ() - camPos.getZ()) / camRay.getZ();
		double zB = (neg.getZ() - camPos.getZ()) / camRay.getZ();
		
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
	
	public static CollisionResult collisionPoint(CompBoard board, Camera camera)
	{
		//TODO: Another ungeneric access
		CubeFull shape = (CubeFull) board.getModelHolder().getSolid().get(0);
		Vector3 position = board.getPosition();
		Quaternion rotation = board.getRotation();
		Vector3 size = shape.getSize();
		if(shape.getMapper() != null)
		{
			size = shape.getMapper().getMappedSize(size, board);
		}
		
		Vector3 cameraPosition = camera.getPosition();
		
		Vector3 cameraRay = Vector3.zp;
		cameraRay = Quaternion.angleAxis(camera.getNeck(), Vector3.xn).multiply(cameraRay);
		cameraRay = Quaternion.angleAxis(camera.getRotation(), Vector3.yn).multiply(cameraRay);
		Vector3 cameraRayBoardSpace = rotation.multiply(cameraRay);
		
		Vector3 cameraPositionBoardSpace = rotation.multiply(cameraPosition.subtract(position)); //Convert the camera position, in the board space.
		
		double distanceLocalMin = (size.getX() - cameraPositionBoardSpace.getX()) / cameraRayBoardSpace.getX();
		double distanceLocalMax = ((-size.getX()) - cameraPositionBoardSpace.getX()) / cameraRayBoardSpace.getX();
		double distanceGlobal;
		Vector3 normalGlobal;
		if(distanceLocalMin < distanceLocalMax)
		{
			distanceGlobal = distanceLocalMin;
			normalGlobal = Vector3.xp;
		}
		else
		{
			distanceGlobal = distanceLocalMax;
			normalGlobal = Vector3.xn;
		}
		
		distanceLocalMin = (size.getY() - cameraPositionBoardSpace.getY()) / cameraRayBoardSpace.getY();
		distanceLocalMax = ((-size.getY()) - cameraPositionBoardSpace.getY()) / cameraRayBoardSpace.getY();
		double distanceLocal;
		Vector3 normalLocal;
		if(distanceLocalMin < distanceLocalMax)
		{
			distanceLocal = distanceLocalMin;
			normalLocal = Vector3.yp;
		}
		else
		{
			distanceLocal = distanceLocalMax;
			normalLocal = Vector3.yn;
		}
		if(distanceGlobal < distanceLocal)
		{
			distanceGlobal = distanceLocal;
			normalGlobal = normalLocal;
		}
		
		distanceLocalMin = (size.getZ() - cameraPositionBoardSpace.getZ()) / cameraRayBoardSpace.getZ();
		distanceLocalMax = ((-size.getZ()) - cameraPositionBoardSpace.getZ()) / cameraRayBoardSpace.getZ();
		if(distanceLocalMin < distanceLocalMax)
		{
			distanceLocal = distanceLocalMin;
			normalLocal = Vector3.zp;
		}
		else
		{
			distanceLocal = distanceLocalMax;
			normalLocal = Vector3.zn;
		}
		if(distanceGlobal < distanceLocal)
		{
			distanceGlobal = distanceLocal;
			normalGlobal = normalLocal;
		}
		
		Vector3 collisionPointBoardSpace = cameraPositionBoardSpace.add(cameraRayBoardSpace.multiply(distanceGlobal));
		return new CollisionResult(normalGlobal, collisionPointBoardSpace);
	}
	
	public static class CollisionResult
	{
		private final Vector3 localNormal;
		private final Vector3 collisionPointBoardSpace;
		
		public CollisionResult(Vector3 normalGlobal, Vector3 collisionPointBoardSpace)
		{
			this.localNormal = normalGlobal;
			this.collisionPointBoardSpace = collisionPointBoardSpace;
		}
		
		public Vector3 getLocalNormal()
		{
			return localNormal;
		}
		
		public Vector3 getCollisionPointBoardSpace()
		{
			return collisionPointBoardSpace;
		}
	}
	
	public static class RaycastResult
	{
		private final Part part;
		private final double distance;
		
		public RaycastResult(Part part, double distance)
		{
			this.part = part;
			this.distance = distance;
		}
		
		public Part getPart()
		{
			return part;
		}
		
		public double getDistance()
		{
			return distance;
		}
	}
}

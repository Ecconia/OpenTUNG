package de.ecconia.java.opentung.raycast;

import de.ecconia.java.opentung.components.conductor.CompWireRaw;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.math.Quaternion;
import de.ecconia.java.opentung.math.Vector3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WireRayCaster
{
	//Debugging cache:
	private List<CastChunkLocation> locations;
	private Vector3 camPos;
	private Vector3 camRay;
	
	public static double maxCastDistance = 100;
	private final Map<CastChunkLocation, List<CompWireRaw>> chunks = new HashMap<>();
	
	public WireRayCaster()
	{
	}
	
	public void clearCache()
	{
		locations = null;
	}
	
	public List<CastChunkLocation> getLocations()
	{
		return locations;
	}
	
	public Vector3 getCamPos()
	{
		return camPos;
	}
	
	public Vector3 getCamRay()
	{
		return camRay;
	}
	
	public RayCastResult castRay(Vector3 camPos, Vector3 camRay)
	{
//		System.out.println();
		int dx = camRay.getX() < 0 ? 0 : 1;
		int dy = camRay.getY() < 0 ? 0 : 1;
		int dz = camRay.getZ() < 0 ? 0 : 1;
		
		CastChunkLocation currentLocation = vec2Pos(camPos);
		boolean addLocations = locations == null;
		RayCastResult result = checkChunk(currentLocation, camPos, camRay);
		if(result != null)
		{
			return result;
		}
		if(addLocations)
		{
			this.camPos = camPos;
			System.out.println("Pos: " + camPos);
			System.out.println("Pos: " + camRay);
			this.camRay = camRay;
			locations = new ArrayList<>();
			locations.add(currentLocation);
			System.out.println("Checking chunk at: " + currentLocation);
		}
		
		while((currentLocation = getNextChunk(currentLocation, dx, dy, dz, camPos, camRay, maxCastDistance)) != null)
		{
			if(addLocations)
			{
				locations.add(currentLocation);
				System.out.println("Checking chunk at: " + currentLocation);
			}
			result = checkChunk(currentLocation, camPos, camRay);
			if(result != null)
			{
				return result;
			}
		}
		
		return null;
	}
	
	public void addWire(CompWireRaw wire)
	{
		//TODO: Use exact bounds:
		Vector3 start = wire.getEnd1();
		Vector3 end = wire.getEnd2();
		
		Vector3 wireRay = end.subtract(start);
		int dx = wireRay.getX() < 0 ? 0 : 1;
		int dy = wireRay.getY() < 0 ? 0 : 1;
		int dz = wireRay.getZ() < 0 ? 0 : 1;
		
		CastChunkLocation chunkLocation = vec2Pos(start);
		addToChunk(chunkLocation, wire);
		while((chunkLocation = getNextChunk(chunkLocation, dx, dy, dz, start, wireRay, wire.getLength())) != null)
		{
			addToChunk(chunkLocation, wire);
		}
	}
	
	public void addToChunk(CastChunkLocation chunkLocation, CompWireRaw wire)
	{
		List<CompWireRaw> chunk = chunks.get(chunkLocation);
		if(chunk == null)
		{
			chunk = new ArrayList<>();
			chunks.put(chunkLocation, chunk);
		}
		chunk.add(wire);
	}
	
	public CastChunkLocation getNextChunk(CastChunkLocation currentChunk, int dx, int dy, int dz, Vector3 camPos, Vector3 camRay, double limit)
	{
		double tx = currentChunk.getX() + dx;
		double ty = currentChunk.getY() + dy;
		double tz = currentChunk.getZ() + dz;
		
		double ttx = (tx - camPos.getX()) / camRay.getX();
		double tty = (ty - camPos.getY()) / camRay.getY();
		double ttz = (tz - camPos.getZ()) / camRay.getZ();
		
		double distance;
		CastChunkLocation newChunk;
		if(tty < ttz)
		{
			if(ttx < tty)
			{
				if(dx == 0)
				{
					dx = -1;
				}
				distance = ttx;
				newChunk = new CastChunkLocation(currentChunk.getX() + dx, currentChunk.getY(), currentChunk.getZ());
			}
			else
			{
				if(dy == 0)
				{
					dy = -1;
				}
				distance = tty;
				newChunk = new CastChunkLocation(currentChunk.getX(), currentChunk.getY() + dy, currentChunk.getZ());
			}
		}
		else
		{
			if(ttx < ttz)
			{
				if(dx == 0)
				{
					dx = -1;
				}
				distance = ttx;
				newChunk = new CastChunkLocation(currentChunk.getX() + dx, currentChunk.getY(), currentChunk.getZ());
			}
			else
			{
				if(dz == 0)
				{
					dz = -1;
				}
				distance = ttz;
				newChunk = new CastChunkLocation(currentChunk.getX(), currentChunk.getY(), currentChunk.getZ() + dz);
			}
		}
		
		if(distance > limit)
		{
			return null;
		}
		
		return newChunk;
	}
	
	public RayCastResult checkChunk(CastChunkLocation chunkLocation, Vector3 camPos, Vector3 camRay)
	{
//		System.out.println("Checking chunk at: " + chunk);
		List<CompWireRaw> chunk = chunks.get(chunkLocation);
		if(chunk == null)
		{
			return null;
		}

//		System.out.println("Checking chunk " + chunkLocation + " with " + chunk.size() + " wires.");
		double distance = Double.MAX_VALUE;
		CompWireRaw match = null;
		for(CompWireRaw wire : chunk)
		{
			Quaternion wireRotation = wire.getRotation();
			Vector3 cameraPositionBoardSpace = wireRotation.multiply(camPos.subtract(wire.getPosition()));
			Vector3 cameraRayBoardSpace = wireRotation.multiply(camRay);
			CubeFull shape = (CubeFull) wire.getModelHolder().getConductors().get(0);
			Vector3 size = shape.getSize();
			if(shape.getMapper() != null)
			{
				size = shape.getMapper().getMappedSize(size, wire);
			}
			
			double xr = 1.0 / cameraRayBoardSpace.getX();
			double yr = 1.0 / cameraRayBoardSpace.getY();
			double zr = 1.0 / cameraRayBoardSpace.getZ();
			
			double xA = (size.getX() - cameraPositionBoardSpace.getX()) * xr;
			double xB = ((-size.getX()) - cameraPositionBoardSpace.getX()) * xr;
			double yA = (size.getY() - cameraPositionBoardSpace.getY()) * yr;
			double yB = ((-size.getY()) - cameraPositionBoardSpace.getY()) * yr;
			double zA = (size.getZ() - cameraPositionBoardSpace.getZ()) * zr;
			double zB = ((-size.getZ()) - cameraPositionBoardSpace.getZ()) * zr;
			
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
				continue; //Behind camera.
			}
			
			if(tMin > tMax)
			{
				continue; //No collision.
			}
			
			if(tMin < distance)
			{
				match = wire;
				distance = tMin;
			}
		}
		
		if(match != null)
		{
//			System.out.println("Found: " + distance);
			return new RayCastResult(distance, match);
		}
		
		return null;
	}
	
	public CastChunkLocation vec2Pos(Vector3 position)
	{
		return new CastChunkLocation(
				(int) Math.floor(position.getX()),
				(int) Math.floor(position.getY()),
				(int) Math.floor(position.getZ())
		);
	}
}

package de.ecconia.java.opentung.raycast;

import de.ecconia.java.opentung.components.conductor.CompWireRaw;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.util.math.Quaternion;
import de.ecconia.java.opentung.util.math.Vector3;
import de.ecconia.java.opentung.settings.Settings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WireRayCaster
{
	private final Map<CastChunkLocation, List<CompWireRaw>> chunks = new HashMap<>();
	
	public RayCastResult castRay(Vector3 camPos, Vector3 camRay)
	{
		int dx = camRay.getX() < 0 ? 0 : 1;
		int dy = camRay.getY() < 0 ? 0 : 1;
		int dz = camRay.getZ() < 0 ? 0 : 1;
		CastChunkLocation currentLocation = vec2Pos(camPos);
		RayCastResult result = checkChunk(currentLocation, camPos, camRay);
		if(result != null)
		{
			return result;
		}
		
		while((currentLocation = getNextChunk(currentLocation, dx, dy, dz, camPos, camRay, Settings.maxCastDistance)) != null)
		{
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
		if(wire.getLength() <= 0 )
		{
			return;
		}
		double halfLength = wire.getLength() / 2.0;
		Vector3 ray = new Vector3(0, 0, 1);
		Vector3 a = new Vector3(+0.025, +0.01, -halfLength);
		Vector3 b = new Vector3(+0.025, -0.01, -halfLength);
		Vector3 c = new Vector3(-0.025, +0.01, -halfLength);
		Vector3 d = new Vector3(-0.025, -0.01, -halfLength);
		
		Quaternion rot = wire.getRotation().inverse();
		ray = rot.multiply(ray);
		a = rot.multiply(a).add(wire.getPosition());
		b = rot.multiply(b).add(wire.getPosition());
		c = rot.multiply(c).add(wire.getPosition());
		d = rot.multiply(d).add(wire.getPosition());
		
		int dx = ray.getX() < 0 ? 0 : 1;
		int dy = ray.getY() < 0 ? 0 : 1;
		int dz = ray.getZ() < 0 ? 0 : 1;
		
		addToChunks(wire, a, ray, dx, dy, dz);
		addToChunks(wire, b, ray, dx, dy, dz);
		addToChunks(wire, c, ray, dx, dy, dz);
		addToChunks(wire, d, ray, dx, dy, dz);
	}
	
	public void removeWire(CompWireRaw wire)
	{
		if(wire.getLength() <= 0 )
		{
			return; //We don't have such wires here.
		}
		double halfLength = wire.getLength() / 2.0;
		Vector3 ray = new Vector3(0, 0, 1);
		Vector3 a = new Vector3(+0.025, +0.01, -halfLength);
		Vector3 b = new Vector3(+0.025, -0.01, -halfLength);
		Vector3 c = new Vector3(-0.025, +0.01, -halfLength);
		Vector3 d = new Vector3(-0.025, -0.01, -halfLength);
		
		Quaternion rot = wire.getRotation().inverse();
		ray = rot.multiply(ray);
		a = rot.multiply(a).add(wire.getPosition());
		b = rot.multiply(b).add(wire.getPosition());
		c = rot.multiply(c).add(wire.getPosition());
		d = rot.multiply(d).add(wire.getPosition());
		
		int dx = ray.getX() < 0 ? 0 : 1;
		int dy = ray.getY() < 0 ? 0 : 1;
		int dz = ray.getZ() < 0 ? 0 : 1;
		
		removeFromChunks(wire, a, ray, dx, dy, dz);
		removeFromChunks(wire, b, ray, dx, dy, dz);
		removeFromChunks(wire, c, ray, dx, dy, dz);
		removeFromChunks(wire, d, ray, dx, dy, dz);
	}
	
	private void addToChunks(CompWireRaw wire, Vector3 pos, Vector3 ray, int dx, int dy, int dz)
	{
		CastChunkLocation chunkLocation = vec2Pos(pos);
		addToChunk(chunkLocation, wire);
		while((chunkLocation = getNextChunk(chunkLocation, dx, dy, dz, pos, ray, wire.getLength())) != null)
		{
			addToChunk(chunkLocation, wire);
		}
	}
	
	private void removeFromChunks(CompWireRaw wire, Vector3 pos, Vector3 ray, int dx, int dy, int dz)
	{
		CastChunkLocation chunkLocation = vec2Pos(pos);
		removeFromChunk(chunkLocation, wire);
		while((chunkLocation = getNextChunk(chunkLocation, dx, dy, dz, pos, ray, wire.getLength())) != null)
		{
			removeFromChunk(chunkLocation, wire);
		}
	}
	
	private void addToChunk(CastChunkLocation chunkLocation, CompWireRaw wire)
	{
		List<CompWireRaw> chunk = chunks.get(chunkLocation);
		if(chunk == null)
		{
			chunk = new ArrayList<>();
			chunks.put(chunkLocation, chunk);
		}
		if(!chunk.contains(wire))
		{
			chunk.add(wire);
		}
	}
	
	private void removeFromChunk(CastChunkLocation chunkLocation, CompWireRaw wire)
	{
		List<CompWireRaw> chunk = chunks.get(chunkLocation);
		if(chunk != null)
		{
			chunk.remove(wire);
			if(chunk.isEmpty())
			{
				chunks.remove(chunkLocation);
			}
		}
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
		List<CompWireRaw> chunk = chunks.get(chunkLocation);
		if(chunk == null)
		{
			return null;
		}
		
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

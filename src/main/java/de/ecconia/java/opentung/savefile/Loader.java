package de.ecconia.java.opentung.savefile;

import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.conductor.CompWireRaw;
import de.ecconia.java.opentung.components.conductor.Connector;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ComponentAwareness;
import de.ecconia.java.opentung.components.meta.CustomData;
import de.ecconia.java.opentung.components.meta.PlaceableInfo;
import de.ecconia.java.opentung.simulation.Powerable;
import de.ecconia.java.opentung.util.io.ByteReader;
import de.ecconia.java.opentung.util.math.MathHelper;
import de.ecconia.java.opentung.util.math.Quaternion;
import de.ecconia.java.opentung.util.math.Vector3;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class Loader
{
	public static BoardAndWires load(File savefile)
	{
		try
		{
			System.out.println("[OpenTUNG-Loading] Parsing savefile: " + savefile.getAbsolutePath());
			byte[] data = Files.readAllBytes(savefile.toPath());
			System.out.println("[OpenTUNG-Loading] Read savefile to memory.");
			ByteReader reader = new ByteReader(data);
			return parseBoard(reader);
		}
		catch(ArrayIndexOutOfBoundsException e)
		{
			throw new RuntimeException("Savefile probably ended unexpected, see stacktrace for details.", e);
		}
		catch(IOException e)
		{
			throw new RuntimeException("Could not read/load tungboard file.", e);
		}
	}
	
	private static BoardAndWires parseBoard(ByteReader reader)
	{
		{
			if(reader.getRemaining() < 12)
			{
				throw new RuntimeException("Savefile does not start with correct header (Too small).");
			}
			String header = CompactText.decode(reader.readBytes(12));
			if(!header.equals("OpenTUNG-Boards"))
			{
				throw new RuntimeException("Save does not start with correct header.");
			}
		}
		
		int fileVersion = reader.readVariableInt();
		if(fileVersion != 1)
		{
			throw new RuntimeException("Unsupported savefile version: " + fileVersion);
		}
		
		int componentCount = reader.readVariableInt();
		if(componentCount == 0)
		{
			throw new RuntimeException("Empty savefile, no board/component to load.");
		}
		int wireCount = reader.readVariableInt();
		
		int dictionarySize = reader.readVariableInt();
		DictionaryEntry[] dictionary = new DictionaryEntry[dictionarySize];
		int connectorCount = 0;
		for(int i = 0; i < dictionarySize; i++)
		{
			DictionaryEntry entry = new DictionaryEntry(i, reader);
			dictionary[i] = entry;
			connectorCount += entry.getComponentCount() * (entry.getBlots() + entry.getPegs());
		}
		
		Map<String, PlaceableInfo> componentLookup = new HashMap<>();
		for(PlaceableInfo info : ComponentAwareness.componentTypes)
		{
			componentLookup.put(info.getName(), info);
		}
		
		int connectorIndex = 0;
		Connector[] connectors = new Connector[connectorCount];
		Component[] components = new Component[componentCount + 1];
		for(int i = 1; i <= componentCount; i++)
		{
			int typeId = reader.readVariableInt();
			if(typeId >= dictionarySize)
			{
				throw new RuntimeException("Component type reference outside dictionary.");
			}
			DictionaryEntry data = dictionary[typeId];
			PlaceableInfo info = componentLookup.get(data.getTag());
			if(info == null)
			{
				throw new RuntimeException("Couldn't find the placeable info for tag: '" + data.getTag() + "'");
			}
			
			//Parent:
			int parentID = reader.readVariableInt();
			Component parent = null;
			if(parentID != 0)
			{
				if(parentID >= i)
				{
					throw new RuntimeException("Parent is referencing to a not yet read component.");
				}
				parent = components[parentID];
				if(!(parent instanceof CompContainer))
				{
					throw new RuntimeException("Parent is not a container type: " + parent.getClass().getSimpleName() + " for " + info.getName());
				}
			}
			Component component = info.instance((CompContainer) parent);
			
			//Position:
			Vector3 position = new Vector3(reader.readDouble(), reader.readDouble(), reader.readDouble());
			component.setPosition(position);
			//Rotation:
			Vector3 qV = new Vector3(reader.readDouble(), reader.readDouble(), reader.readDouble());
			Quaternion rotation = new Quaternion(reader.readDouble(), qV);
			component.setRotation(rotation);
			//Blot outputs:
			for(int j = 0; j < data.getBlots(); j++)
			{
				((Powerable) component).setPowered(i, reader.readBoolean());
			}
			//Custom data:
			if(data.hasCustomData())
			{
				byte[] customData = reader.readBytes(reader.readVariableInt());
				((CustomData) component).setCustomData(customData);
			}
			//Component init:
			component.init(); //Has to be called for some components after pos/rot is set.
			
			if(parent != null)
			{
				((CompContainer) parent).addChild(component);
			}
			for(Connector connector : component.getConnectors())
			{
				connectors[connectorIndex++] = connector;
			}
			components[i] = component;
		}
		
		if(!(components[1] instanceof CompBoard))
		{
			throw new RuntimeException("First component in savefile was not a board.");
		}
		
		CompWireRaw[] wires = new CompWireRaw[wireCount];
		for(int i = 0; i < wireCount; i++)
		{
			int aID = reader.readVariableInt();
			int bID = reader.readVariableInt();
			
			Connector a = connectors[aID];
			Connector b = connectors[bID];
			
			CompWireRaw wire = new CompWireRaw(null);
			
			Vector3 fromPos = a.getConnectionPoint();
			Vector3 toPos = b.getConnectionPoint();
			
			//Pos + Rot
			Vector3 direction = fromPos.subtract(toPos).divide(2);
			double distance = direction.length();
			Quaternion rotation = MathHelper.rotationFromVectors(Vector3.zp, direction.normalize());
			Vector3 position = toPos.add(direction);
			wire.setRotation(rotation);
			wire.setPosition(position);
			wire.setLength((float) distance * 2f);
			
			wire.setConnectorA(a);
			wire.setConnectorB(b);
			
			wires[i] = wire;
		}
		
		return new BoardAndWires((CompBoard) components[1], wires);
	}
}

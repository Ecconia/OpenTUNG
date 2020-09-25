package de.ecconia.java.opentung.savefile;

import de.ecconia.java.opentung.BoardUniverse;
import de.ecconia.java.opentung.PlaceableInfo;
import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.conductor.Blot;
import de.ecconia.java.opentung.components.conductor.CompWireRaw;
import de.ecconia.java.opentung.components.conductor.Connector;
import de.ecconia.java.opentung.components.conductor.Peg;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.CustomData;
import de.ecconia.java.opentung.math.Quaternion;
import de.ecconia.java.opentung.math.Vector3;
import de.ecconia.java.opentung.simulation.Wire;
import de.ecconia.java.opentung.util.io.ByteWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

public class Saver
{
	public static void save(BoardUniverse boardWrapper, File saveFile)
	{
		//Getting:
		int componentAmount = boardWrapper.getBoardsToRender().size() + boardWrapper.getComponentsToRender().size() - boardWrapper.getSnappingWires().size();
		List<CompWireRaw> wires = boardWrapper.getWiresToRender();
		CompBoard rootBoard = boardWrapper.getRootBoard();
		
		//Collecting:
		int expectedBoards = boardWrapper.getBoardsToRender().size();
		int boardCount = 0;
		List<Component> components = new ArrayList<>(componentAmount);
		{
			Queue<Component> pending = new LinkedList<>();
			pending.add(rootBoard);
			while(!pending.isEmpty())
			{
				Component current = pending.remove();
				if(current instanceof CompWireRaw)
				{
					continue;
				}
				if(current instanceof CompBoard)
				{
					boardCount++;
				}
				components.add(current);
				if(current instanceof CompContainer)
				{
					for(Component child : ((CompContainer) current).getChildren())
					{
						pending.add(child);
					}
				}
			}
		}
		if(boardCount != expectedBoards)
		{
			System.out.println("[OpenTUNG-Saver] BOARDS: " + boardCount + " / " + expectedBoards);
		}
		if(components.size() != componentAmount)
		{
			System.out.println("[OpenTUNG-Saver] COMPONENTS: " + components.size() + " / " + componentAmount);
		}
		else
		{
			System.out.println("[OpenTUNG-Saver] Collected: " + components.size());
		}
		
		Map<Component, Integer> componentIDs = new HashMap<>();
		Map<Connector, Integer> connectorIDs = new HashMap<>();
		Map<PlaceableInfo, DictionaryEntry> dictionary = new HashMap<>();
		int componentID = 1;
		int connectorID = 0;
		int dictionaryID = 0;
		for(Component component : components)
		{
			componentIDs.put(component, componentID++);
			PlaceableInfo info = component.getInfo();
			DictionaryEntry data = dictionary.get(info);
			if(data == null)
			{
				data = new DictionaryEntry(info, dictionaryID++);
				dictionary.put(info, data);
			}
			data.incrementCounter();
			for(Peg peg : component.getPegs())
			{
				connectorIDs.put(peg, connectorID++);
			}
			for(Blot blot : component.getBlots())
			{
				connectorIDs.put(blot, connectorID++);
			}
		}
		
		System.out.println("[OpenTUNG-Saver] Connector IDs: " + connectorID);
		
		System.out.println("[OpenTUNG-Saver] Dumping component dictionary:");
		List<DictionaryEntry> sortedDictionary = dictionary.values().stream().sorted(Comparator.comparingInt(DictionaryEntry::getId)).collect(Collectors.toList());
		for(DictionaryEntry data : sortedDictionary)
		{
			String id = String.valueOf(data.getId());
			if(id.length() < 2)
			{
				id = '0' + id;
			}
			System.out.println("ID: " + id
					+ " P: " + data.getPegs()
					+ " B: " + data.getBlots()
					+ " V: " + data.getVersion()
					+ " CD: " + data.hasCustomData()
					+ " T: \"" + data.getTag() + "\""
					+ " U: " + data.getComponentCount());
		}
		
		//TBI: Store blots data as separate section with bit-wise encoding?
		
		try
		{
			ByteWriter writer = new ByteWriter(saveFile);
			//Write OpenTUNG header:
			writer.writeBytes(CompactText.encode("OpenTUNG-Boards")); //No length prefix, thus this unwrap.
			//Write file-version:
			writer.writeByte(1);
			//Write counts of components and wires:
			writer.writeVariableInt(components.size());
			writer.writeVariableInt(wires.size());
			//Write component dictionary with: (see above):
			writer.writeVariableInt(sortedDictionary.size());
			for(DictionaryEntry data : sortedDictionary)
			{
				//Tag:
				writer.writeCompactString(data.getTag());
				//Version:
				writer.writeCompactString(data.getVersion());
				//Pegs:
				writer.writeVariableInt(data.getPegs());
				//Blots:
				writer.writeVariableInt(data.getBlots());
				//CustomData:
				writer.writeBoolean(data.hasCustomData());
				//Usages:
				writer.writeVariableInt(data.getComponentCount());
			}
			//Write components with: type-id, position, direction, blots, custom-data
			for(Component component : components)
			{
				DictionaryEntry data = dictionary.get(component.getInfo());
				int typeId = data.getId();
				writer.writeVariableInt(typeId);
				//Parent:
				Component parent = component.getParent();
				int parentID = parent == null ? 0 : componentIDs.get(parent);
				writer.writeVariableInt(parentID);
				//Position:
				Vector3 position = component.getPosition();
				writer.writeDouble(position.getX());
				writer.writeDouble(position.getY());
				writer.writeDouble(position.getZ());
				//Direction/Rotation:
				Quaternion quaternion = component.getRotation();
				Vector3 v = quaternion.getV();
				double a = quaternion.getA();
				writer.writeDouble(v.getX());
				writer.writeDouble(v.getY());
				writer.writeDouble(v.getZ());
				writer.writeDouble(a);
				//Blots:
				for(Blot blot : component.getBlots())
				{
					writer.writeBoolean(blot.getCluster().isActive());
				}
				//Custom-Data:
				if(data.hasCustomData())
				{
					byte[] customData = ((CustomData) component).getCustomData();
					writer.writeVariableInt(customData.length);
					writer.writeBytes(customData);
				}
			}
			//Write wires with: connector-id, connector-id, rotation
			for(Wire wire : wires)
			{
				Connector c = wire.getConnectorA();
				int id = connectorIDs.get(c);
				writer.writeVariableInt(id);
				c = wire.getConnectorB();
				id = connectorIDs.get(c);
				writer.writeVariableInt(id);
			}
			
			writer.close();
		}
		catch(Exception e)
		{
			//TODO: Handle.
			System.out.println("Error while saving, please report. Continuing.");
			e.printStackTrace();
		}
	}
}

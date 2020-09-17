package de.ecconia.java.opentung.savefile;

import de.ecconia.java.opentung.BoardUniverse;
import de.ecconia.java.opentung.OpenTUNG;
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
	public static void save(BoardUniverse boardWrapper)
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
			System.out.println("BOARDS: " + boardCount + " / " + expectedBoards);
		}
		if(components.size() != componentAmount)
		{
			System.out.println("COMPONENTS: " + components.size() + " / " + componentAmount);
		}
		else
		{
			System.out.println("Collected: " + components.size());
		}
		
		Map<Component, Integer> componentIDs = new HashMap<>();
		Map<Connector, Integer> connectorIDs = new HashMap<>();
		Map<PlaceableInfo, ComponentData> dictionary = new HashMap<>();
		int componentID = 1;
		int connectorID = 0;
		int dictionaryID = 0;
		for(Component component : components)
		{
			componentIDs.put(component, componentID++);
			PlaceableInfo info = component.getInfo();
			ComponentData data = dictionary.get(info);
			if(data == null)
			{
				data = new ComponentData(info, dictionaryID++);
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
		
		List<ComponentData> sortedDictionary = dictionary.values().stream().sorted(Comparator.comparingInt(ComponentData::getId)).collect(Collectors.toList());
		for(ComponentData data : sortedDictionary)
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
		
		File saveFolder = new File(OpenTUNG.dataFolder, "boards");
		File saveFile = new File(saveFolder, "testsave.opentung");
		
		try
		{
			//TODO: For the sake of <your god(s) here>, add a buffer array.
			FileOutputStream fos = new FileOutputStream(saveFile, false);
			//Write OpenTUNG header:
			fos.write(CompactText.encode("OpenTUNG-Boards"));
			//Write file-version:
			fos.write(ByteLevelHelper.writeUnsignedInt(1));
			//Write counts of components and wires:
			fos.write(ByteLevelHelper.writeUnsignedInt(components.size()));
			fos.write(ByteLevelHelper.writeUnsignedInt(wires.size()));
			//Write component dictionary with: (see above):
			fos.write(ByteLevelHelper.writeUnsignedInt(sortedDictionary.size()));
			for(ComponentData data : sortedDictionary)
			{
				//Tag:
				byte[] bytes = CompactText.encode(data.getTag());
				fos.write(ByteLevelHelper.writeUnsignedInt(bytes.length));
				fos.write(bytes);
				//Version:
				bytes = CompactText.encode(data.getVersion());
				fos.write(ByteLevelHelper.writeUnsignedInt(bytes.length));
				fos.write(bytes);
				//Pegs:
				fos.write(ByteLevelHelper.writeUnsignedInt(data.getPegs()));
				//Blots:
				fos.write(ByteLevelHelper.writeUnsignedInt(data.getBlots()));
				//CustomData:
				fos.write(ByteLevelHelper.writeBoolean(data.hasCustomData()));
				//Usages:
				fos.write(ByteLevelHelper.writeUnsignedInt(data.getComponentCount()));
			}
			//Write components with: type-id, position, direction, blots, custom-data
			for(Component component : components)
			{
				ComponentData data = dictionary.get(component.getInfo());
				int typeId = data.getId();
				fos.write(ByteLevelHelper.writeUnsignedInt(typeId));
				//Parent:
				Component parent = component.getParent();
				int parentID = parent == null ? 0 : componentIDs.get(parent);
				fos.write(ByteLevelHelper.writeUnsignedInt(parentID));
				//Position:
				Vector3 position = component.getPosition();
				fos.write(ByteLevelHelper.writeDouble(position.getX()));
				fos.write(ByteLevelHelper.writeDouble(position.getY()));
				fos.write(ByteLevelHelper.writeDouble(position.getZ()));
				//Direction/Rotation:
				Quaternion quaternion = component.getRotation();
				Vector3 v = quaternion.getV();
				double a = quaternion.getA();
				fos.write(ByteLevelHelper.writeDouble(v.getX()));
				fos.write(ByteLevelHelper.writeDouble(v.getY()));
				fos.write(ByteLevelHelper.writeDouble(v.getZ()));
				fos.write(ByteLevelHelper.writeDouble(a));
				//Blots:
				for(Blot blot : component.getBlots())
				{
					fos.write(ByteLevelHelper.writeBoolean(blot.getCluster().isActive()));
				}
				//Custom-Data:
				if(data.hasCustomData())
				{
					byte[] customData = ((CustomData) component).getCustomData();
					fos.write(ByteLevelHelper.writeUnsignedInt(customData.length));
					fos.write(customData);
				}
			}
			//Write wires with: connector-id, connector-id, rotation
			for(Wire wire : wires)
			{
				Connector c = wire.getConnectorA();
				int id = connectorIDs.get(c);
				fos.write(ByteLevelHelper.writeUnsignedInt(id));
				c = wire.getConnectorB();
				id = connectorIDs.get(c);
				fos.write(ByteLevelHelper.writeUnsignedInt(id));
			}
			
			fos.flush();
			fos.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private static class ComponentData
	{
		private final int id;
		private final String tag;
		private final String version;
		private final int pegs;
		private final int blots;
		private final boolean customData;
		
		private int componentCount;
		
		public ComponentData(PlaceableInfo info, int id)
		{
			this.id = id;
			this.pegs = info.getModel().getPegModels().size();
			this.blots = info.getModel().getBlotModels().size();
			this.customData = info.hasCustomData();
			this.version = info.getVersion();
			
			String tag = info.getName();
			if(tag.startsWith("TUNG-"))
			{
				tag = "TUNG." + tag.substring(5);
			}
			this.tag = tag;
		}
		
		public void incrementCounter()
		{
			componentCount++;
		}
		
		public int getId()
		{
			return id;
		}
		
		public String getTag()
		{
			return tag;
		}
		
		public String getVersion()
		{
			return version;
		}
		
		public int getComponentCount()
		{
			return componentCount;
		}
		
		public int getPegs()
		{
			return pegs;
		}
		
		public int getBlots()
		{
			return blots;
		}
		
		public boolean hasCustomData()
		{
			return customData;
		}
	}
}

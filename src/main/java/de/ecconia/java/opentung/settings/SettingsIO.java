package de.ecconia.java.opentung.settings;

import de.ecconia.java.opentung.util.Ansi;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SettingsIO
{
	private final File file;
	private final Node root = new Node("root");
	
	public SettingsIO(File file, DataFolderWatcher watcher, Class<?> settingsClass)
	{
		this.file = file;
		
		createSettingsNode(settingsClass);
		
		if(file.exists())
		{
			loadFile();
		}
		else
		{
			writeDefaultFile();
		}
		
		if(!watcher.register(file.getName(), () -> {
			System.out.println("[Settings] Updating from file:");
			resetKeys();
			try
			{
				loadFile();
			}
			catch(Exception e)
			{
				System.out.println();
				System.out.println("[Settings] " + Ansi.red + "Was not able to load settings. See exception:" + Ansi.r);
				e.printStackTrace(System.out);
				System.out.println();
			}
		}))
		{
			System.out.println("[Settings] " + Ansi.red + "File watcher could not be initialized, settings won't update on runtime." + Ansi.r);
		}
	}
	
	private void createSettingsNode(Class<?> settingsClass)
	{
		for(Field field : settingsClass.getFields())
		{
			SettingInfo info = field.getAnnotation(SettingInfo.class);
			if(info != null)
			{
				SettingsSectionStart section = field.getAnnotation(SettingsSectionStart.class);
				if(section != null)
				{
					Node node = root.getNode(section.key(), true);
					node.setComment(section.comment());
				}
				
				Node node = root.getNode(info.key(), true);
				if(!info.comment().isEmpty())
				{
					node.setComment(info.comment());
				}
				node.setField(field);
			}
			else
			{
				System.out.println("[Settings] " + Ansi.red + "WARNING: Settings variable without settings key: " + Ansi.r + field.getName());
			}
		}
	}
	
	private void resetKeys()
	{
		resetKeys(root);
	}
	
	private void resetKeys(Node node)
	{
		node.setVisit(false);
		for(Node child : node.getChildren())
		{
			resetKeys(child);
		}
	}
	
	private void loadFile()
	{
		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(file));
			
			String line = getNextValidLine(reader);
			if(line != null)
			{
				int ownIndentation = countIndentation(line);
				String content = line.replace('\t', ' ').trim();
				
				int separatorIndex = content.indexOf(':');
				if(separatorIndex < 0)
				{
					throw new RuntimeException("Malformed settings file. No ':' in line with content: " + content);
				}
				
				String key = content.substring(0, separatorIndex);
				String value = content.substring(separatorIndex + 1).trim();
				
				//Applying:
				
				readSetting(key, value);
				
				//Assume child:
				
				String nextLine = getNextValidLine(reader);
				if(nextLine != null)
				{
					int nextLineIndentation = countIndentation(nextLine);
					line = readLayer("\t", reader, nextLineIndentation, ownIndentation, key + ".", nextLine);
					if(line != null)
					{
						throw new RuntimeException("Could not read all keys, since non-root keys are not supported yet!");
					}
				}
			}
			
			reader.close();
			
			//Verify that all settings are present in the file.
			for(Node child : root.getChildren())
			{
				verifyKeys("", child);
			}
		}
		catch(IOException e)
		{
			throw new RuntimeException("Could not read settings file.", e);
		}
	}
	
	private void verifyKeys(String key, Node node)
	{
		String fullKey = key + node.getKey();
		if(node.getField() != null && !node.isVisited())
		{
			System.out.println("[Settings] " + Ansi.red + "WARNING: Setting " + Ansi.r + fullKey + Ansi.red + " is not present in settings file. Default value: " + Ansi.r + node.getDefaultValue());
		}
		else
		{
			for(Node child : node.getChildren())
			{
				verifyKeys(fullKey + ".", child);
			}
		}
	}
	
	private String getNextValidLine(BufferedReader reader) throws IOException
	{
		while(true)
		{
			String line = reader.readLine();
			if(line == null)
			{
				return null;
			}
			
			if(line.replace('\t', ' ').trim().startsWith("#"))
			{
				continue;
			}
			
			return line;
		}
	}
	
	private int countIndentation(String line)
	{
		int ownIndentation = 0;
		for(char c : line.toCharArray())
		{
			if(c != ' ' && c != '\t')
			{
				break;
			}
			ownIndentation++;
		}
		return ownIndentation;
	}
	
	private String readLayer(String debugPrefix, BufferedReader reader, int newIndentation, int parentIndentation, String parentKey, String line) throws IOException
	{
		while(true)
		{
			if(newIndentation <= parentIndentation)
			{
				return line;
			}
			
			String content = line.replace('\t', ' ').trim();
			
			int separatorIndex = content.indexOf(':');
			if(separatorIndex < 0)
			{
				throw new RuntimeException("Malformed settings file. No ':' in line with content: " + content);
			}
			
			String key = parentKey + content.substring(0, separatorIndex);
			String value = content.substring(separatorIndex + 1).trim();
			
			//Applying:
			
			readSetting(key, value);
			
			//Assume child:
			
			String nextLine = getNextValidLine(reader);
			if(nextLine != null)
			{
				int nextLineIndentation = countIndentation(nextLine);
				line = readLayer(debugPrefix + "\t", reader, nextLineIndentation, newIndentation, key + ".", nextLine);
				
				if(line == null)
				{
					return null;
				}
				newIndentation = countIndentation(line);
			}
			else
			{
				return null;
			}
		}
	}
	
	private void readSetting(String key, String value)
	{
		if(!value.isEmpty())
		{
			Node node = root.getNode(key, false);
			if(node == null)
			{
				throw new RuntimeException("Settings contain unknown key: " + key);
			}
			
			if(node.setValueIfRequired(key, value))
			{
				System.out.println("[Settings] Setting " + key + " to '" + value + "'");
			}
		}
	}
	
	private void writeDefaultFile()
	{
		StringBuilder builder = new StringBuilder();
		for(Node child : root.getChildren())
		{
			printNode("", child, builder);
		}
		
		try
		{
			FileWriter writer = new FileWriter(file, false);
			writer.write(builder.toString());
			writer.flush();
			writer.close();
			System.out.println("[Settings] Written default settings file.");
		}
		catch(IOException e)
		{
			throw new RuntimeException("Could not save default settings file.", e);
		}
	}
	
	private void printNode(String prefix, Node node, StringBuilder builder)
	{
		String comment = node.getComment();
		if(comment != null)
		{
			builder.append(prefix).append('#').append(comment.replace("\n", "\n" + prefix + "#")).append('\n');
		}
		Field field = node.getField();
		Collection<Node> children = node.getChildren();
		if(field == null)
		{
			builder.append(prefix).append(node.getKey()).append(":\n");
		}
		else
		{
			builder.append(prefix).append(node.getKey()).append(": ").append(node.getDefaultValue()).append('\n');
		}
		for(Node child : children)
		{
			printNode(prefix + "\t", child, builder);
		}
	}
	
	private static String valueAsString(Field field)
	{
		Class<?> type = field.getType();
		try
		{
			if(type == Float.TYPE)
			{
				return String.valueOf(field.getFloat(field));
			}
			if(type == Double.TYPE)
			{
				return String.valueOf(field.getDouble(field));
			}
			else if(type == Integer.TYPE)
			{
				return String.valueOf(field.getInt(field));
			}
			else if(type == Boolean.TYPE)
			{
				return String.valueOf(field.getBoolean(field));
			}
			else
			{
				throw new RuntimeException("Unknown type: " + field.getType().getName());
			}
		}
		catch(IllegalAccessException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private static class Node
	{
		private final String key;
		private String comment;
		private Field field;
		private String defaultValue;
		private String lastValue;
		private boolean visited;
		
		private final List<Node> children = new ArrayList<>();
		
		public Node(String key)
		{
			this.key = key;
		}
		
		public void setComment(String comment)
		{
			this.comment = comment;
		}
		
		public void setField(Field field)
		{
			this.field = field;
			this.defaultValue = lastValue = valueAsString(field);
		}
		
		public String getKey()
		{
			return key;
		}
		
		public String getComment()
		{
			return comment;
		}
		
		public Field getField()
		{
			return field;
		}
		
		public String getDefaultValue()
		{
			return defaultValue;
		}
		
		public List<Node> getChildren()
		{
			return children;
		}
		
		public Node getNode(String key, boolean createNew)
		{
			String[] keys = key.split("\\.", 2);
			Node childKey = getChildKey(keys[0], createNew);
			if(childKey == null)
			{
				return null;
			}
			if(keys.length == 1)
			{
				return childKey;
			}
			else
			{
				return childKey.getNode(keys[1], createNew);
			}
		}
		
		private Node getChildKey(String key, boolean createNew)
		{
			Node node = null;
			//Java frameworks seem to have unreliable sorting. And its not that many keys to fear slowness.
			for(Node child : children)
			{
				if(child.getKey().equals(key))
				{
					node = child;
					break;
				}
			}
			
			if(node == null && createNew)
			{
				node = new Node(key);
				children.add(node);
			}
			return node;
		}
		
		public String getLastValue()
		{
			return lastValue;
		}
		
		public boolean setValueIfRequired(String key, String value)
		{
			if(field == null)
			{
				throw new RuntimeException("Received value '" + value + "' for key '" + key + "', but no setting on this one.");
			}
			
			if(visited)
			{
				throw new RuntimeException("Already written to the key " + key);
			}
			visited = true;
			
			if(lastValue.equals(value))
			{
				//No change.
				return false;
			}
			
			Class<?> type = field.getType();
			try
			{
				if(type == Float.TYPE)
				{
					field.setFloat(field, Float.parseFloat(value));
				}
				if(type == Double.TYPE)
				{
					field.setDouble(field, Double.parseDouble(value));
				}
				else if(type == Integer.TYPE)
				{
					field.setInt(field, Integer.parseInt(value));
				}
				else if(type == Boolean.TYPE)
				{
					field.setBoolean(field, Boolean.parseBoolean(value));
				}
				
				//Success, apply past.
				lastValue = value;
				
				return true;
			}
			catch(IllegalAccessException e)
			{
				throw new RuntimeException(e);
			}
			catch(NumberFormatException e)
			{
				throw new RuntimeException("Error parsing number while writing to " + key + " value: " + value);
			}
		}
		
		public void setVisit(boolean visit)
		{
			visited = visit;
		}
		
		public boolean isVisited()
		{
			return visited;
		}
	}
}

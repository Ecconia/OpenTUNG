package de.ecconia.java.opentung.settings;

import de.ecconia.Ansi;
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
	
	public SettingsIO(File file, Class<?> settingsClass)
	{
		this.file = file;

//		System.out.println("Getting fields:\n");
		for(Field field : settingsClass.getFields())
		{
			SettingInfo info = field.getAnnotation(SettingInfo.class);
			SettingsSectionStart section = field.getAnnotation(SettingsSectionStart.class);
			
			if(section != null)
			{
//				System.out.println();
//				System.out.println(Ansi.yellow + "#" + section.comment() + Ansi.r + " (" + section.key() + ")");
				Node node = root.getNode(section.key());
				node.setComment(section.comment());
			}
			if(info != null)
			{
				Node node = root.getNode(info.key());
				if(!info.comment().isEmpty())
				{
//					System.out.println(Ansi.yellow + "#" + info.comment().replace("\n", "\n#") + Ansi.r);
					node.setComment(info.comment());
				}
//				System.out.println("+" + Ansi.green + info.key() + Ansi.r + " = " + field.getName());
				node.setField(field);
			}
			else
			{
				System.out.println(Ansi.red + "WARNING: Settings variable without settings key: " + Ansi.r + field.getName());
			}
		}
		
		if(file.exists())
		{
			loadFile();
			new FileWatcher(file, () -> {
				System.out.println("Update.");
				loadFile();
			});
		}
		else
		{
			writeDefaultFile();
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
						System.out.println("Could not read all keys, since non-root keys are not supported yet!");
					}
				}
			}
			
			reader.close();
		}
		catch(IOException e)
		{
			throw new RuntimeException("Could not read settings file.", e);
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
			
//			System.out.println(debugPrefix + "In layer: " + parentKey + " Line: " + line);
			
			String content = line.replace('\t', ' ').trim();
			
			int separatorIndex = content.indexOf(':');
			if(separatorIndex < 0)
			{
				throw new RuntimeException("Malformed settings file. No ':' in line with content: " + content);
			}
			
//			System.out.println(debugPrefix + "Processing line: " + line);
			
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
			Node node = root.getNode(key);
			if(node == null)
			{
				throw new RuntimeException("Settings contain unknown key: " + key);
			}
			
			Field field = node.getField();
			if(field == null)
			{
				return;
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
			}
			catch(IllegalAccessException e)
			{
				throw new RuntimeException(e);
			}
			
			System.out.println("Applied setting: " + key + " = " + value);
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
		}
		catch(IOException e)
		{
			throw new RuntimeException("Could not save default settings file.", e);
		}
		System.out.println("Written default settings file.");
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
			this.defaultValue = valueAsString(field);
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
		
		public Node getNode(String key)
		{
			String[] keys = key.split("\\.", 2);
			Node childKey = getChildKey(keys[0]);
			if(keys.length == 1)
			{
				return childKey;
			}
			else
			{
				return childKey.getNode(keys[1]);
			}
		}
		
		private Node getChildKey(String key)
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
			
			if(node == null)
			{
				node = new Node(key);
				children.add(node);
			}
			return node;
		}
	}
}

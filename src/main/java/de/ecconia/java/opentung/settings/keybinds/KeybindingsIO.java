package de.ecconia.java.opentung.settings.keybinds;

import de.ecconia.java.opentung.settings.DataFolderWatcher;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.lwjgl.glfw.GLFW;

public class KeybindingsIO
{
	private final Map<String, KeyEntry> keys = new HashMap<>();
	private final LinkedList<KeyEntry> orderedKeys = new LinkedList<>();
	
	private final LinkedList<Object> sourceContent = new LinkedList<>(); //Used when saving changes back.
	
	public KeybindingsIO(Path path, DataFolderWatcher watcher)
	{
		processKeybindingsClass();
		
		if(Files.exists(path))
		{
			System.out.println("[Keybindings] File exists, loading...");
			loadFile(path);
		}
		else
		{
			System.out.println("[Keybindings] File does not exist, creating...");
			generateDefaultFile(path);
		}
		
		if(watcher != null)
		{
			watcher.register(path.getFileName().toString(), () -> {
				sourceContent.clear();
				try
				{
					System.out.println("[Keybindings] Reloading keybinding settings file:");
					loadFile(path);
					System.out.println("[Keybindings] Reloading successful.");
					populateKeybindings();
				}
				catch(Exception e)
				{
					e.printStackTrace(System.out);
				}
			});
		}
		
		populateKeybindings();
	}
	
	public void overwriteFile(Path path)
	{
		StringBuilder builder = new StringBuilder();
		for(Object sourceEntry : sourceContent)
		{
			builder.append(sourceEntry).append('\n');
		}
		
		try
		{
			Files.write(path, builder.toString().getBytes(StandardCharsets.UTF_8));
		}
		catch(IOException e)
		{
			System.out.println("[Keybindings] Not able to write " + path + " file. Please report this issue.");
			e.printStackTrace(System.out);
		}
	}
	
	private void loadFile(Path path)
	{
		try
		{
			List<String> lines = Files.readAllLines(path);
			for(String line : lines)
			{
				String trimmed = line.trim();
				if(trimmed.isEmpty())
				{
					sourceContent.addLast(line);
					continue;
				}
				if(trimmed.charAt(0) == '#')
				{
					sourceContent.addLast(line);
					continue;
				}
				int separatorIndex = line.indexOf(": ");
				if(separatorIndex < 0)
				{
					throw new RuntimeException("Could not load Keybindings file, non-comment line has wrong format (missing ': '): '" + line + "'.");
				}
				String key = line.substring(0, separatorIndex);
				
				KeyEntry entry = keys.get(key);
				if(entry == null)
				{
					throw new RuntimeException("Key is not used (anymore): " + key);
				}
				sourceContent.addLast(entry);
				
				String value = line.substring(separatorIndex + 2);
				
				int additionalIndex = value.indexOf(", ");
				if(additionalIndex >= 0)
				{
					//Fully ignore readable for now...
					value = value.substring(0, additionalIndex);
				}
				
				int scancode;
				{
					if(value.matches("[0-9]+"))
					{
						scancode = Integer.parseInt(value);
					}
					else
					{
						int keyIndex = GLFWKeyMapper.resolveKeyName(value);
						if(keyIndex < 0)
						{
							throw new RuntimeException("Keybindings: Cannot resolve glfw-key-name: " + value);
						}
						
						scancode = GLFW.glfwGetKeyScancode(keyIndex);
						if(scancode == -1)
						{
							throw new RuntimeException("Scancode could not be resolved!");
						}
					}
				}
				
				//Apply:
				entry.setScancode(scancode);
				entry.setKeyValue(value);
				entry.setReadable(GLFW.glfwGetKeyName(GLFW.GLFW_KEY_UNKNOWN, scancode));
			}
		}
		catch(IOException e)
		{
			throw new RuntimeException("Exception while reading key bindings file", e);
		}
	}
	
	private void generateDefaultFile(Path path)
	{
		StringBuilder builder = new StringBuilder();
		
		for(KeyEntry entry : orderedKeys)
		{
			String comment = entry.getComment();
			if(!comment.isEmpty())
			{
				for(String line : comment.split("\n", -1))
				{
					String formattedComment = line.isEmpty() ? "" : '#' + line;
					builder.append(formattedComment).append('\n');
					sourceContent.addLast(formattedComment);
				}
			}
			builder.append(entry).append('\n');
			sourceContent.addLast(entry);
		}
		
		try
		{
			Files.write(path, builder.toString().getBytes(StandardCharsets.UTF_8));
		}
		catch(IOException e)
		{
			System.out.println("[Keybindings] Not able to write " + path + " file. Please report this issue.");
			e.printStackTrace(System.out);
		}
	}
	
	private void processKeybindingsClass()
	{
		for(Field field : Keybindings.class.getFields())
		{
			KeybindingDefaults info = field.getAnnotation(KeybindingDefaults.class);
			if(info != null)
			{
				String key = info.key();
				String defaultValue = info.defaultValue();
				String comment = info.comment();
				int keyIndex = GLFWKeyMapper.resolveKeyName(defaultValue);
				if(keyIndex < 0)
				{
					throw new RuntimeException("Keybindings: Cannot resolve '" + defaultValue + "' to a key. Please check GLFW class file for its fields.");
				}
				int scancode = GLFW.glfwGetKeyScancode(keyIndex);
				if(scancode < 0)
				{
					throw new RuntimeException("Keybindings: Was not able to get scancode for key '" + defaultValue + "', please report this.");
				}
				String readable = GLFW.glfwGetKeyName(GLFW.GLFW_KEY_UNKNOWN, scancode);
				if(readable != null && readable.length() == 1 && readable.charAt(0) >= 'a' && readable.charAt(0) <= 'z')
				{
					readable = String.valueOf((char) (readable.charAt(0) - 'a' + 'A'));
				}
				KeyEntry entry = new KeyEntry(key, field, defaultValue, scancode, comment, readable);
				keys.put(key, entry);
				orderedKeys.addLast(entry);
			}
			else
			{
				throw new RuntimeException("Keybindings: Field " + field.getName() + " has no KeybindingDefaults annotation.");
			}
		}
	}
	
	private void populateKeybindings()
	{
		for(KeyEntry entry : keys.values())
		{
			Field field = entry.getField();
			try
			{
				field.setInt(field, entry.getScancode());
			}
			catch(IllegalAccessException e)
			{
				System.out.println("For field: " + field.getName());
				throw new RuntimeException(e);
			}
		}
	}
	
	public Collection<KeyEntry> getKeys()
	{
		return orderedKeys;
	}
	
	public static class KeyEntry
	{
		private final String key;
		private final Field field;
		private final String defaultValue;
		private final int defaultScancode;
		private final String comment;
		
		private String readable;
		private String keyValue;
		private int scancode;
		
		public KeyEntry(String key, Field field, String defaultValue, int defaultScancode, String comment, String readable)
		{
			this.key = key;
			this.field = field;
			this.defaultValue = defaultValue;
			this.defaultScancode = defaultScancode;
			this.comment = comment;
			
			this.readable = readable;
			this.keyValue = defaultValue;
			this.scancode = defaultScancode;
		}
		
		@Override
		public String toString()
		{
			return key + ": " + (keyValue == null ? scancode == defaultScancode ? defaultValue : scancode : keyValue) + (readable == null || readable.length() != 1 ? "" : ", " + ((readable.charAt(0) >= 'a' && readable.charAt(0) <= 'z') ? String.valueOf((char) (readable.charAt(0) - 'a' + 'A')) : readable));
		}
		
		public String getKey()
		{
			return key;
		}
		
		public Field getField()
		{
			return field;
		}
		
		public String getKeyValue()
		{
			return keyValue;
		}
		
		public int getDefaultScancode()
		{
			return defaultScancode;
		}
		
		public int getScancode()
		{
			return scancode;
		}
		
		public String getComment()
		{
			return comment;
		}
		
		public String getDefaultValue()
		{
			return defaultValue;
		}
		
		public String getReadable()
		{
			return readable;
		}
		
		public void setScancode(int scancode)
		{
			this.scancode = scancode;
		}
		
		public void setReadable(String readable)
		{
			this.readable = readable;
		}
		
		public void setKeyValue(String keyValue)
		{
			this.keyValue = keyValue;
		}
	}
}

package de.ecconia.java.opentung.libwrap;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class ShaderProgram
{
	private int id;
	private int[] uniformIDs;
	
	public ShaderProgram(String name)
	{
		//Vertex Shader:
		String vShaderCode = loadFile(name + ".vs");
		if(vShaderCode == null)
		{
			throw new RuntimeException("Could not find shader file: " + name + ".vs");
		}
		int vertexShaderID = GL30.glCreateShader(GL30.GL_VERTEX_SHADER);
		GL30.glShaderSource(vertexShaderID, vShaderCode);
		GL30.glCompileShader(vertexShaderID);
		
		if(GL30.glGetShaderi(vertexShaderID, GL30.GL_COMPILE_STATUS) == GL30.GL_FALSE)
		{
			String errorText = GL30.glGetShaderInfoLog(vertexShaderID, GL30.glGetShaderi(vertexShaderID, GL30.GL_INFO_LOG_LENGTH));
			throw new RuntimeException("Error loading Vertex shader '" + name + "': >" + errorText + "<");
		}
		
		//Fragment Shader:
		String fShaderCode = loadFile(name + ".fs");
		if(fShaderCode == null)
		{
			throw new RuntimeException("Could not find shader file: " + name + ".fs");
		}
		int fragmentShaderID = GL30.glCreateShader(GL30.GL_FRAGMENT_SHADER);
		GL30.glShaderSource(fragmentShaderID, fShaderCode);
		GL30.glCompileShader(fragmentShaderID);
		
		if(GL30.glGetShaderi(fragmentShaderID, GL30.GL_COMPILE_STATUS) == GL30.GL_FALSE)
		{
			String errorText = GL30.glGetShaderInfoLog(fragmentShaderID, GL30.glGetShaderi(fragmentShaderID, GL30.GL_INFO_LOG_LENGTH));
			throw new RuntimeException("Error loading Fragment shader '" + name + "': >" + errorText + "<");
		}
		
		//Program:
		id = GL30.glCreateProgram();
		GL30.glAttachShader(id, vertexShaderID);
		GL30.glAttachShader(id, fragmentShaderID);
		GL30.glLinkProgram(id);
		
		if(GL30.glGetProgrami(id, GL30.GL_LINK_STATUS) == GL30.GL_FALSE)
		{
			String errorText = GL30.glGetProgramInfoLog(id, GL30.glGetProgrami(id, GL30.GL_INFO_LOG_LENGTH));
			throw new RuntimeException("Error creating Program '" + name + "': >" + errorText + "<");
		}
		
		GL30.glDeleteShader(vertexShaderID);
		GL30.glDeleteShader(fragmentShaderID);
		
		//Uniform stuff:
		List<String> uniformsVertex = scanForUniforms(name, vShaderCode);
		List<String> uniformsFragment = scanForUniforms(name, fShaderCode);
		
//		System.out.println(name + ":");
		uniformIDs = new int[uniformsVertex.size() + uniformsFragment.size()];
		for(int i = 0; i < uniformsVertex.size(); i++)
		{
			uniformIDs[i] = GL30.glGetUniformLocation(id, uniformsVertex.get(i));
//			System.out.println(uniformsVertex.get(i) + ": " + uniformIDs[i]);
		}
		for(int i = 0; i < uniformsFragment.size(); i++)
		{
			uniformIDs[i + uniformsVertex.size()] = GL30.glGetUniformLocation(id, uniformsFragment.get(i));
//			System.out.println(uniformsFragment.get(i) + ": " + uniformIDs[i + uniformsVertex.size()]);
		}
	}
	
	private static List<String> scanForUniforms(String name, String source)
	{
		List<String> uniforms = new ArrayList<>();
		for(String line : source.split("\n"))
		{
			if(line.startsWith("uniform"))
			{
				String[] parts = line.split(" ");
				if(parts.length < 3)
				{
					throw new RuntimeException("Weird uniform variable declaration: " + line);
				}
				
				String variable = parts[2];
				variable = variable.substring(0, variable.indexOf(';'));
				int arrayI = variable.indexOf('[');
				if(arrayI >= 0)
				{
					variable = variable.substring(0, arrayI);
				}
				
//				System.out.println("Shader " + name + " has uniform variable: " + variable);
				uniforms.add(variable);
			}
		}
		return uniforms;
	}
	
	public void use()
	{
		GL20.glUseProgram(id);
	}
	
	public int getId()
	{
		return id;
	}
	
	public int getUniformID(int location)
	{
		return uniformIDs[location];
	}
	
	public void setUniformM4(int id, float[] matrix)
	{
		GL30.glUniformMatrix4fv(uniformIDs[id], false, matrix);
	}
	
	public void setUniformV1(int id, float value)
	{
		GL30.glUniform1f(uniformIDs[id], value);
	}
	
	public void setUniformV2(int id, float[] vec)
	{
		GL30.glUniform2fv(uniformIDs[id], vec);
	}
	
	public void setUniformV3(int id, float[] vec)
	{
		GL30.glUniform3fv(uniformIDs[id], vec);
	}
	
	public void setUniformV4(int id, float[] vec)
	{
		GL30.glUniform4fv(uniformIDs[id], vec);
	}
	
	private static String loadFile(String path)
	{
		try
		{
			InputStream stream = ShaderProgram.class.getClassLoader().getResourceAsStream("shaders/" + path);
			if(stream == null)
			{
				return null;
			}
			
			List<String> lines = new ArrayList<>();
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			String tmp;
			while((tmp = reader.readLine()) != null)
			{
				lines.add(tmp);
			}
			
			String ret = "";
			for(String line : lines)
			{
				ret += line + "\n";
			}
			return ret;
		}
		catch(IOException e)
		{
			if(e instanceof FileNotFoundException)
			{
				throw new RuntimeException("Could not find shader file: " + path);
			}
			
			e.printStackTrace(System.out);
			throw new RuntimeException("Could not load shader code: " + path);
		}
	}
	
	public void setUniformArray(int id, int[] array)
	{
		GL30.glUniform4uiv(uniformIDs[id], array);
	}
	
	public void setUniformArray(int id, float[] array)
	{
		GL30.glUniform3fv(uniformIDs[id], array);
	}
}

package de.ecconia.java.opentung.core;

import de.ecconia.java.opentung.components.meta.PlaceableInfo;
import java.io.File;
import java.util.concurrent.BlockingQueue;

public class SharedData
{
	private PlaceableInfo currentPlaceable;
	private BlockingQueue<GPUTask> gpuTasks;
	private boolean saving;
	private RenderPlane3D renderPlane3D;
	private File currentBoardFile;
	private boolean simulationLoaded;
	private ShaderStorage shaderStorage;
	private int lastFPS;
	
	private final BoardUniverse boardUniverse;
	
	public SharedData(BoardUniverse boardUniverse, File boardFile)
	{
		this.currentBoardFile = boardFile;
		this.boardUniverse = boardUniverse;
	}
	
	public BoardUniverse getBoardUniverse()
	{
		return boardUniverse;
	}
	
	public PlaceableInfo getCurrentPlaceable()
	{
		return currentPlaceable;
	}
	
	public void setCurrentPlaceable(PlaceableInfo currentPlaceable)
	{
		this.currentPlaceable = currentPlaceable;
	}
	
	public void setGPUTasks(BlockingQueue<GPUTask> gpuTasks)
	{
		this.gpuTasks = gpuTasks;
	}
	
	public BlockingQueue<GPUTask> getGpuTasks()
	{
		return gpuTasks;
	}
	
	public void setRenderPlane3D(RenderPlane3D renderPlane3D)
	{
		this.renderPlane3D = renderPlane3D;
	}
	
	public RenderPlane3D getRenderPlane3D()
	{
		return renderPlane3D;
	}
	
	public void setSaving()
	{
		saving = true;
	}
	
	public boolean isSaving()
	{
		return saving;
	}
	
	public void unsetSaving()
	{
		saving = false;
	}
	
	public File getCurrentBoardFile()
	{
		return currentBoardFile;
	}
	
	public void setCurrentBoardFile(File currentBoardFile)
	{
		this.currentBoardFile = currentBoardFile;
	}
	
	public void setSimulationLoaded(boolean simulationLoaded)
	{
		this.simulationLoaded = simulationLoaded;
	}
	
	public boolean isSimulationLoaded()
	{
		return simulationLoaded;
	}
	
	public void setShaderStorage(ShaderStorage shaderStorage)
	{
		this.shaderStorage = shaderStorage;
	}
	
	public ShaderStorage getShaderStorage()
	{
		return shaderStorage;
	}
	
	public void setFPS(int lastFPS)
	{
		this.lastFPS = lastFPS;
	}
	
	public int getFPS()
	{
		return lastFPS;
	}
}

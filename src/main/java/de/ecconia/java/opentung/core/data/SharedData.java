package de.ecconia.java.opentung.core.data;

import de.ecconia.java.opentung.components.meta.PlaceableInfo;
import de.ecconia.java.opentung.core.BoardUniverse;
import de.ecconia.java.opentung.core.RenderPlane3D;
import de.ecconia.java.opentung.core.structs.GPUTask;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;

public class SharedData
{
	private PlaceableInfo currentPlaceable;
	private BlockingQueue<GPUTask> gpuTasks;
	private boolean saving;
	private RenderPlane3D renderPlane3D;
	private Path currentBoardFile;
	private boolean simulationLoaded;
	private ShaderStorage shaderStorage;
	private int lastFPS;
	
	private final BoardUniverse boardUniverse;
	
	public SharedData(BoardUniverse boardUniverse, Path boardFile)
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
	
	public Path getCurrentBoardFile()
	{
		return currentBoardFile;
	}
	
	public void setCurrentBoardFile(Path currentBoardFile)
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

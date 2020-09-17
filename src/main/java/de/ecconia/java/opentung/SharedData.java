package de.ecconia.java.opentung;

import java.util.concurrent.BlockingQueue;

public class SharedData
{
	private PlaceableInfo currentPlaceable;
	private BlockingQueue<GPUTask> gpuTasks;
	private boolean saving;
	private RenderPlane3D renderPlane3D;
	
	private final BoardUniverse boardUniverse;
	
	public SharedData(BoardUniverse boardUniverse)
	{
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
}

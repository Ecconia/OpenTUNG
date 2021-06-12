package de.ecconia.java.opentung.savefile;

import de.ecconia.java.opentung.OpenTUNG;
import de.ecconia.java.opentung.core.RenderPlane3D;
import de.ecconia.java.opentung.core.data.SharedData;
import de.ecconia.java.opentung.interfaces.windows.PauseMenu;
import de.ecconia.java.opentung.simulation.SimulationManager;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class SavePrepareUnit
{
	private SharedData sharedData;
	private PauseMenu pauseMenu;
	
	public SavePrepareUnit(PauseMenu pauseMenu, SharedData sharedData, boolean chooser)
	{
		//The input thread shall be in charge of setting this flag. That way fast-clicking on save will not be possible.
		if(sharedData.isSaving())
		{
			return;
		}
		sharedData.setSaving();
		
		this.sharedData = sharedData;
		this.pauseMenu = pauseMenu;
		
		//Now switch to another thread, cause we no longer want to disturb/stop the main-window's input thread.
		// Non-Daemon (Can be killed):
		Thread t = new Thread(() -> {
			{
				Path currentSavePath = sharedData.getCurrentBoardFile();
				if(chooser)
				{
					if(currentSavePath == null)
					{
						currentSavePath = OpenTUNG.boardFolder;
					}
					else
					{
						currentSavePath = currentSavePath.getParent();
					}
					JFileChooser fileChooser = new JFileChooser(currentSavePath.toFile());
					int result = fileChooser.showSaveDialog(null);
					if(result != JFileChooser.APPROVE_OPTION)
					{
						endSaving();
						return;
					}
					currentSavePath = fileChooser.getSelectedFile().toPath();
					String fileName = currentSavePath.getFileName().toString();
					int endingIndex = fileName.lastIndexOf('.');
					if(endingIndex < 0)
					{
						currentSavePath = currentSavePath.resolveSibling(fileName + ".opentung");
					}
					else
					{
						String ending = fileName.substring(endingIndex + 1);
						if(!ending.equals("opentung"))
						{
							JOptionPane.showMessageDialog(null, "File-ending must be '.opentung', change or leave blank.", "Can only save .opentung files.", JOptionPane.ERROR_MESSAGE, null);
							endSaving();
							return;
						}
					}
				}
				else
				{
					//Button disabled if currentSavePath is null.
					String fileName = currentSavePath.getFileName().toString();
					int endingIndex = fileName.lastIndexOf('.');
					if(fileName.substring(endingIndex + 1).equals("tungboard")) //Assumes file has always ending.
					{
						int result = JOptionPane.showOptionDialog(null, "You loaded a .tungboard file, save as .opentung file?", "Save as OpenTUNG-Save?", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
						if(result != JOptionPane.OK_OPTION)
						{
							endSaving();
							return;
						}
						currentSavePath = currentSavePath.getParent().resolve(fileName.substring(0, endingIndex + 1) + "opentung");
						//TODO: Add check that the file does not exist...
					}
				}
				sharedData.setCurrentBoardFile(currentSavePath);
			}
			
			//Lock
			SimulationManager simulationManager = sharedData.getBoardUniverse().getSimulation();
			RenderPlane3D renderPlane3D = sharedData.getRenderPlane3D();
			{
				AtomicInteger pauseArrived = new AtomicInteger();
				//TBI: May skip the execution of some simulation tasks with external source, problem?
				simulationManager.pauseSimulation(pauseArrived);
				//Following task is appended to the end of the task-queue and will allow saving.
				//TBI: Assumes that the interface is open and thus no new GPU tasks had been added.
				sharedData.getGpuTasks().add((render) -> {
					renderPlane3D.prepareSaving();
					pauseArrived.incrementAndGet();
				});
				while(pauseArrived.get() != 2)
				{
					try
					{
						Thread.sleep(10);
					}
					catch(InterruptedException e)
					{
						e.printStackTrace();
					}
				}
			}
			pauseMenu.setSaveButtonsDisabled(true);
			
			//Start daemon thread (Cannot be killed):
			Thread saveThread = new Thread(() -> {
				System.out.println("Saving...");
				long startTime = System.currentTimeMillis();
				
				Saver.save(sharedData.getBoardUniverse(), sharedData.getCurrentBoardFile());
				
				System.out.println("Done, took: " + (System.currentTimeMillis() - startTime) + "ms");
				
				//Unlock:
				simulationManager.resumeSimulation();
				endSaving();
			}, "SaveThread");
			saveThread.setDaemon(false); //Yes it should finish saving first! Thus no daemon.
			saveThread.start();
		}, "Save-Preparation-Thread");
		t.start();
	}
	
	private void endSaving()
	{
		sharedData.unsetSaving();
		pauseMenu.setSaveButtonsDisabled(false);
	}
}

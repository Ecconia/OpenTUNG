package de.ecconia.java.opentung.savefile;

import de.ecconia.java.opentung.OpenTUNG;
import de.ecconia.java.opentung.core.RenderPlane3D;
import de.ecconia.java.opentung.core.data.SharedData;
import de.ecconia.java.opentung.core.structs.GPUTask;
import de.ecconia.java.opentung.interfaces.windows.PauseMenu;
import de.ecconia.java.opentung.simulation.SimulationManager;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class SavePrepareUnit
{
	private final BlockingQueue<Callable> backNForthQueue = new LinkedBlockingQueue<>();
	
	private SharedData sharedData;
	private PauseMenu pauseMenu;
	
	private boolean simulationDone;
	private boolean renderDone;
	
	public SavePrepareUnit(PauseMenu pauseMenu, SharedData sharedData, boolean chooser)
	{
		//The input thread shall be in charge of setting this flag. That way fast-clicking on save will not be possible.
		if(sharedData.isSaving())
		{
			return;
		}
		sharedData.setSaving();
		pauseMenu.setSaveButtonsDisabled(true);
		
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
				System.out.println("[PreSave] Waiting for simulation/render jobs to be processed and simulation to be halted.");
				simulationManager.lockSimulation();
				if(simulationManager.isAlive())
				{
					simulationManager.updateJobNextTickThreadSafe(new SimulationFinalJob());
				}
				else
				{
					System.out.println("[PreSave] Simulation thread crashed, saving anyway...");
					simulationDone = true;
				}
				
				sharedData.getGpuTasks().add((render) -> {
					renderPlane3D.prepareSaving(); //Stops all modes
				});
				
				sharedData.getGpuTasks().add(new GPUFinalJob());
				try
				{
					while(!(simulationDone && renderDone))
					{
						backNForthQueue.take().call();
					}
				}
				catch(InterruptedException e)
				{
					//Does not expect to be interrupted.
					e.printStackTrace();
					endSaving();
					return; //Something interfered, for safety reasons lets not continue here.
				}
			}
			
			//Start daemon thread (Cannot be killed):
			Thread saveThread = new Thread(() -> {
				System.out.println("Saving...");
				long startTime = System.currentTimeMillis();
				Saver.save(sharedData.getBoardUniverse(), sharedData.getCurrentBoardFile());
				System.out.println("Done, took: " + (System.currentTimeMillis() - startTime) + "ms");
				//Unlock:
				simulationManager.unlockSimulation();
				endSaving();
			}, "SaveThread");
			saveThread.setDaemon(false); //Yes it should finish saving first! Thus no daemon.
			saveThread.start();
		}, "Save-Preparation-Thread");
		t.start();
	}
	
	private class GPUFinalJob implements GPUTask
	{
		private int idleCounter = 0;
		
		@Override
		public void execute(RenderPlane3D renderPlane3D)
		{
			int currentJobAmount = renderPlane3D.getGpuTasksCurrentSize();
			if(currentJobAmount > 1)
			{
				idleCounter = 0;
				reschedule();
			}
			else
			{
				if(idleCounter++ >= 6)
				{
					backNForthQueue.add(() -> {
						//Trigger the queue, since that will cause the flag below to be checked.
						System.out.println("[PreSave] Render jobs done.");
						renderDone = true;
					});
				}
				else
				{
					//Try again next cycle:
					reschedule();
				}
			}
		}
		
		private void reschedule()
		{
			backNForthQueue.add(() -> {
				try
				{
					Thread.sleep(50);
				}
				catch(InterruptedException e)
				{
					e.printStackTrace();
				}
				sharedData.getGpuTasks().add(this);
			});
		}
	}
	
	private class SimulationFinalJob implements SimulationManager.UpdateJob
	{
		private int idleCounter = 0;
		
		@Override
		public void update(SimulationManager simulation)
		{
			//Wait for the simulation to
			if(!simulation.isSimulationHalted())
			{
				if(idleCounter != 0)
				{
					System.out.println("[PreSave] WARNING: While preparing saving and stopping simulation, the simulation after locked continued to run again! This should never be the case. Counter was: " + idleCounter);
					idleCounter = 0;
				}
				simulation.updateJobNextTickThreadSafe(this);
				return;
			}
			
			//Wait until the simulation job queue was empty for like 4-6 "cycles".
			int simulationJobs = simulation.getCurrentJobQueueSize();
			if(simulationJobs > 1)
			{
				//Try again next cycle:
				idleCounter = 0;
				simulation.updateJobNextTickThreadSafe(this);
			}
			else
			{
				if(idleCounter++ >= 6)
				{
					backNForthQueue.add(() -> {
						//Trigger the queue, since that will cause the flag below to be checked.
						System.out.println("[PreSave] Simulation jobs done.");
						simulationDone = true;
					});
				}
				else
				{
					//Try again next cycle:
					simulation.updateJobNextTickThreadSafe(this);
				}
			}
		}
	}
	
	private interface Callable
	{
		void call();
	}
	
	private void endSaving()
	{
		sharedData.unsetSaving();
		pauseMenu.setSaveButtonsDisabled(false);
	}
}

package de.ecconia.java.opentung.core.tools;

import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.CompLabel;
import de.ecconia.java.opentung.components.conductor.Connector;
import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.Meshable;
import de.ecconia.java.opentung.components.meta.Colorable;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ConnectedComponent;
import de.ecconia.java.opentung.components.meta.CustomColor;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.components.meta.Part;
import de.ecconia.java.opentung.core.data.Hitpoint;
import de.ecconia.java.opentung.core.data.ShaderStorage;
import de.ecconia.java.opentung.core.data.SharedData;
import de.ecconia.java.opentung.core.helper.World3DHelper;
import de.ecconia.java.opentung.interfaces.RenderPlane2D;
import de.ecconia.java.opentung.interfaces.windows.ColorSwitcher;
import de.ecconia.java.opentung.interfaces.windows.LabelEditor;
import de.ecconia.java.opentung.libwrap.Matrix;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;
import de.ecconia.java.opentung.settings.keybinds.Keybindings;
import de.ecconia.java.opentung.util.math.Quaternion;
import de.ecconia.java.opentung.util.math.Vector3;

public class EditWindow implements Tool
{
	private final SharedData sharedData;
	
	private Component component;
	
	ColorSwitcher colorSwitcher;
	LabelEditor labelEditor;
	
	public EditWindow(SharedData sharedData)
	{
		this.sharedData = sharedData;
		
		RenderPlane2D interfaceRenderer = sharedData.getRenderPlane2D();
		colorSwitcher = new ColorSwitcher(interfaceRenderer);
		labelEditor = new LabelEditor(interfaceRenderer);
		interfaceRenderer.addWindow(colorSwitcher);
		interfaceRenderer.addWindow(labelEditor);
	}
	
	@Override
	public Boolean activateKeyUp(Hitpoint hitpoint, int scancode, boolean control)
	{
		if(scancode == Keybindings.KeyEditComponent && !hitpoint.isEmpty())
		{
			Part hitPart = hitpoint.getHitPart();
			if(hitPart instanceof Connector)
			{
				hitPart = hitPart.getParent();
			}
			if(hitPart instanceof CustomColor)
			{
				this.component = (Component) hitPart;
				return true; //Causes activateNow() to be called.
			}
			if(hitPart instanceof CompLabel)
			{
				this.component = (Component) hitPart;
				return true;
			}
			return false;
		}
		return null;
	}
	
	@Override
	public void activateNow(Hitpoint hitpoint)
	{
		if(component instanceof CustomColor)
		{
			sharedData.getGpuTasks().add((worldRenderer) -> {
				worldRenderer.getWorldMesh().removeComponent(component, sharedData.getBoardUniverse().getSimulation());
				colorSwitcher.activate(this, (CustomColor) component);
				worldRenderer.toolReady();
			});
		}
		else //Label
		{
			sharedData.getGpuTasks().add((worldRenderer) -> {
				labelEditor.activate(this, (CompLabel) component);
				worldRenderer.toolReady();
			});
		}
	}
	
	public void guiClosed()
	{
		sharedData.getRenderPlane3D().toolStopInputs();
		sharedData.getGpuTasks().add((worldRenderer) -> {
			if(component instanceof CustomColor)
			{
				worldRenderer.getWorldMesh().addComponent(component, sharedData.getBoardUniverse().getSimulation());
			}
			worldRenderer.toolDisable();
		});
	}
	
	@Override
	public void renderWorld(float[] view)
	{
		if(component instanceof CompLabel)
		{
			return; //Do nothing, only the LabelEditor window does things.
		}
		
		ShaderStorage shaderStorage = sharedData.getShaderStorage();
		//Render the removed component:
		if(component instanceof CompBoard)
		{
			CompBoard board = (CompBoard) component;
			
			//TBI: Ehh skip the model? (For now yes, the component is very defined in TUNG and LW).
			Matrix matrix = new Matrix();
			//Apply global position:
			Vector3 position = board.getPositionGlobal();
			matrix.translate((float) position.getX(), (float) position.getY(), (float) position.getZ());
			Quaternion newAlignment = board.getAlignmentGlobal();
			matrix.multiply(new Matrix(newAlignment.createMatrix())); //Apply global rotation.
			//The cube is centered, no translation.
			matrix.scale((float) board.getX() * 0.15f, 0.075f, (float) board.getZ() * 0.15f); //Just use the right size from the start... At this point in code it always has that size.
			
			//Draw the board:
			shaderStorage.getBoardTexture().activate();
			ShaderProgram textureCubeShader = shaderStorage.getTextureCubeShader();
			textureCubeShader.use();
			textureCubeShader.setUniformM4(1, view);
			textureCubeShader.setUniformM4(2, matrix.getMat());
			textureCubeShader.setUniformV2(3, new float[]{board.getX(), board.getZ()});
			textureCubeShader.setUniformV4(4, board.getColor().asArray());
			GenericVAO textureCube = shaderStorage.getVisibleOpTexCube();
			textureCube.use();
			textureCube.draw();
		}
		else
		{
			ShaderProgram visibleCubeShader = shaderStorage.getVisibleCubeShader();
			GenericVAO visibleCube = shaderStorage.getVisibleOpTexCube();
			ModelHolder model = component.getModelHolder();
			Vector3 position = component.getPositionGlobal();
			Quaternion alignment = component.getAlignmentGlobal();
			
			visibleCubeShader.use();
			visibleCubeShader.setUniformM4(1, view);
			Matrix matrix = new Matrix();
			Matrix rotation = new Matrix(alignment.createMatrix());
			Vector3 placementOffset = model.getPlacementOffset();
			for(int i = 0; i < model.getColorables().size(); i++)
			{
				Meshable meshable = model.getColorables().get(i);
				visibleCubeShader.setUniformV4(3, ((Colorable) component).getCurrentColor(i).asArray());
				World3DHelper.drawCubeFull(visibleCubeShader, visibleCube, (CubeFull) meshable, position, rotation, placementOffset, matrix);
			}
			for(Meshable meshable : model.getSolid())
			{
				visibleCubeShader.setUniformV4(3, ((CubeFull) meshable).getColorArray());
				World3DHelper.drawCubeFull(visibleCubeShader, visibleCube, (CubeFull) meshable, position, rotation, placementOffset, matrix);
			}
			if(component instanceof ConnectedComponent)
			{
				for(Connector connector : ((ConnectedComponent) component).getConnectors())
				{
					visibleCubeShader.setUniformV4(3, connector.getCluster().isActive() ? Color.circuitON.asArray() : Color.circuitOFF.asArray());
					World3DHelper.drawCubeFull(visibleCubeShader, visibleCube, connector.getModel(), position, rotation, placementOffset, matrix);
				}
			}
		}
	}
}

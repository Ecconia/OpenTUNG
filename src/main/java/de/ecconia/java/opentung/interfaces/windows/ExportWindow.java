package de.ecconia.java.opentung.interfaces.windows;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Path;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;

import de.ecconia.java.opentung.OpenTUNG;
import de.ecconia.java.opentung.core.data.ShaderStorage;
import de.ecconia.java.opentung.core.tools.grabbing.Grabbing;
import de.ecconia.java.opentung.interfaces.GUIColors;
import de.ecconia.java.opentung.interfaces.MeshText;
import de.ecconia.java.opentung.interfaces.RenderPlane2D;
import de.ecconia.java.opentung.interfaces.Shapes;
import de.ecconia.java.opentung.interfaces.Window;
import de.ecconia.java.opentung.libwrap.FloatShortArraysInt;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;
import de.ecconia.java.opentung.settings.Settings;
import org.lwjgl.opengl.GL30;

public class ExportWindow extends Window
{
	private static final String[] labelTextInstruction = {
			"Please use the external window to choose a file to export the board to.",
			"Click inside of OpenTUNG or close the external window to abort."
	};
	
	private final GenericVAO[] textMesh = new GenericVAO[labelTextInstruction.length];
	private final int[] textWidth = new int[labelTextInstruction.length];
	
	private final RenderPlane2D interfaceRenderer;
	private final Grabbing grabbing;
	
	private final float windowWidth;
	private final float windowHeight;
	
	private JFrame frame;
	private Path chosenPath;
	
	public ExportWindow(Grabbing grabbing, RenderPlane2D interfaceRenderer)
	{
		this.grabbing = grabbing;
		this.interfaceRenderer = interfaceRenderer;
		
		windowWidth = 830;
		windowHeight = 80;
		
		MeshText text = interfaceRenderer.getText();
		text.addLetters(labelTextInstruction[0]);
		text.addLetters(labelTextInstruction[1]);
	}
	
	public void activate()
	{
		interfaceRenderer.getInputHandler().switchTo2D(); //TBI: Should be more generic?
		isVisible = true;
		
		frame = new JFrame("Choose file to export to");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosed(WindowEvent e)
			{
				//TODO: Warning: Interaction with input thread methods, but this is not the input thread.
				// As in, might explode. But minor issues. Lets ignore this for now and hope for the best.
				if(frame != null)
				{
					close();
					interfaceRenderer.getInputHandler().switchTo3D();
				}
			}
		});
		
		//Content:
		
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(OpenTUNG.bootstrap.getBoardFolder().toFile());
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setApproveButtonText("Export");
		chooser.addActionListener(e -> {
			File file = chooser.getSelectedFile();
			if(file != null)
			{
				chosenPath = file.toPath();
			}
			close();
			interfaceRenderer.getInputHandler().switchTo3D();
		});
		chooser.setFileFilter(new FileFilter()
		{
			@Override
			public boolean accept(File f)
			{
				return f.isDirectory() || f.getName().toLowerCase().endsWith(".opentung");
			}
			
			@Override
			public String getDescription()
			{
				return "OpenTUNG boards (*.opentung)";
			}
		});
		frame.add(chooser);
		
		//Finally:
		
		frame.pack();
		{
			//Set location:
			Dimension size = frame.getSize();
			int[] bounds = interfaceRenderer.getSharedData().getWindow().getWindowBounds(); //Not thread-safe, but it does not matter if these values change.
			int x = bounds[0];
			int y = bounds[1];
			int w = bounds[2];
			int h = bounds[3];
			
			x += (w - size.width) / 2;
			if(h > size.height) //Never let the window spawn above the OpenTUNG frame.
			{
				y += (h - size.height) / 2;
			}
			frame.setLocation(x, y);
		}
		frame.setVisible(true);
	}
	
	@Override
	public void close()
	{
		super.close();
		
		if(frame != null)
		{
			JFrame copy = frame;
			frame = null;
			copy.dispose();
		}
		
		grabbing.guiExportClosed(chosenPath);
		chosenPath = null;
	}
	
	@Override
	public void setup()
	{
		MeshText fontUnit = interfaceRenderer.getText();
		for(int i = 0; i < labelTextInstruction.length; i++)
		{
			FloatShortArraysInt r = fontUnit.fillArray(labelTextInstruction[i], 50);
			textMesh[i] = new GenericVAO(r.getFloats(), r.getShorts())
			{
				@Override
				protected void init()
				{
					//Position:
					GL30.glVertexAttribPointer(0, 2, GL30.GL_FLOAT, false, 4 * Float.BYTES, 0);
					GL30.glEnableVertexAttribArray(0);
					//TextureCoord:
					GL30.glVertexAttribPointer(1, 2, GL30.GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
					GL30.glEnableVertexAttribArray(1);
				}
			};
			textWidth[i] = r.getInteger();
		}
	}
	
	private float middleX;
	private float middleY;
	
	@Override
	public void renderFrame()
	{
		float scale = Settings.guiScale;
		long nvg = interfaceRenderer.vg;
		
		middleX = interfaceRenderer.realWidth(scale) / 2f;
		middleY = interfaceRenderer.realHeight(scale) / 2f;
		Shapes.drawBox(nvg, middleX, middleY, windowWidth, windowHeight, GUIColors.background, GUIColors.outline);
	}
	
	@Override
	public void renderDecor(ShaderStorage shaderStorage)
	{
		float scale = Settings.guiScale;
		
		MeshText fontUnit = interfaceRenderer.getText();
		fontUnit.activate();
		ShaderProgram textShader = shaderStorage.getFlatTextShader();
		textShader.use();
		textShader.setUniformV3(2, new float[]{0, 0, 0}); //Color
		
		int y = -15;
		for(int i = 0; i < labelTextInstruction.length; i++)
		{
			textShader.setUniformV3(1, new float[]{
					(middleX - textWidth[i] / 2f) * scale,
					(middleY + y) * scale,
					scale
			});
			textMesh[i].use();
			textMesh[i].draw();
			y += 30;
		}
	}
}

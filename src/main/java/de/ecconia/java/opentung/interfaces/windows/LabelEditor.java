package de.ecconia.java.opentung.interfaces.windows;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import de.ecconia.java.opentung.components.CompLabel;
import de.ecconia.java.opentung.core.data.ShaderStorage;
import de.ecconia.java.opentung.core.tools.EditWindow;
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

public class LabelEditor extends Window
{
	private static final String[] labelTextInstruction = {
			"Please use external editor to change the label text.",
			"Use Control+Enter or ESC to close the external window.",
			"You can also close this internal window to stop editing."
	};
	
	private final RenderPlane2D interfaceRenderer;
	
	private final float windowWidth;
	private final float windowHeight;
	
	private EditWindow editWindow;
	private CompLabel component;
	
	public LabelEditor(RenderPlane2D interfaceRenderer)
	{
		this.interfaceRenderer = interfaceRenderer;
		
		windowWidth = 700;
		windowHeight = 100;
		
		MeshText text = interfaceRenderer.getText();
		text.addLetters(labelTextInstruction[0]);
		text.addLetters(labelTextInstruction[1]);
		text.addLetters(labelTextInstruction[2]);
	}
	
	public void activate(EditWindow editWindow, CompLabel component)
	{
		interfaceRenderer.getInputHandler().switchTo2D(); //TBI: Should be more generic?
		setComponent(editWindow, component);
		isVisible = true;
	}
	
	private JFrame frame;
	private String newText;
	private float newFontSize;
	
	private void setComponent(EditWindow editWindow, CompLabel component)
	{
		this.editWindow = editWindow;
		this.component = component;
		
		newText = component.getText();
		newFontSize = component.getFontSize();
		
		//open() - Start the external window:
		
		frame = new JFrame("OpenTUNG: Edit label");
		frame.setResizable(false);
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
		
		//Font Magic:
		
		JTextArea area = new JTextArea(newText);
		JSlider slider = new JSlider();
		slider.setMinimum(1);
		slider.setMaximum(500);
		slider.setValue((int) (newFontSize * 100f));
		JTextField field = new JTextField();
		
		DocumentListener listener = new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				update();
			}
			
			@Override
			public void removeUpdate(DocumentEvent e)
			{
				update();
			}
			
			@Override
			public void changedUpdate(DocumentEvent e)
			{
				update();
			}
			
			private void update()
			{
				newText = area.getText();
				try
				{
					newFontSize = Float.parseFloat(field.getText());
					
					float originalFontSize = (newFontSize / 11f) * 400.0f * 1.28f; //The last factor is to "make it work", idk why it differs this much. The font size probably does not scale the same.
					area.setFont(new Font(Font.DIALOG, Font.BOLD, (int) (originalFontSize)));
					
					field.setForeground(Color.black);
				}
				catch(Exception e)
				{
					field.setForeground(Color.red);
				}
			}
		};
		KeyAdapter controlListener = new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if(e.getKeyCode() == KeyEvent.VK_ENTER && e.isControlDown())
				{
					frame.dispose();
				}
			}
		};
		
		area.setMinimumSize(new Dimension(400, 400));
		area.setPreferredSize(new Dimension(400, 400));
		area.setFont(new Font(Font.DIALOG, Font.BOLD, 60));
		area.getDocument().addDocumentListener(listener);
		area.addKeyListener(controlListener);
		frame.getContentPane().add(area);
		
		JPanel south = new JPanel();
		south.setLayout(new BorderLayout());
		frame.getContentPane().add(south, BorderLayout.SOUTH);
		
		slider.setMinimumSize(new Dimension(400, 40));
		slider.setPreferredSize(new Dimension(400, 40));
		slider.setFocusable(false);
		slider.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent e)
			{
				field.setText(String.valueOf(slider.getValue() * 0.01f));
			}
		});
		south.add(slider);
		field.setFont(new Font(Font.DIALOG, Font.BOLD, 30));
		field.setMinimumSize(new Dimension(100, 40));
		field.setPreferredSize(new Dimension(100, 40));
		field.getDocument().addDocumentListener(listener);
		field.setText(String.valueOf(newFontSize));
		field.addKeyListener(controlListener);
		south.add(field, BorderLayout.EAST);
		
		//END
		
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
		
		final CompLabel component = this.component;
		if(component.getFontSize() != newFontSize || !component.getText().equals(newText))
		{
			//Update component:
			interfaceRenderer.getSharedData().getGpuTasks().add((world3D -> {
				boolean hadNoText = !component.hasText();
				component.setText(newText);
				component.setFontSize(newFontSize);
				
				if(newText.isEmpty())
				{
					world3D.getSharedData().getBoardUniverse().getLabelsToRender().remove(component); //No longer render this component.
					component.unload(); //If it had a texture, unload it (if allowed/possible).
				}
				else
				{
					world3D.getLabelToolkit().processSingleLabel(world3D.getSharedData().getBoardUniverse().getRootBoard(), world3D.getSharedData().getGpuTasks(), component); //Sets the loading logo and later the rendered text.
					if(hadNoText)
					{
						world3D.getSharedData().getBoardUniverse().getLabelsToRender().add(component);
					}
				}
			}));
		}
		if(frame != null)
		{
			JFrame copy = frame;
			frame = null;
			copy.dispose();
		}
		this.component = null;
		editWindow.guiClosed();
	}
	
	private final GenericVAO[] textMesh = new GenericVAO[3];
	private final int[] textWidth = new int[3];
	
	@Override
	public void setup()
	{
		MeshText fontUnit = interfaceRenderer.getText();
		float y = windowHeight / 2f - 50 / 2f - 18;
		for(int i = 0; i < 3; i++)
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
			y -= 60;
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
		
		int y = -30;
		for(int i = 0; i < 3; i++)
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

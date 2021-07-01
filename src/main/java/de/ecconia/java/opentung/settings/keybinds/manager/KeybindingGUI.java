package de.ecconia.java.opentung.settings.keybinds.manager;

import de.ecconia.java.opentung.settings.keybinds.KeybindingsIO;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

//DISCLAIMER: At some point, I stopped cleaning up this class. Means validation and repaint calls are redundant. However they do their job now.
public class KeybindingGUI extends JFrame
{
	private final KeybindingManager manager;
	private final MyBooleanPrinter booleanPrinter;
	
	private final Font globalFont = new Font(Font.DIALOG, Font.PLAIN, 16);
	
	private final JPanel entryPanel;
	private final ScrollSection scroller;
	
	private KeyBindingEntry activatedKeyBinding;
	
	private int lastHeight = 0;
	
	public KeybindingGUI(KeybindingManager manager, Collection<KeybindingsIO.KeyEntry> keys)
	{
		this.manager = manager;
		
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setTitle("OpenTUNG - Keybinding manager");
		
		JPanel content = new JPanel();
		setContentPane(content);
		content.setFocusable(false);
		content.setBorder(new EmptyBorder(3, 3, 3, 3));
		content.setBackground(Color.gray);
		content.setLayout(new VerticalListLayout(4, false));
		
		{
			JPanel panel = new JPanel();
			panel.setBackground(null);
			content.add(panel);
			panel.setLayout(new HorizontalListLayout(2, true));
			panel.add(new Label("Click the function to set its key."));
		}
		{
			JPanel panel = new JPanel();
			panel.setBackground(null);
			panel.setLayout(new HorizontalListLayout(2, true));
			panel.add(new Label("Input-Capture-Window is: "));
			booleanPrinter = new MyBooleanPrinter("active", "inactive");
			panel.add(booleanPrinter);
			content.add(panel);
		}
		entryPanel = new JPanel();
		entryPanel.setBackground(Color.gray);
		entryPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		entryPanel.setLayout(new VerticalListLayout(4, false));
		for(KeybindingsIO.KeyEntry entry : keys)
		{
			entryPanel.add(new KeyBindingEntry(entry));
		}
		scroller = new ScrollSection(entryPanel);
		scroller.setBackground(null);
		scroller.setMinimumSize(new Dimension(entryPanel.getMinimumSize().width, 500));
		scroller.setPreferredSize(new Dimension(entryPanel.getMinimumSize().width, 500));
		content.add(scroller);
		
		content.addComponentListener(new ComponentListener()
		{
			@Override
			public void componentResized(ComponentEvent e)
			{
				getContentPane().revalidate();
				int otherHeight = getContentPane().getHeight() - lastHeight;
				scroller.setPreferredSize(new Dimension(entryPanel.getMinimumSize().width, scroller.getHeight() + otherHeight));
				scroller.revalidate();
				lastHeight = getContentPane().getHeight();
			}
			
			@Override
			public void componentMoved(ComponentEvent e)
			{
			}
			
			@Override
			public void componentShown(ComponentEvent e)
			{
			}
			
			@Override
			public void componentHidden(ComponentEvent e)
			{
			}
		});
		setFocusable(true);
		addKeyListener(new KeyListener()
		{
			@Override
			public void keyTyped(KeyEvent e)
			{
			}
			
			@Override
			public void keyPressed(KeyEvent e)
			{
				if(e.getKeyCode() == KeyEvent.VK_ESCAPE)
				{
					System.out.println("Escape pressed, quitting.");
					dispose(); //Close this window.
					manager.closeGrabber();
				}
			}
			
			@Override
			public void keyReleased(KeyEvent e)
			{
			}
		});
		
		pack();
		lastHeight = getContentPane().getHeight();
		setLocationRelativeTo(null);
		setVisible(true);
	}
	
	public void redrawWholeDamnWindow()
	{
		int widthNeeded = entryPanel.getMinimumSize().width + 6; //6 for insets/border
		if(getWidth() != widthNeeded)
		{
			System.out.println("Readjust window size.");
			scroller.setMinimumSize(new Dimension(widthNeeded, Integer.MAX_VALUE));
			setSize(widthNeeded, getHeight());
		}
		revalidate();
		scroller.revalidate();
		scroller.getVerticalScrollBar().revalidate();
	}
	
	public void inputFocused(boolean focused)
	{
		booleanPrinter.setState(focused);
	}
	
	public void keyboardInput(int scancode, String glfwName, String osCharacter)
	{
		osCharacter = KeybindingsIO.fixReadable(osCharacter);
		System.out.println("Received input: " + scancode + " " + glfwName + " " + osCharacter);
		if(activatedKeyBinding != null)
		{
			KeybindingsIO.KeyEntry entry = activatedKeyBinding.getEntry();
			entry.setScancode(scancode);
			entry.setKeyValue(glfwName);
			entry.setReadable(osCharacter);
			activatedKeyBinding.updateValues();
			manager.saveData();
		}
		requestFocus();
	}
	
	private class KeyBindingEntry extends JPanel
	{
		private final Color activeColor = new Color(100, 100, 255);
		private final Color inactiveColor = new Color(150, 150, 150);
		private final Color normalForeground = getForeground();
		private final KeybindingsIO.KeyEntry entry;
		
		private final MyTextHighlightField scancodeField;
		private final MyTextHighlightField glfwNameField;
		private final MyTextHighlightField readableField;
		
		public KeyBindingEntry(KeybindingsIO.KeyEntry entry)
		{
			this.entry = entry;
			
			setBackground(inactiveColor);
			setBorder(new LineBorder(Color.black, 4));
			setLayout(new VerticalListLayout(2, false)); //Anything just not BorderLayouts...
			
			MyContainer contentWrapper = new MyContainer();
			contentWrapper.setBorder(new EmptyBorder(3, 5, 3, 5));
			contentWrapper.setBackground(null);
			contentWrapper.setLayout(new VerticalListLayout(2, false));
			add(contentWrapper);
			
			{
				JPanel panel = new JPanel();
				panel.setBackground(null);
				contentWrapper.add(panel);
				panel.setLayout(new HorizontalListLayout(2, true));
				panel.add(new Label(entry.getKey() + ": "));
			}
			
			MyContainer nextLine = new MyContainer();
			nextLine.setBackground(null);
			nextLine.setLayout(new HorizontalListLayout(0, false));
			contentWrapper.add(nextLine);
			
			nextLine.add(new Label("Code: "));
			String scancode = String.valueOf(entry.getScancode());
			scancodeField = new MyTextHighlightField(scancode);
			nextLine.add(scancodeField);
			
			nextLine.add(new Label(" US-Layout KeyName: "));
			glfwNameField = new MyTextHighlightField(entry.getKeyValue());
			nextLine.add(glfwNameField);
			
			nextLine.add(new Label(" Typed letter: "));
			String val = entry.getReadable() == null ? "<?>" : entry.getReadable();
			readableField = new MyTextHighlightField(val);
			nextLine.add(readableField);
			
			addMouseListener(new MouseListener()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
				}
				
				@Override
				public void mousePressed(MouseEvent e)
				{
				}
				
				@Override
				public void mouseReleased(MouseEvent e)
				{
					if(activatedKeyBinding != null)
					{
						activatedKeyBinding.setActivated(false);
					}
					activatedKeyBinding = KeyBindingEntry.this;
					setActivated(true);
					manager.inputFocus();
				}
				
				@Override
				public void mouseEntered(MouseEvent e)
				{
				}
				
				@Override
				public void mouseExited(MouseEvent e)
				{
				}
			});
		}
		
		public KeybindingsIO.KeyEntry getEntry()
		{
			return entry;
		}
		
		public void setActivated(boolean activated)
		{
			setBackground(activated ? activeColor : inactiveColor);
			setForeground(activated ? Color.black : normalForeground);
			revalidate();
			redrawWholeDamnWindow();
		}
		
		public void updateValues()
		{
			scancodeField.setText(String.valueOf(entry.getScancode()));
			
			String val = entry.getKeyValue();
			if(val == null)
			{
				val = "<?>";
			}
			glfwNameField.setText(val);
			
			val = entry.getReadable() == null ? "<?>" : entry.getReadable();
			readableField.setText(val);
			
			invalidate();
			validate();
			redrawWholeDamnWindow();
			repaint();
		}
	}
	
	private static class MyContainer extends JComponent
	{
		//Just a non-background coloring container.
	}
	
	private class MyBooleanPrinter extends Label
	{
		private final String truthy;
		private final String falsy;
		
		public MyBooleanPrinter(String truthy, String falsy)
		{
			super(null);
			this.truthy = truthy;
			this.falsy = falsy;
			
			setState(false);
		}
		
		public void setState(boolean state)
		{
			if(state)
			{
				setForeground(Color.green);
				setText(truthy);
			}
			else
			{
				setForeground(Color.red);
				setText(falsy);
			}
			revalidate();
		}
	}
	
	private class Label extends JComponent
	{
		private final FontRenderContext ftc = new FontRenderContext(new AffineTransform(), true, true);
		
		private String text;
		int width;
		int y;
		
		public Label(String text)
		{
			if(text != null)
			{
				setText(text);
			}
		}
		
		public void setText(String text)
		{
			if(text == null)
			{
				text = "null";
			}
			this.text = text;
			
			Rectangle2D rect = globalFont.getStringBounds(text, ftc);
			width = (int) Math.ceil(rect.getWidth());
			int height = (int) Math.ceil(rect.getHeight());
			
			LineMetrics metrics = globalFont.getLineMetrics(text, ftc);
			y = (int) Math.ceil(metrics.getHeight() - metrics.getDescent());
			
			int extraWidth = getInsets().left + getInsets().right;
			int extraHeight = getInsets().top + getInsets().bottom;
			setPreferredSize(new Dimension(width + extraWidth, height + extraHeight));
			setMinimumSize(new Dimension(width + extraWidth, height + extraHeight));
			
			revalidate();
		}
		
		@Override
		public void paint(Graphics g)
		{
			Insets insets = getInsets();
			Font f = g.getFont();
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setFont(globalFont);
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth() + insets.left + insets.right, getHeight() + insets.top + insets.bottom);
			g.setColor(getForeground());
			g.drawString(text, insets.left, y + insets.top);
			g.setFont(f);
		}
	}
	
	private class MyTextHighlightField extends Label
	{
		public MyTextHighlightField(String text)
		{
			super(null);
			setBackground(Color.white);
			setBorder(new EmptyBorder(0, 5, 0, 5));
			setText(text);
		}
	}
}

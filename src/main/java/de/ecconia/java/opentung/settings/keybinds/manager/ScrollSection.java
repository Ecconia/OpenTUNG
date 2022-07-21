package de.ecconia.java.opentung.settings.keybinds.manager;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneLayout;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;

public class ScrollSection extends JScrollPane
{
	public ScrollSection(JComponent component)
	{
		super(component);
		
		getVerticalScrollBar().setUnitIncrement(16);
		setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		setComponentZOrder(getVerticalScrollBar(), 0);
		setComponentZOrder(getViewport(), 1);
		getVerticalScrollBar().setOpaque(false);
		setBorder(new EmptyBorder(0, 0, 0, 0));
		
		//https://stackoverflow.com/questions/16373459/java-jscrollbar-design
		setLayout(new ScrollPaneLayout()
		{
			@Override
			public void layoutContainer(Container parent)
			{
				JScrollPane scrollPane = (JScrollPane) parent;
				
				Rectangle bounds = scrollPane.getBounds();
				bounds.x = bounds.y = 0;
				
				Insets insets = parent.getInsets();
				bounds.x = insets.left;
				bounds.y = insets.top;
				bounds.width -= insets.left + insets.right;
				bounds.height -= insets.top + insets.bottom;
				
				Rectangle verticalScrollBarBounds = new Rectangle();
				verticalScrollBarBounds.width = 12;
				verticalScrollBarBounds.height = bounds.height;
				verticalScrollBarBounds.x = bounds.x + bounds.width - verticalScrollBarBounds.width;
				verticalScrollBarBounds.y = bounds.y;
				
				if(viewport != null)
				{
					viewport.setBounds(bounds);
				}
				if(vsb != null)
				{
					vsb.setVisible(true);
					vsb.setBounds(verticalScrollBarBounds);
				}
			}
		});
		getVerticalScrollBar().setPreferredSize(new Dimension(12, 0));
		getVerticalScrollBar().setUI(new BasicScrollBarUI()
		{
			private final Dimension d = new Dimension();
			
			@Override
			protected JButton createDecreaseButton(int orientation)
			{
				return new JButton()
				{
					@Override
					public Dimension getPreferredSize()
					{
						return d;
					}
				};
			}
			
			@Override
			protected JButton createIncreaseButton(int orientation)
			{
				return new JButton()
				{
					@Override
					public Dimension getPreferredSize()
					{
						return d;
					}
				};
			}
			
			@Override
			protected void paintTrack(Graphics g, JComponent c, Rectangle r)
			{
			}
			
			@Override
			protected void paintThumb(Graphics g, JComponent c, Rectangle r)
			{
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				Color color;
				JScrollBar sb = (JScrollBar) c;
				if(!sb.isEnabled() || r.width > r.height)
				{
					return;
				}
				else if(isDragging)
				{
					color = new Color(80, 80, 80, 200);
				}
				else if(isThumbRollover())
				{
					color = new Color(100, 100, 100, 200);
				}
				else
				{
					color = new Color(50, 50, 50, 200);
				}
				g2.setPaint(color);
				g2.fillRoundRect(r.x + 1, r.y, r.width - 2, r.height, 10, 10);
				g2.setPaint(Color.gray);
				g2.drawRoundRect(r.x, r.y, r.width - 1, r.height, 10, 10);
				g2.dispose();
			}
			
			@Override
			protected void setThumbBounds(int x, int y, int width, int height)
			{
				super.setThumbBounds(x, y, width, height);
				scrollbar.repaint();
			}
		});
	}
}

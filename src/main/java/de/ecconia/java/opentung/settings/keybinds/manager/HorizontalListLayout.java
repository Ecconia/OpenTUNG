package de.ecconia.java.opentung.settings.keybinds.manager;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

public class HorizontalListLayout implements LayoutManager
{
	private final int hGap;
	private final boolean preferMinimum;
	
	public HorizontalListLayout(int hGap, boolean preferMinimum)
	{
		this.hGap = hGap;
		this.preferMinimum = preferMinimum;
	}
	
	@Override
	public void addLayoutComponent(String name, Component comp)
	{
	}
	
	@Override
	public void removeLayoutComponent(Component comp)
	{
	}
	
	@Override
	public Dimension preferredLayoutSize(Container parent)
	{
		synchronized(parent.getTreeLock())
		{
			Insets insets = parent.getInsets();
			int w = 0;
			int h = 0;
			for(Component comp : parent.getComponents())
			{
				Dimension min = comp.getPreferredSize();
				if(min.height > h)
				{
					h = min.height;
				}
				w += min.width;
			}
			w += (parent.getComponentCount() - 1) * hGap;
			
			return new Dimension(w + insets.left + insets.right, h + insets.top + insets.bottom);
		}
	}
	
	@Override
	public Dimension minimumLayoutSize(Container parent)
	{
		synchronized(parent.getTreeLock())
		{
			Insets insets = parent.getInsets();
			int w = 0;
			int h = 0;
			for(Component comp : parent.getComponents())
			{
				Dimension min = comp.getMinimumSize();
				if(min.height > h)
				{
					h = min.height;
				}
				w += min.width;
			}
			w += (parent.getComponentCount() - 1) * hGap;
			
			return new Dimension(w + insets.left + insets.right, h + insets.top + insets.bottom);
		}
	}
	
	@Override
	public void layoutContainer(Container parent)
	{
		synchronized(parent.getTreeLock())
		{
			Insets insets = parent.getInsets();
			int height = (preferMinimum ? minimumLayoutSize(parent) : preferredLayoutSize(parent)).height - insets.top - insets.bottom;
			int w = insets.left;
			for(Component comp : parent.getComponents())
			{
				Dimension min = preferMinimum ? comp.getMinimumSize() : comp.getPreferredSize();
				comp.setBounds(w, insets.top, min.width, height);
				w += hGap + min.width;
			}
		}
	}
}

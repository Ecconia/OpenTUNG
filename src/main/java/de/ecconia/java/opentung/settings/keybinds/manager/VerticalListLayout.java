package de.ecconia.java.opentung.settings.keybinds.manager;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

public class VerticalListLayout implements LayoutManager
{
	private final int vGap;
	private final boolean preferMinimum;
	
	public VerticalListLayout(int vGap, boolean preferMinimum)
	{
		this.vGap = vGap;
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
				if(min.width > w)
				{
					w = min.width;
				}
				h += min.height;
			}
			h += (parent.getComponentCount() - 1) * vGap;
			
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
				if(min.width > w)
				{
					w = min.width;
				}
				h += min.height;
			}
			h += (parent.getComponentCount() - 1) * vGap;
			
			return new Dimension(w + insets.left + insets.right, h + insets.top + insets.bottom);
		}
	}
	
	@Override
	public void layoutContainer(Container parent)
	{
		synchronized(parent.getTreeLock())
		{
			Insets insets = parent.getInsets();
			int width = (preferMinimum ? preferredLayoutSize(parent) : minimumLayoutSize(parent)).width - insets.right - insets.left;
			int h = insets.top;
			for(Component comp : parent.getComponents())
			{
				Dimension min = preferMinimum ? comp.getMinimumSize() : comp.getPreferredSize();
				comp.setBounds(insets.left, h, width, min.height);
				h += vGap + min.height;
			}
		}
	}
}

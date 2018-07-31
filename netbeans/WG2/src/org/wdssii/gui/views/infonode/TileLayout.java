package org.wdssii.gui.views.infonode;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.Vector;

import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

/** My implementation of a tile layout that matches the old c++ display
 * 
 * @author Robert Toomey
 * 
 */
public class TileLayout  implements LayoutManager {
	
	private final static Logger LOG = LoggerFactory.getLogger(TileLayout.class);

	private int minWidth = 0, minHeight = 0;
	private int preferredWidth = 0, preferredHeight = 0;

	public TileLayout() {
	}

	/* Required by LayoutManager. */
	public void addLayoutComponent(String name, Component comp) {
	}

	/* Required by LayoutManager. */
	public void removeLayoutComponent(Component comp) {
	}


	/* Required by LayoutManager. */
	public Dimension preferredLayoutSize(Container parent) {
		Dimension dim = new Dimension(0, 0);

		//Always add the container's insets!
		Insets insets = parent.getInsets();
		dim.width = preferredWidth
				+ insets.left + insets.right;
		dim.height = preferredHeight
				+ insets.top + insets.bottom;

		return dim;
	}

	/* Required by LayoutManager. */
	public Dimension minimumLayoutSize(Container parent) {
		Dimension dim = new Dimension(0, 0);
		int nComps = parent.getComponentCount();

		//Always add the container's insets!
		Insets insets = parent.getInsets();
		dim.width = minWidth
				+ insets.left + insets.right;
		dim.height = minHeight
				+ insets.top + insets.bottom;

		return dim;
	}

	/* Required by LayoutManager. */
	/*
	 * Do our tile layout from c++ wg version.
	 * 
	 */
	public void layoutContainer(Container parent) {
		int n = parent.getComponentCount();

		// Rectangle of parent.
		Insets insets = parent.getInsets();
		
		final int il = insets.left;
		final int ir = insets.right;
		final int it = insets.top;
		final int ib = insets.bottom;
		
		int rwidth = parent.getWidth() - (il + ir);
		int rheight = parent.getHeight() - (it + ib);
		int width = rwidth;
		int height = rheight;
		int rx = 0+il;
		int ry = 0+it;

		int rows = 1;
		int cols = 1;
		while (rows * cols < n)
		{
			if (cols <= rows) {
				cols++;
			}else {
				rows++;
			}
		}
		int add = cols* rows -n;
		Vector<Boolean> used = new Vector<Boolean>();
		for(int z = 0; z < rows*cols; z++) {used.add(false); }
		int row = 0;
		int col = 0;
		int w = width/cols;
		int h = height/rows;

		for (int i = 0 ; i < n ; i++) {
			Component c = parent.getComponent(i);

			// How to skip a window from tiling
			// if (yaWannaSkip) continue;
			if (!c.isVisible()) { continue; }

			used.set(row*cols+col, true);

			if (add > 0) {
				c.setBounds(rx+col*w, ry+row*h, w, (2*h));
				used.set((row+1)*cols+col, true);
				add--;
			}else {
				c.setBounds(rx+col*w, ry+row*h, w, h );
			}

			while ((row < rows) && (col < cols) && used.get(row*cols+col))
			{
				col++;
				if (col == cols) {
					col = 0;
					row++;
				}
			}
		}
	}

	public String toString() {
		return getClass().getName();
	}
}
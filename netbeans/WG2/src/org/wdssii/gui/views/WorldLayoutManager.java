package org.wdssii.gui.views;

/**
 * World Layout Manager. A custom layout manager for the multiple world grid. We
 * want a quick way to show/hide multiple windows, etc...without having to
 * constantly recreate a MigLayout or other layout object.
 *
 * This Layout Manager expects everything to be a WorldWindow
 *
 * @author Robert Toomey
 */
import java.awt.*;

public class WorldLayoutManager implements LayoutManager {

    private int vgap;
    private int minWidth = 0, minHeight = 0;
    private int preferredWidth = 0, preferredHeight = 0;
    private boolean sizeUnknown = true;

    public WorldLayoutManager() {
        this(5);
    }

    public WorldLayoutManager(int v) {
        vgap = v;
    }

    /* Required by LayoutManager. */
    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    /* Required by LayoutManager. */
    @Override
    public void removeLayoutComponent(Component comp) {
    }

    private void setSizes(Container parent) {
        int nComps = parent.getComponentCount();
        Dimension d = null;

        //Reset preferred/minimum width and height.
        preferredWidth = 0;
        preferredHeight = 0;
        minWidth = 0;
        minHeight = 0;

        for (int i = 0; i < nComps; i++) {
            Component c = parent.getComponent(i);
            if (c.isVisible()) {  // Might even ignore the visibility
                d = c.getPreferredSize();

                if (i > 0) {
                    preferredWidth += d.width / 2;
                    preferredHeight += vgap;
                } else {
                    preferredWidth = d.width;
                }
                preferredHeight += d.height;

                minWidth = Math.max(c.getMinimumSize().width,
                        minWidth);
                minHeight = preferredHeight;
            }
        }
    }

    /* Required by LayoutManager. */
    @Override
    public Dimension preferredLayoutSize(Container parent) {
        Dimension dim = new Dimension(0, 0);
        int nComps = parent.getComponentCount();

        setSizes(parent);

        //Always add the container's insets!
        Insets insets = parent.getInsets();
        dim.width = preferredWidth
                + insets.left + insets.right;
        dim.height = preferredHeight
                + insets.top + insets.bottom;

        sizeUnknown = false;

        return dim;
    }

    /* Required by LayoutManager. */
    @Override
    public Dimension minimumLayoutSize(Container parent) {
        Dimension dim = new Dimension(0, 0);
        int nComps = parent.getComponentCount();

        //Always add the container's insets!
        Insets insets = parent.getInsets();
        dim.width = minWidth
                + insets.left + insets.right;
        dim.height = minHeight
                + insets.top + insets.bottom;

        sizeUnknown = false;

        return dim;
    }

    /* Required by LayoutManager. */
    /*
     * This is called when the panel is first displayed,
     * and every time its size changes.
     * Note: You CAN'T assume preferredLayoutSize or
     * minimumLayoutSize will be called -- in the case
     * of applets, at least, they probably won't be.
     */
    @Override
    public void layoutContainer(Container parent) {

        // We will layout in a grid, where we maximize the 'square' of 
        // each window.
        int nComps = parent.getComponentCount();
        if (nComps == 0){ return; } 
        Dimension d = parent.getSize();
       
       // float wantedWidth =  d.width/nComps;  // don't we need modulus maybe?
       // float wantedHeight = d.height;
      //  int x = 0;
       // for (int i = 0; i < nComps; i++) {
       //     Component c = parent.getComponent(i);
       //     c.setBounds(x, 0, (int) wantedWidth, (int) wantedHeight);
       //     x += wantedWidth;
       //     
       // }
        
        int nRows = 1;
        int nCols = nComps;
        int comp = 0;
        float wantedWidth =  d.width/nCols;  // don't we need modulus maybe?
        float wantedHeight = d.height/nRows;
        // Roworder when width >= height..
        int x = 0;
        int y = 0;
        for(int r = 0; r < nRows; r++){
            for(int c = 0; c < nCols; c++){
                Component o = parent.getComponent(comp++);
                o.setBounds(x, y, (int) wantedWidth, (int) wantedHeight);
                x += wantedWidth;
            }
        }
    }

    @Override
    public String toString() {
        String str = "";
        return getClass().getName() + "[vgap=" + vgap + str + "]";
    }
}
package org.wdssii.gui.swing.icon;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;

/**
 * An arrow for navigational direction that is drawn by polygon, it
 * also has two colors the border and the fill.
 * This makes sure we always have basic nav icons, even if icons are
 * not on disk.
 * 
 * @author Robert Toomey
 */
public class DynamicArrowIcon implements Icon {

    private int width = 16;
    private int height = 16;
    protected BasicStroke stroke = new BasicStroke(1);

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public int getIconHeight() {
        return height;
    }

    public Color getLineColor() {
        return Color.BLACK;
    }

    public Color getFillColor() {
        return Color.GREEN;
    }
}

package org.wdssii.gui.swing.icon;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;

/**
 *
 * @author Robert Toomey
 */
public class DownArrowIcon extends DynamicArrowIcon {

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2d = (Graphics2D) g.create();
        Polygon p = new Polygon();
        p.addPoint(7, 14);   // point
        p.addPoint(1, 8);   // left side
        p.addPoint(13, 8);  // right side
        p.translate(x, y);
        g2d.setColor(getFillColor());
        g2d.fillPolygon(p);
        g2d.setColor(getLineColor());
        g2d.setStroke(stroke);
        g2d.drawPolygon(p);
        g2d.dispose();
    }
}

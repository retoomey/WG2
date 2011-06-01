package org.wdssii.gui.swing.icon;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;

/**
 *
 * @author Robert Toomey
 */
public class RightArrowEndIcon extends DynamicArrowIcon {

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2d = (Graphics2D) g.create();
        Polygon p = new Polygon();
        p.addPoint(4, 1);   // left side
        p.addPoint(10, 7);  // point
        p.addPoint(4, 13);  // right side
        p.translate(x, y);
        g2d.setColor(getFillColor());
        g2d.fillPolygon(p);
        g2d.setColor(getLineColor());
        g2d.setStroke(stroke);
        g2d.drawPolygon(p);


        p = new Polygon();
        p.addPoint(12, 1);   // top left
        p.addPoint(12, 13);  // bottom left
        p.addPoint(14, 13);  // bottom right
        p.addPoint(14, 1);   // top right
        p.translate(x, y);
        g2d.setColor(getFillColor());
        g2d.fillPolygon(p);
        g2d.setColor(getLineColor());
        g2d.setStroke(stroke);
        g2d.drawPolygon(p);


        g2d.dispose();
    }
}
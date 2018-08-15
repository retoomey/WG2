package org.wdssii.gui.renderers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.wdssii.xml.iconSetConfig.ArrowSymbol;
import org.wdssii.xml.iconSetConfig.Symbol;

/**
 * ArrowSymbolRenderer draws an arrow head and tail
 *
 * @author Robert Toomey
 */
public class ArrowSymbolRenderer extends SymbolRenderer {

    private ArrowSymbol s = null;

    @Override
    public int getPointSize() {
        return s.pointsize;
    }

    /**
     * Return the color for our merging count algorithm. Default is white
     */
    @Override
    public Color getMergedBorderColor() {
        if (s != null) {
            return s.color;
        }
        return Color.WHITE;
    }

    /**
     * Draws an arrow pointing in x direction
     */
    public static void arrow(GL glold, double length, double width, int taillength, int tailthick) {

        if (tailthick > width) {
            tailthick = (int) width;
        }
        final double halfLength = length / 2.0;
        final double halfWidth = width / 2.0;
        final GL2 gl = glold.getGL().getGL2();
        gl.glVertex2d(halfLength, 0);           // Tip
        gl.glVertex2d(-halfLength, halfWidth);  // Left wing

        // Optional tail section....making part of polygon.  Maybe
        // would be better as a generic 'line' option to any symbol..?
        if (taillength > -1) {
            final double t = tailthick / 2.0d;
            gl.glVertex2d(-halfLength, t);
            gl.glVertex2d(-halfLength - taillength, t);
            gl.glVertex2d(-halfLength - taillength, -t);
            gl.glVertex2d(-halfLength, -t);
        }
        gl.glVertex2d(-halfLength, -halfWidth); // Right wing

    }

    public static void arrow(Polygon p, double length, double width, int taillength, int tailthick) {

        if (tailthick > width) {
            tailthick = (int) width;
        }
        final int halfLength = (int) (length / 2.0);
        final int halfWidth = (int) (width / 2.0);
        
        p.addPoint(halfLength, 0);           // Tip
        p.addPoint(-halfLength, halfWidth);  // Left wing

        // Optional tail section....making part of polygon.  Maybe
        // would be better as a generic 'line' option to any symbol..?
        if (taillength > -1) {
            final int t = (int)(tailthick / 2.0d);
            p.addPoint(-halfLength, t);
            p.addPoint(-halfLength - taillength, t);
            p.addPoint(-halfLength - taillength, -t);
            p.addPoint(-halfLength, -t);
        }
        p.addPoint(-halfLength, -halfWidth); // Right wing  
    }

    @Override
    public void setSymbol(Symbol symbol) {
        if (symbol instanceof ArrowSymbol) {
            s = (ArrowSymbol) symbol;
        }
    }

    /**
     * Draw icon in 2D (assume 2d viewport and 0,0 as center of icon)
     *
     */
    @Override
    public void render(GL glold) {

        //final double polyRadius = s.pointsize / 2.0;
        final GL2 gl = glold.getGL().getGL2();
        // Translate icon.
        gl.glTranslatef((float) s.xoffset, (float) s.yoffset, 0);
        gl.glRotatef(s.phaseangle, 0, 0, 1);

        /**
         * Draw symbol
         */
        gl.glColor4f(s.color.getRed() / 255.0f, s.color.getGreen() / 255.0f,
                s.color.getBlue() / 255.0f, s.color.getAlpha() / 255.0f);
        gl.glBegin(GL2.GL_POLYGON);
        
        arrow(gl, s.pointsize, s.width, s.taillength, s.tailthick);
        gl.glEnd();

        /**
         * Draw outline of symbol
         */
        if (s.useOutline) {
            gl.glLineWidth(s.osize);
            gl.glColor4f(s.ocolor.getRed() / 255.0f, s.ocolor.getGreen() / 255.0f,
                    s.ocolor.getBlue() / 255.0f, s.ocolor.getAlpha() / 255.0f);
            gl.glBegin(GL.GL_LINE_LOOP);
            arrow(gl, s.pointsize, s.width, s.taillength, s.tailthick);
            gl.glEnd();
        }

        // Translate back.  Wasteful if all icons translate the same
        gl.glRotatef(-s.phaseangle, 0, 0, 1);
        super.render(gl);
        gl.glTranslatef((float) -s.xoffset, (float) -s.yoffset, 0);

    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        if (s != null) {

            int size = getIconHeight();
            final double polyRadius = size / 2.0;
            /**
             * Draw symbol
             */
            Graphics2D g2d = (Graphics2D) (g);
            g2d.setColor(s.color);
            Polygon p = new Polygon();
            g2d.translate(x + polyRadius, y + polyRadius);
            g2d.rotate(Math.toRadians(360-s.phaseangle));
            int width = Math.min(size, s.width);
            int length = Math.min((int)polyRadius, s.pointsize);
            arrow(p, length, width, s.taillength, s.tailthick);
            g2d.fillPolygon(p);
            /**
             * Draw outline of symbol
             */
            if (s.useOutline) {
                g2d.setStroke(new BasicStroke(s.osize));
                g2d.setColor(s.ocolor);
                g2d.drawPolygon(p);
            }
            
            g2d.rotate(Math.toRadians(-(360-s.phaseangle)));
            g2d.translate(-(x + polyRadius), -(y + polyRadius));
        } else {
            // Humm blank?
        }
    }
}

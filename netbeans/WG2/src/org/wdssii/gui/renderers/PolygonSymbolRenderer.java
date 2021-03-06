package org.wdssii.gui.renderers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.wdssii.xml.iconSetConfig.PolygonSymbol;
import org.wdssii.xml.iconSetConfig.Symbol;

/**
 * polygonSymbolRenderer draws a equal angled polygon around a center location.
 * This can draw squares, diamonds, circles.
 *
 * @author Robert Toomey
 */
public class PolygonSymbolRenderer extends SymbolRenderer {

    private PolygonSymbol s = null;

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
     * Square -- polygon(gl, polyRadius, 45,4); Diamond -- polygon(gl,
     * polyRadius, 0,4); GL_POLYGON (inside), GL_LINE_LOOP (outline)
     */
    public static void polygon(GL glold, double radius, double phase, int points) {
    	final GL2 gl = glold.getGL().getGL2();
        final double m = 2.0 * Math.PI / points;
        final double pr = Math.toRadians(phase);
        double angle = pr;  // Start angle
        // OpenGL starts bottomleft...
        for (int i = 0; i < points; i++) {
            gl.glVertex2d(radius * Math.cos(angle), radius * Math.sin(angle));
            angle += m;
        }
    }

    public static void polygon(Graphics g, Polygon p, double radius, double phase, int points) {
        final double m = 2.0 * Math.PI / points;
        final double pr = Math.toRadians(phase);
        double angle = pr;  // Start angle
        // Java 2D starts topleft....
        for (int i = 0; i < points; i++) {
            p.addPoint((int) (-radius * Math.cos(angle)), (int) (-radius * Math.sin(angle)));
            angle += m;
        }
    }

    @Override
    public void setSymbol(Symbol symbol) {
        if (symbol instanceof PolygonSymbol) {
            s = (PolygonSymbol) symbol;
        }
    }

    /**
     * Draw icon in 2D (assume 2d viewport and 0,0 as center of icon)
     *
     */
    @Override
    public void render(GL glold) {

        final double polyRadius = s.pointsize / 2.0;
    	final GL2 gl = glold.getGL().getGL2();

        // Translate icon.   (this could be done outside for speed)
        gl.glTranslatef((float) s.xoffset, (float) s.yoffset, 0);

        /**
         * Draw symbol
         */
        gl.glColor4f(s.color.getRed() / 255.0f, s.color.getGreen() / 255.0f,
                s.color.getBlue() / 255.0f, s.color.getAlpha() / 255.0f);
        gl.glBegin(GL2.GL_POLYGON);
        polygon(gl, polyRadius, s.phaseangle, s.numpoints);
        gl.glEnd();

        /**
         * Draw outline of symbol
         */
        if (s.useOutline) {
            gl.glLineWidth(s.osize);
            gl.glColor4f(s.ocolor.getRed() / 255.0f, s.ocolor.getGreen() / 255.0f,
                    s.ocolor.getBlue() / 255.0f, s.ocolor.getAlpha() / 255.0f);
            gl.glBegin(GL.GL_LINE_LOOP);
            polygon(gl, polyRadius, s.phaseangle, s.numpoints);
            gl.glEnd();
        }

        // Translate back.  Wasteful if all icons translate the same
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
            polygon(g2d, p, polyRadius, s.phaseangle, s.numpoints);
            g2d.fillPolygon(p);
            /**
             * Draw outline of symbol
             */
            if (s.useOutline) {
                g2d.setStroke(new BasicStroke(s.osize));
                g2d.setColor(s.ocolor);
                g2d.drawPolygon(p);
            }

        } else {
            // Humm blank?
        }
    }
}

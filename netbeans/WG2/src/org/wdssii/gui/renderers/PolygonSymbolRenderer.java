package org.wdssii.gui.renderers;

import javax.media.opengl.GL;
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

    /**
     * Square -- polygon(gl, polyRadius, 45,4); Diamond -- polygon(gl,
     * polyRadius, 0,4); GL_POLYGON (inside), GL_LINE_LOOP (outline)
     */
    public static void polygon(GL gl, double radius, double phase, int points) {
        final double m = 2.0 * Math.PI / points;
        final double pr = Math.toRadians(phase);
        double angle = pr;  // Start angle
        for (int i = 0; i < points; i++) {
            gl.glVertex2d(radius * Math.cos(angle), radius * Math.sin(angle));
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
    public void render(GL gl) {

        final double polyRadius = s.pointsize / 2.0;

        // Translate icon.   (this could be done outside for speed)
        gl.glTranslatef((float) s.xoffset, (float) s.yoffset, 0);

        /**
         * Draw symbol
         */
        gl.glColor4f(s.color.getRed() / 255.0f, s.color.getGreen() / 255.0f,
                s.color.getBlue() / 255.0f, s.color.getAlpha() / 255.0f);
        gl.glBegin(GL.GL_POLYGON);
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
        gl.glTranslatef((float) -s.xoffset, (float) -s.yoffset, 0);

    }
}

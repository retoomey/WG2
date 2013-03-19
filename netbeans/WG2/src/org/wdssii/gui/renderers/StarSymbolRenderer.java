package org.wdssii.gui.renderers;

import javax.media.opengl.GL;
import org.wdssii.xml.iconSetConfig.StarSymbol;
import org.wdssii.xml.iconSetConfig.Symbol;

/**
 *
 * Star symbol draws a collection of equal angled lines through a center
 * point of the location.
 *
 * @author Robert Toomey
 */
public class StarSymbolRenderer extends SymbolRenderer {

    private StarSymbol s;

    @Override
    public void setSymbol(Symbol symbol) {
        if (symbol instanceof StarSymbol) {
            s = (StarSymbol) symbol;
        }
    }

    /**
     * Cross -- star(gl, polyRadius, 0,4); GL_LINES
     */
    public static void star(GL gl, double radius, double phase, int points) {
        points = (points/2);   // Have to be multiple of 2    
        final double m =  Math.PI / points;
        final double pr = Math.toRadians(phase);
        double angle = pr;  // Start angle
        double a2 = pr + Math.PI; // Other side
        for (int i = 0; i < points; i++) {
            gl.glVertex2d(radius * Math.cos(angle), radius * Math.sin(angle));
            gl.glVertex2d(radius * Math.cos(a2), radius * Math.sin(a2));
            angle += m;
            a2 += m;
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
         * Draw outline of symbol (first for lines, make outline the 'background')
         */
        if (s.useOutline) {
            gl.glLineWidth(s.osize);
            gl.glColor4f(s.ocolor.getRed(), s.ocolor.getGreen(), s.ocolor.getBlue(), s.color.getAlpha());
            gl.glBegin(GL.GL_LINES);
            star(gl, polyRadius, s.phaseangle, s.numpoints);
            gl.glEnd();
        }

        /**
         * Draw symbol
         */
        gl.glLineWidth(s.lsize);
        gl.glColor4f(s.color.getRed(), s.color.getGreen(), s.color.getBlue(), s.color.getAlpha());
        gl.glBegin(GL.GL_LINES);
        star(gl, polyRadius, s.phaseangle, s.numpoints);
        gl.glEnd();

        // Translate back.  Wasteful if all icons translate the same
        gl.glTranslatef((float) -s.xoffset, (float) -s.yoffset, 0);

    }
}

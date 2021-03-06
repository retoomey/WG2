package org.wdssii.gui.renderers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.swing.Icon;
import org.wdssii.xml.iconSetConfig.Symbol;

/**
 * Base class for symbol renderers.
 *
 * @author Robert Toomey
 */
public abstract class SymbolRenderer implements Icon {

    public final boolean DEBUG_BOX = false;
    public  static boolean COLOR1 = true;
    
    /**
     * Data structure for my merging rectangle attempt
     */
    public static class SymbolRectangle {

        public int x;
        public int x2;
        public int y;
        public int y2;
        public int centerx;
        public int centery;
    }
    // OpenGL methods...

    public abstract void setSymbol(Symbol symbol);

    /** Return the color for our merging count algorithm.  Default is white */
    public Color getMergedBorderColor(){
        return Color.WHITE;
    }
    
    /** Rectangle used for our merging count algorithm */
    public void renderSymbolRectangle(GL glold, SymbolRectangle r){
         Color c = getMergedBorderColor();
     	 final GL2 gl = glold.getGL().getGL2();

         gl.glLineWidth(3);
         gl.glColor4f(c.getRed()/255.0f, c.getGreen()/255.0f, c.getBlue()/255.0f, .80f);
         gl.glEnable(GL2.GL_LINE_STIPPLE);
         gl.glLineStipple(1, (short) 0xAAAA);
         gl.glBegin(GL.GL_LINE_LOOP);
         gl.glVertex2d(r.x, r.y);
         gl.glVertex2d(r.x, r.y2);
         gl.glVertex2d(r.x2, r.y2);
         gl.glVertex2d(r.x2, r.y);
         gl.glEnd();
         gl.glDisable(GL2.GL_LINE_STIPPLE);
         gl.glLineWidth(1);
    }
    
    public void render(GL gl) {

        // Render a red box for debugging...subclass should translate
        // before calling super...
        if (DEBUG_BOX) {
            double p = getPointSize() / 2.0;
            double radius = Math.sqrt(2*p*p);
        	final GL2 gl2 = gl.getGL().getGL2();

            gl2.glLineWidth(1);
            if (COLOR1){
            gl2.glColor4f(1, 0, 0, 1);
            }else{
                gl2.glColor4f(0,1,0,1);
            }
            gl2.glBegin(GL.GL_LINE_LOOP);
            PolygonSymbolRenderer.polygon(gl, radius, 45.0, 4);
            gl2.glEnd();
        }
    }

    public SymbolRectangle getSymbolRectangle(int cx, int cy) {
        SymbolRectangle r = new SymbolRectangle();
        r.centerx = cx;
        r.centery = cy;
        final double radius = getPointSize() / 2.0;
        r.x = (int) (cx - radius);
        r.y = (int) (cy - radius);
        r.x2 = (int) (cx + radius);
        r.y2 = (int) (cy + radius);
        return r;
    }

    public abstract int getPointSize();

    // Java Icon methods...
    @Override
    public abstract void paintIcon(Component c, Graphics g, int x, int y);

    @Override
    public int getIconWidth() {
        return 16;
    }

    @Override
    public int getIconHeight() {
        return 16;
    }
}

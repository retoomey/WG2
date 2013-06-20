package org.wdssii.gui.swing;

import java.awt.Graphics;
import javax.swing.JComponent;
import org.wdssii.gui.ColorMap;
import org.wdssii.gui.renderers.ColorMapRenderer;

/**
 * A widget that draws a ColorMap using a ColorMapRenderer
 * 
 * @author Robert Toomey
 */
public class JColorMap extends JComponent {

    /** The renderer that draws our stuff */
    ColorMapRenderer myRenderer;

    /** Set the renderer we use to fill ourselves */
    public void setColorMapRenderer(ColorMapRenderer r) {
        myRenderer = r;
        repaint();
    }

    /** Standard swing java paint... */
    @Override
    public void paintComponent(Graphics g) {
        if (myRenderer != null) {
            myRenderer.paintToGraphics(g, getWidth(), getHeight());
        }
    }

    /** Paint to file at the scale of our current size, return a
     string telling what happened */
    public String paintToFile(String fileName) {
        return paintToFile(fileName, getWidth(), getHeight());
    }

    /** Paint to file at the scale of our current size */
    public String paintToFile(String fileName, int w, int h) {
        String status = "No renderer to draw color map";
        if (myRenderer != null) {
            status = myRenderer.paintToFile(fileName, w, h);
        }
        return status;
    }

    /** Set the current color map of our renderer */
    public void setColorMap(ColorMap c) {
        if (myRenderer != null) {
            myRenderer.setColorMap(c);
        }
        repaint();
    }
}

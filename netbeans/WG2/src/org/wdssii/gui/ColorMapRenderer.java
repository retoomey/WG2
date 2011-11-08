package org.wdssii.gui;

import com.sun.opengl.util.j2d.TextRenderer;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.wdssii.gui.ColorMap.ColorMapOutput;

/**
 * ColorMapRenderer.   Base class for rendering a ColorMap.
 * Since I'm only planning a few different ways of rendering, just went ahead
 * and made it a single class with functions.  Might subclass for the openGL
 * part of it just to be cleaner...
 * 1.  Java graphics
 * 2.  OpenGL
 * 3.  Disk (through Java graphics)
 * @author Robert Toomey
 */
public class ColorMapRenderer {

    private static Log log = LogFactory.getLog(ColorMapRenderer.class);
    /** ColorMap used by the renderer */
    private ColorMap myColorMap;

    /** The extra filler padding 'above' and 'below' a color bin label */
    private int hTextPadding = 0;
    
    /** Get the ColorMap used by the renderer */
    public ColorMap getColorMap() {
        return myColorMap;
    }

    /** Set the ColorMap used by the renderer */
    public void setColorMap(ColorMap c) {
        myColorMap = c;
    }

    public ColorMapRenderer(ColorMap initColorMap) {
        myColorMap = initColorMap;
    }

    public ColorMapRenderer() {
        // Must call setColorMap before this will draw...
    }

    /** Paint to a file (snapshot for all purposes).  Returns an empty
    string on success, otherwise a reason for failure.
     * 
     * The GUI knows to ask for overwrite confirmation, if you call this directly
     * realize it will overwrite any given file with new stuff.
     */
    public String paintToFile(String fileName, int w, int h) {
        String success = "";
        if (myColorMap != null) {
            try {
                // Get the extension use it as the file type.  Supported types are
                // "PNG", "JPEG", "gif", "BMP"
                // "Mypicture.gif" --> ".gif"
                int dot = fileName.lastIndexOf(".");
                String type = fileName.substring(dot + 1);

                // Default is ".png" file
                // "Mypicture" --> "Mypicture.png"
                if ((type.equals(fileName)) || (type.isEmpty())) {
                    type = "png";
                    fileName += ".png";
                } else {
                    type = type.toLowerCase();
                }

                // Check that we have a writer for the suffix..
                String writerNames[] = ImageIO.getWriterFormatNames();
                boolean haveFormat = false;
                for (String s : writerNames) {
                    if (s.equals(type)) {
                        haveFormat = true;
                        break;
                    }
                }

                if (haveFormat) {
                    // Create a buffered image and render into it.
                    // TYPE_INT_ARGB specifies the image format: 8-bit RGBA packed
                    // into integer pixels

                    // This is annoying, we need a graphics to tell what size
                    // to draw the image as, but creating an image takes a size,
                    // lol.  So we do it twice.  FIXME: easier way?
                    BufferedImage bi = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D ig2 = bi.createGraphics();
                    int minH = getColorKeyMinHeight(ig2);
                    if (h < minH) {
                        h = minH;
                    }

                    // Create it now with wanted size...
                    bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    ig2 = bi.createGraphics();
                    paintToGraphics(ig2, w, h);

                    // ImageIO freaks unless the file already exists...
                    File output = new File(fileName);
                    //   if (!output.createNewFile()){
                    //      return "Writing image failed because '"+fileName+"' already exists";
                    //  }

                    if (output == null) {
                        success = "Writing image failed because file is null";
                    } else {
                        // Write draw graphics to file....

                        ImageIO.write(bi, type, output);

                        //ImageIO.write(bi, "PNG", new File("c:\\yourImageName.PNG"));
                        // ImageIO.write(bi, "JPEG", new File("c:\\yourImageName.JPG"));
                        // ImageIO.write(bi, "gif", new File("c:\\yourImageName.GIF"));
                        // ImageIO.write(bi, "BMP", new File("c:\\yourImageName.BMP"));
                        log.info("Drew color key to '" + fileName + "' as type '" + type + "'");
                    }
                } else {
                    success = "Writing image failed because there is no writer for '" + type + "'";
                }
            } catch (Exception e) {
                log.error(e.toString());
                success = "Writing image failed:" + e.toString();
            }
        } else {
            success = "Writing image failed because ColorMap is null";
        }
        return success;
    }

    /** Paint to a standard java graphics context */
    public void paintToGraphics(Graphics g, int w, int h) {

        if (myColorMap != null) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);

            FontRenderContext frc = g2.getFontRenderContext();
            Font f = getFont();

            FontMetrics metrics = g2.getFontMetrics(f);
            int textHeight = metrics.getMaxAscent() + metrics.getMaxDescent();
            int renderY = metrics.getMaxAscent() + (hTextPadding / 2);
            int cellHeight = textHeight+hTextPadding;
            // FILL: int cellHeight = Math.max(textHeight + padding, h);

            ColorMapOutput hi = new ColorMapOutput();
            ColorMapOutput lo = new ColorMapOutput();

            // Width of unit text
            int unitWidth = 0;
            String unitName = myColorMap.getUnits();
            if ((unitName != null) && (unitName.length() > 0)) {
                unitWidth = metrics.stringWidth(unitName) + 2;
            } else {
                unitWidth = 0;
            }

            // Calculate height
            int barwidth = Math.max(w - unitWidth, 1);
            int aSize = myColorMap.getNumberOfBins();
            int cellWidth = barwidth / aSize;
            barwidth = cellWidth * aSize;

            int currentX = 0;
            int top = 0;

            // Erase square of colormap
            g2.setColor(Color.BLACK);
            g2.fillRect(0, top, w, cellHeight);

            // Draw the boxes of the color map....
            for (int i = 0; i < aSize; i++) {

                myColorMap.getUpperBoundColor(hi, i);
                myColorMap.getLowerBoundColor(lo, i);

                Color loC = new Color(lo.redI(), lo.greenI(), lo.blueI());
                Color hiC = new Color(hi.redI(), hi.greenI(), hi.blueI());
                int curInt = currentX;

                GradientPaint p = new GradientPaint(curInt, top, loC,
                        curInt + cellWidth, top, hiC);
                g2.setPaint(p);
                g2.fillRect(curInt, top, cellWidth, cellHeight);

                currentX += cellWidth;
            }

            // Draw the text labels for bins
            boolean drawText = (barwidth >= 100);
            int viewx = 0;
            if (drawText) {
                currentX = viewx;
                int extraXGap = 7; // Force at least these pixels
                // between labels
                int drawnToX = viewx;
                for (int i = 0; i < aSize; i++) {
                    String label = myColorMap.getBinLabel(i);
                    int wtxt = metrics.stringWidth(label);

                    // Sparse draw, skipping when text overlaps
                    if (currentX >= drawnToX) {

                        // Don't draw if text sticks outside box
                        if (currentX + wtxt < (viewx + barwidth)) {

                            // Ok, render and remember how far it drew
                            TextLayout t2 = new TextLayout(label, f, frc);
                            // Shape outline = t2.getOutline(null);

                            cheezyOutline(g2, currentX + 2, renderY, t2);
                            drawnToX = currentX + wtxt + extraXGap;
                        }
                    }

                    currentX += cellWidth;
                }
            }
            // Draw the units, only there if unitWidth > 0
            if (unitWidth > 0) {
                int start = (viewx + w - unitWidth);
                TextLayout t2 = new TextLayout(unitName, f, frc);
                cheezyOutline(g2, start, renderY, t2);
            }
        } else {
        }
    }

    /** Paint to a given OpenGL context */
    public void paintToOpenGL(GL gl, int aViewWidth, int aViewHeight, float opacity) {

        if (myColorMap != null) {
            boolean attribsPushed = false;
            boolean modelviewPushed = false;
            boolean projectionPushed = false;

            int viewx = 0;

            // Created text renderer
            TextRenderer aText = null;
            Font font = getFont();
            if (aText == null) {
                aText = new TextRenderer(font, true, true);
            }
            //FontRenderContext frc = aText.getFontRenderContext();
            FontMetrics metrics = getFontMetrics(font, null);

            int textHeight = metrics.getMaxAscent() + metrics.getMaxDescent();
            int renderY = metrics.getMaxAscent() + (hTextPadding / 2);
            int cellHeight = textHeight+hTextPadding;
            // Different in regular paint because we fill entire image size...
            //int cellHeight = Math.max(textHeight + padding, h);

            ColorMapOutput hi = new ColorMapOutput();
            ColorMapOutput lo = new ColorMapOutput();

            // Width of unit text
            int unitWidth = 0;
            String unitName = myColorMap.getUnits();
            if ((unitName != null) && (unitName.length() > 0)) {
                //Rectangle2D boundsUnits = aText.getBounds(unitName);
                //wtxt = (int) (boundsUnits.getWidth() + 2.0d);
                unitWidth = metrics.stringWidth(unitName) + 2;
            } else {
                unitWidth = 0;
            }

            // Calculate height
            int barwidth = Math.max(aViewWidth - unitWidth, 1);
            int aSize = myColorMap.getNumberOfBins();
            int cellWidth = barwidth / aSize;
            barwidth = cellWidth * aSize;

            double currentX = 0.0;
            double top = aViewHeight;

            try {

                // System.out.println("Drawing color key layer");
                gl.glPushAttrib(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT
                        | GL.GL_ENABLE_BIT | GL.GL_TEXTURE_BIT
                        | GL.GL_TRANSFORM_BIT | GL.GL_VIEWPORT_BIT
                        | GL.GL_CURRENT_BIT);
                attribsPushed = true;
                gl.glEnable(GL.GL_BLEND);
                gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
                gl.glDisable(GL.GL_DEPTH_TEST);

                // Standard ortho projection
                gl.glMatrixMode(GL.GL_PROJECTION);
                gl.glPushMatrix();
                projectionPushed = true;
                gl.glLoadIdentity();
                gl.glOrtho(0, aViewWidth, 0, aViewHeight, -1, 1);  // TopLeft
                gl.glMatrixMode(GL.GL_MODELVIEW);
                gl.glPushMatrix();
                modelviewPushed = true;
                gl.glLoadIdentity();

                gl.glDisable(GL.GL_TEXTURE_2D); // no textures
                gl.glShadeModel(GL.GL_SMOOTH); // FIXME: pop attrib

                // Erase square of colormap
                Color backColor = Color.BLACK;
                gl.glColor4ub((byte) backColor.getRed(),  //FIXME
                        (byte) backColor.getGreen(), (byte) backColor.getBlue(),
                        (byte) (backColor.getAlpha() * opacity));
                gl.glRectd(0.0, top, 0.0 + aViewWidth + 0.5, top-cellHeight-0.5);

                if (aSize > 0) {
                    gl.glBegin(GL.GL_QUADS);

                    // Draw the boxes of the color map....
                    for (int i = 0; i < aSize; i++) {

                        myColorMap.getUpperBoundColor(hi, i);
                        myColorMap.getLowerBoundColor(lo, i);

                        gl.glColor4f(lo.redF(), lo.greenF(), lo.blueF(),
                                opacity);
                        gl.glVertex2d(currentX, top);
                        gl.glVertex2d(currentX, top-cellHeight);
                        gl.glColor4f(hi.redF(), hi.greenF(), hi.blueF(),
                                opacity);
                        gl.glVertex2d(currentX + cellWidth, top-cellHeight);
                        gl.glVertex2d(currentX + cellWidth, top);

                        currentX += cellWidth;
                    }
                    gl.glEnd();
                }
                
                gl.glColor4i(255, 0, 0, 255);
                // Draw the text labels for bins
                aText.begin3DRendering();
                aText.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                
                boolean drawText = (barwidth >= 100);
                if (drawText) {
                    currentX = viewx;
                    int extraXGap = 7; // Force at least these pixels
                    // between labels
                    int drawnToX = viewx;
                    for (int i = 0; i < aSize; i++) {
                        String label = myColorMap.getBinLabel(i);
                        int wtxt = metrics.stringWidth(label);
                        
                        // Sparse draw, skipping when text overlaps
                        if (currentX >= drawnToX) {

                            // Don't draw if text sticks outside box
                            if (currentX + wtxt < (viewx + barwidth)) {
                                
                                aText.draw(label, (int) (currentX + 2),
                                        (int)(top-renderY));
                                drawnToX = (int) (currentX + wtxt + extraXGap);
                            }
                        }

                        currentX += cellWidth;
                    }
                }

                // Draw the units (only there if wtxt > 0 from above)
                if (unitWidth > 0) {
                    int start = (viewx + aViewWidth - unitWidth);
                    aText.draw(unitName, start, (int)(top-renderY));
                }
                aText.end3DRendering();


            } finally {
                if (projectionPushed) {
                    gl.glMatrixMode(GL.GL_PROJECTION);
                    gl.glPopMatrix();
                }
                if (modelviewPushed) {
                    gl.glMatrixMode(GL.GL_MODELVIEW);
                    gl.glPopMatrix();
                }
                if (attribsPushed) {
                    gl.glPopAttrib();
                }
            }
        }
    }

    public FontMetrics getFontMetrics(Font forFont, Graphics2D g) {
        // Humm..do this everytime or cache it?
        FontMetrics fm;
        // No graphics if openGL.  'Could' pass a component down from above
        if (g == null) {
            BufferedImage bi = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
            fm = bi.getGraphics().getFontMetrics(forFont);
        } else {
            fm = g.getFontMetrics(forFont);
        }
        return fm;
    }

    public Font getFont() {
        return new Font("Arial", Font.PLAIN, 12);
        // Font.decode("Arial-PLAIN-12"); // shared with opengl code
    }

    /** A cheezy outline behind the text that doesn't require an outline
     * font to render.  It shadows by shifting the text 1 pixel in every
     * direction.  Not very fast, but color keys are more about looks.
     */
    public void cheezyOutline(Graphics2D g, int x, int y, TextLayout t) {

        // Draw a 'grid' of background to shadow the character....
        // We can get away with this because there aren't that many labels
        // in a color key really. Draw 8 labels shifted to get outline.
        g.setColor(Color.black);
        t.draw(g, x + 1, y + 1);
        t.draw(g, x, y + 1);
        t.draw(g, x - 1, y + 1);
        t.draw(g, x - 1, y);
        t.draw(g, x - 1, y - 1);
        t.draw(g, x, y - 1);
        t.draw(g, x + 1, y - 1);
        t.draw(g, x + 1, y);

        g.setColor(Color.white);
        t.draw(g, x, y);
    }

    public int getColorKeyMinHeight(Graphics g) {
        int cellHeight = 5;
        if (myColorMap != null) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);

            Font f = getFont();
            FontMetrics metrics = g2.getFontMetrics(f);
            int textHeight = metrics.getMaxAscent() + metrics.getMaxDescent();
            cellHeight = textHeight + hTextPadding;
        }
        return cellHeight;
    }
}

package org.wdssii.gui.charts;

import static org.wdssii.gui.charts.VSliceChart.myNumCols;
import static org.wdssii.gui.charts.VSliceChart.myNumRows;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

import org.wdssii.gui.GLUtil;
import org.wdssii.gui.GLWorld;
import org.wdssii.gui.products.FilterList;
import org.wdssii.gui.products.VolumeSlice2DOutput;
import org.wdssii.gui.products.VolumeSliceInput;
import org.wdssii.gui.products.volumes.ProductVolume;
import org.wdssii.gui.volumes.LLHArea;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

import com.sun.opengl.util.j2d.TextRenderer;

/**
 *
 * @author Robert Toomey
 */
public class LLHGLEventListener implements GLEventListener {

    private final static Logger LOG = LoggerFactory.getLogger(LLHGLEventListener.class);
    protected final static int TEXTURE_TARGET = GL.GL_TEXTURE_2D;
    protected int texture = -1;
    protected int texture3d = -1;
    protected ByteBuffer myBuffer = null;
    protected String myCurrentVolumeValueName;
    protected VolumeSlice2DOutput my2DSlice = new VolumeSlice2DOutput();
    protected ProductVolume myVolume = null;
    protected LLHArea myLLHArea;
    protected FilterList myList;
    protected VolumeSliceInput sourceGrid;
    protected String myTitle = "";
    protected String myLLHLabel = "";

    @Override
    public void init(GLAutoDrawable gld) {

        // Init actually called occasionally during resize, not just
        // once as the name implies...so we lazy create stuff in the display method
        GL gl = gld.getGL();
        gl.glClearColor(0.0f, 0.0f, 1.0f, 1.0f);
        LOG.debug("******************OPENGL INIT CALLED");
    }

    public void setData(LLHArea llhArea, ProductVolume volume, FilterList list) {
        myVolume = volume;
        myLLHArea = llhArea;
        myList = list;
    }

    public void updateBufferForTexture() {
    }

    public void setTitle(String t) {
        myTitle = t;
    }

    public FontMetrics getFontMetrics(Font forFont, Graphics g) {
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

    @Override
    public void display(GLAutoDrawable glad) {
     //   StopWatch watch = new StopWatch();
    //    watch.start();
        int w = myNumCols;
        int h = myNumRows;

        // We render a 2D texture into a 2D box inside a 2D ortho projection,
        // at least here.  in 3D we can use the texture but we have to map a lot
        // of texture points to 'stretch' it accurately... 'or' we use quads for
        // complete accuracy, but lose any texture quick abilities like smoothing...

        GL gl = glad.getGL();
        final int ww = glad.getWidth();
        final int wh = glad.getHeight();

        // Text stuff.  Gonna slow us down a bit...
        Font font = new Font("Arial", Font.PLAIN, 14);
        FontMetrics metrics = getFontMetrics(font, null);
        int textHeight = metrics.getMaxAscent() + metrics.getMaxDescent();
        TextRenderer aText = null;
        if (aText == null) {
            aText = new TextRenderer(font, true, true);
        }
        // Get ID handle for the openGL texture...
        if (texture == -1) {
            int[] textures = new int[1];
            gl.glGenTextures(1, textures, 0);
            texture = textures[0];
            updateBufferForTexture();
        }

        if (texture >= 0) {

            // Draw in a 2D world of this width/height
            // (We will probably have legends, text, etc... surrounding slice)
            GLUtil.pushOrtho2D(gl, ww, wh);

            gl.glEnable(TEXTURE_TARGET);

            gl.glBindTexture(TEXTURE_TARGET, texture);

            gl.glPixelStorei(GL.GL_UNPACK_ROW_LENGTH, 0);
            gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);

            // No mipmapping, regular linear
            int filter = GL.GL_NEAREST;
            gl.glTexParameteri(TEXTURE_TARGET, GL.GL_TEXTURE_MAG_FILTER, filter);
            gl.glTexParameteri(TEXTURE_TARGET, GL.GL_TEXTURE_MIN_FILTER, filter);
            int internalFormat = GL.GL_RGBA;
            int dataFmt = GL.GL_RGBA;
            int dataType = GL.GL_UNSIGNED_BYTE;

            // Not sure if we're doing this 100% correctly.  Seems fast enough
            gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, internalFormat, w, h, 0, dataFmt, dataType, myBuffer);
            //gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, internalFormat, w, h, 0, dataFmt, dataType, myBuffer);

            gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);  // Color affects texture
            // Bind the texture with (0-1 coordinate system) to the coordinates of the 2D window area we want to draw at.
            // We have to draw upside down in the Y to render correctly.

            // Margins, if we have the room for them...
            int mr, ml, mt, mb;
            boolean haveMargins;
            if (ww > 40) {
                ml = mr = mt = mb = 20;
                mt = textHeight + 3;
                mb = textHeight + 3;
                haveMargins = true;
            } else {
                ml = mr = mt = mb = 0;
                haveMargins = false;
            }

            // Draw the texture to the 2D area it displayed in *********************************
            // Flip the Y, since opengl upsidedown...
            gl.glBegin(GL.GL_QUADS);
            gl.glTexCoord2f(0f, 0f);
            gl.glVertex2f(ml, wh - mt);

            gl.glTexCoord2f(0f, 1f);
            gl.glVertex2f(ml, mb);

            gl.glTexCoord2f(1f, 1f);
            gl.glVertex2f(ww - mr, mb);

            gl.glTexCoord2f(1f, 0f);
            gl.glVertex2f(ww - mr, wh - mt);
            gl.glEnd();

            gl.glDisable(TEXTURE_TARGET);

            // Erase around the image and border it....
            if (haveMargins) {
                // We need to erase around where the image will be..
                //four boxes...why this?  because cheaper to draw four small boxes
                // than erase entire square due to opengl pixel fill rate
                gl.glColor4f(0.0f, 0.0f, 0.0f, 1.0f);

                gl.glBegin(GL.GL_QUADS);
                // Left bar
                gl.glVertex2f(0, 0);
                gl.glVertex2f(0, wh);
                gl.glVertex2f(ml, wh);
                gl.glVertex2f(ml, 0);

                // Top bar
                gl.glVertex2f(ml, wh - mt);
                gl.glVertex2f(ml, wh);
                gl.glVertex2f(ww - mr, wh);
                gl.glVertex2f(ww - mr, wh - mt);

                // Right bar
                gl.glVertex2f(ww - mr, 0);
                gl.glVertex2f(ww - mr, wh);
                gl.glVertex2f(ww, wh);
                gl.glVertex2f(ww, 0);

                // Bottom bar
                gl.glVertex2f(ml, 0);
                gl.glVertex2f(ml, mb);
                gl.glVertex2f(ww - mr, mb);
                gl.glVertex2f(ww - mr, 0);
                gl.glEnd();

            }
            // Draw a test border
            gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
            gl.glBegin(GL.GL_LINE_LOOP);
            gl.glVertex2d(ml, wh - mt);
            gl.glVertex2d(ml, mr);
            gl.glVertex2d(ww - mr, mb);
            gl.glVertex2d(ww - mr, wh - mt);
            gl.glEnd();


            // Draw all the text labels.
            aText.begin3DRendering();

            // Draw the product title
            aText.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            int textWidth = metrics.stringWidth(myTitle);
            aText.draw(myTitle, ww - mr - textWidth - 2, wh - textHeight - 2);
            textWidth = metrics.stringWidth(myLLHLabel);
            aText.draw(myLLHLabel, ww - mr - textWidth - 2, textHeight);
            aText.end3DRendering();

            // End the 2D World 
            GLUtil.popOrtho2D(gl);
        }
        gl.glFlush();  // for the speed test
     //   watch.stop();
     //   LOG.debug("OPEN_GL_RENDERTIME IS " + watch);
    }

    @Override
    public void reshape(GLAutoDrawable glad, int i, int i1, int i2, int i3) {
        //LOG.debug("reshape called ");
    }

    @Override
    public void displayChanged(GLAutoDrawable glad, boolean bln, boolean bln1) {
        // LOG.debug("display changed called ");
    }

    public void setLLHLabel(String t) {
        myLLHLabel = t;
    }
    
    public void drawGLWorld(GLWorld w) {
    }
}

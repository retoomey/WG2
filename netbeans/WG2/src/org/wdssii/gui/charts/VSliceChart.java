package org.wdssii.gui.charts;

import com.sun.opengl.util.BufferUtil;
import com.sun.opengl.util.j2d.TextRenderer;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLJPanel;
import org.wdssii.core.StopWatch;
import org.wdssii.geom.LLD;
import org.wdssii.gui.Application;
import org.wdssii.gui.GLUtil;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.commands.VolumeSetTypeCommand.VolumeTypeFollowerView;
import org.wdssii.gui.commands.VolumeValueCommand.VolumeValueFollowerView;
import org.wdssii.gui.products.FilterList;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.ProductFeature;
import org.wdssii.gui.products.VolumeSlice2DOutput;
import org.wdssii.gui.products.VolumeSliceInput;
import org.wdssii.gui.products.volumes.ProductVolume;
import org.wdssii.gui.products.volumes.VolumeValue;
import org.wdssii.gui.volumes.LLHArea;
import org.wdssii.gui.volumes.LLHAreaSet;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

/**
 * An attempt at a pure openGL chart. Need to try to get back the sweetnest of
 * the original WDSSII slice and cappi... Plus we need to build a more generic
 * library for future AWIPS2 I think...
 *
 * FIXME: Dispose texture on dispose?
 *
 * @author Robert Toomey
 */
public class VSliceChart extends LLHAreaChartGL implements VolumeValueFollowerView, VolumeTypeFollowerView {

    private final static Logger LOG = LoggerFactory.getLogger(VSliceChart.class);
    private ProductVolume myVolume = null;
    private final static int TEXTURE_TARGET = GL.GL_TEXTURE_2D;
    private Component myComponent = null;
    /**
     * Keep volume value setting per chart
     */
    public String myCurrentVolumeValue = "";
    /**
     * The number of rows or altitudes of the VSlice
     */
    public static final int myNumRows = 150;  //50
    /**
     * The number of cols or change in Lat/Lon
     */
    public static final int myNumCols = 300; //100
    /**
     * Holder for the slice GIS 'state'
     */
    private VolumeSliceInput myCurrentGrid =
            new VolumeSliceInput(myNumRows, myNumCols, 0, 0,
            0, 0, 0, 50);
    private LLD myLeftLocation;
    private LLD myRightLocation;
    
    /** The opengl draw listener */
    private VSlice2DGLEventListener myGLListener = new VSlice2DGLEventListener();

    /**
     * Static method to create, called by reflection. Humm couldn't we just call
     * basic constructor lol a method directly?
     */
    public static VSliceChart create() {

        return new VSliceChart();

    }

    @Override
    public void setCurrentVolumeValue(String changeTo) {
        myCurrentVolumeValue = changeTo;
        if (myVolume != null) {
            updateChart(true); // Force update
        }
    }

    @Override
    public String getCurrentVolumeValue() {
        if (myVolume != null) {
            VolumeValue v = myVolume.getVolumeValue(myCurrentVolumeValue);
            if (v != null) {
                myCurrentVolumeValue = v.getName();
            }
            return myCurrentVolumeValue;
        }
        return "";
    }

    @Override
    public java.util.List<String> getValueNameList() {

        // We get this from the current volume...
        java.util.List<String> s;
        if (myVolume == null) {
            s = new ArrayList<String>();
            s.add("No volume data");
        } else {
            s = myVolume.getValueNameList();
        }

        return s;
    }

    @Override
    public void updateChart(boolean force) {
        LOG.debug("***********************UPDATE CHART CALLED");
        // This is called a lot...during point drag, etc.  We should check to see
        // if anything has CHANGED that needs us to regenerate.

        // The LLHArea is the geometry in the 3d window we are
        // matching our coordinates to.  It can be valid without
        // any product/volume information.
        //myPlot.setCurrentVolumeValueName(myCurrentVolumeValue);
        LLHAreaSet llhArea = getLLHAreaToPlot();
        if (llhArea == null) {
            // If there isn't a 3D slice LLHArea object geometry to follow,
            // clear us...
            myGLListener.setData(null, null, null);
            myGLListener.myTitle = "No slice in 3d window";
            myGLListener.updateBufferForTexture();  // Why do I have to call this?
            if (myComponent != null) {
                myComponent.repaint();
            }
            return;
        }   // No slice to follow, return..
        // myPlot.setLLHArea(llhArea);

        /**
         * Get the GIS key
         */
        String gisKey = llhArea.getGISKey();

        // Sync the height/range axis to the GIS vslice range when updated, this resets
        // any 'zoom' in the chart...but only if GIS key has CHANGED.  This
        // way users can toggle products and keep zoom level for comparison,
        // but if they drag the vslice we reset to full slice.
        if (!getGISKey().equals(gisKey)) {
            // Get the full non-subgrid area and make the distance line axis correct,
            // even if we are missing everything else
            VolumeSliceInput info = llhArea.getSegmentInfo(myCurrentGrid, 0, myNumRows, myNumCols);
            if (info != null) {
                String t = getGISLabel(info.startLat, info.startLon,
                        info.endLat, info.endLon);
                myGLListener.setLLHLabel(t);
                //myDistanceAxis.setFixedRange(new Range(0, llhArea.getRangeKms(0, 1) / 1000.0));
                //myHeightAxis.setFixedRange(new Range(llhArea.getBottomHeightKms() / 1000.0, llhArea.getTopHeightKms() / 1000.0));
            }
        }
        setGISKey(gisKey);

        ProductVolume volume = ProductManager.getCurrentVolumeProduct(getUseProductKey(), getUseVirtualVolume());

        FilterList aList = null;
        String useKey = getUseProductKey();
        String titleKey;
        /**
         * Get the filter list of the product we are following
         */
        ProductFeature tph = ProductManager.getInstance().getProductFeature(useKey);
        Product p = null;
        if (tph != null) {
            aList = tph.getFList();
            p = tph.getProduct();
        }
        if (p != null) {
            titleKey = p.getProductInfoString(false);
        } else {
            titleKey = "No product";
        }

        if ((volume == null) || (aList == null)) {
            // If there isn't a valid data source, clear us out...
            // clear us...
            myVolume = null;
            // CLEAR everything (Blank the slice)
            myGLListener.setData(null, null, null);
            myGLListener.myTitle = "No volume data";
            myGLListener.updateBufferForTexture();  // Why do I have to call this?
            if (myComponent != null) {
                myComponent.repaint();
            }
            return;
        }

        /**
         * Physical key of the Lat/Lon/Height location
         */
        String key = gisKey;

        /**
         * Add volume key
         */
        key += volume.getKey();

        /**
         * Add filter key
         */
        key += aList.getFilterKey(getUseProductFilters());

        boolean keyDifferent = false;
        if (!key.equals(getFullKey())) {
            keyDifferent = true;
        }
        setFullKey(key);

        if (!force && !keyDifferent) {
            return;
        }

        myVolume = volume;

        // REGENERATE the slice....
        // With texture, I think we can use it in the 3D window as well as the 2D,
        // just have to generate the binding points for each...
        if (myGLListener != null) {
            myGLListener.setData(llhArea, volume, aList);
            myGLListener.myTitle = titleKey;
            myGLListener.updateBufferForTexture();  // Why do I have to call this?
            if (myComponent != null) {
                myComponent.repaint();
            }
        }
    }

    public static class VSlice2DGLEventListener implements GLEventListener {

        private int texture = -1;
        private ByteBuffer myBuffer = null;
        private int imageWidth = 0;
        private int imageHeight = 0;
        private boolean flip = false;
        private String myCurrentVolumeValueName;
        private VolumeSlice2DOutput my2DSlice = new VolumeSlice2DOutput();
        private ProductVolume myVolume = null;
        private LLHArea myLLHArea;
        private FilterList myList;
        private VolumeSliceInput sourceGrid;
        private String myTitle = "";
        private String myLLHLabel = "";

        @Override
        public void init(GLAutoDrawable gld) {

            // Init actually called occasionally during resize, not just
            // once as the name implies...so we lazy create stuff in the display method
            GL gl = gld.getGL();
            gl.glClearColor(0.0f, 0.0f, 1.0f, 1.0f);
        }

        public void setData(LLHArea llhArea, ProductVolume volume, FilterList list) {
            myVolume = volume;
            myLLHArea = llhArea;
            myList = list;
        }

        public void updateBufferForTexture() {
            imageWidth = myNumCols;
            imageHeight = myNumRows;
            final int total = imageWidth * imageHeight;

            StopWatch watch = new StopWatch();
            watch.start();
            ByteBuffer buffer;
            if (myBuffer != null) {  // FIXME: AND THE SIZE WE NEED
                buffer = myBuffer;  // AT THE MOMENT ASSUMING NEVER CHANGES
            } else {
                buffer = BufferUtil.newByteBuffer(total * 4);
            }

            if ((myVolume != null) && (myLLHArea != null)) {
                VolumeValue v = myVolume.getVolumeValue(myCurrentVolumeValueName);
                if (v != null) {
                    myCurrentVolumeValueName = v.getName();
                }
                //sourceGrid = myLLHArea.getSegmentInfo(null, 0, myNumRows, myNumCols);
                sourceGrid = myLLHArea.getSegmentInfo(null, 0, myNumRows, myNumCols);

                myVolume.generate2DGrid(sourceGrid, my2DSlice, myList, false, v);
                int[] data = my2DSlice.getColor2dFloatArray(0);

                int counter = 0;
                for (int i = 0; i < total; i++) {
                    int pixel = data[counter++];
                    buffer.put((byte) ((pixel >>> 16) & 0xFF));     // Red component
                    buffer.put((byte) ((pixel >>> 8) & 0xFF));      // Green component
                    buffer.put((byte) (pixel & 0xFF));               // Blue component
                    // buffer.put((byte) ((pixel >> 24) & 0xFF));    // Alpha component. Only for RGBA
                    buffer.put((byte) 255);
                }
            } else {
                for (int i = 0; i < total; i++) {
                    buffer.put((byte) 255);
                    buffer.put((byte) 0);
                    buffer.put((byte) 0);
                    buffer.put((byte) 255);
                }
            }
            flip = !flip;

            buffer.flip();
            myBuffer = buffer;
            watch.stop();
            LOG.debug("VSlice GENERATION TIME IS " + watch);
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
            StopWatch watch = new StopWatch();
            watch.start();
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
                aText.draw(myTitle, ww - mr - textWidth-2, wh - textHeight - 2);
                textWidth = metrics.stringWidth(myLLHLabel);
                aText.draw(myLLHLabel, ww - mr - textWidth-2, textHeight);
                aText.end3DRendering();
                
                // End the 2D World 
                GLUtil.popOrtho2D(gl);
            }
            gl.glFlush();  // for the speed test
            watch.stop();
            LOG.debug("OPEN_GL_RENDERTIME IS " + watch);
        }

        @Override
        public void reshape(GLAutoDrawable glad, int i, int i1, int i2, int i3) {
            //LOG.debug("reshape called ");
        }

        @Override
        public void displayChanged(GLAutoDrawable glad, boolean bln, boolean bln1) {
            // LOG.debug("display changed called ");
        }

        private void setLLHLabel(String t) {
                myLLHLabel = t;
        }
    }

    /**
     * Generate the Chart itself. Basically the stuff that will draw the chart
     * in the composite
     */
    @Override
    public Object getNewGUIForChart(Object parent) {
        // FIXME: maybe some text? "Hey this isn't working?"
        //Composite nothing = new Composite((Composite)parent, SWT.NONE);
        //return nothing;
        // JPanel test = new JPanel();

        // test.setBackground(Color.red);
        boolean heavy = Application.USE_HEAVYWEIGHT_GL;

        // Humm...let's try to use true and deal with the effects for now,
        // need to check frame rate vs panel
        heavy = false;
        // myGLListener = new VSlice2DGLEventListener();

        if (heavy) {
            GLCanvas glCanvas = new GLCanvas();
            // GLCapabilities glcaps = new GLCapabilities();
            // GLCanvas glcanvas =  jogl 2?
            //        GLDrawableFactory.getFactory().createGLCanvas(glcaps);
            glCanvas.addGLEventListener(myGLListener);
            myComponent = glCanvas;
            return glCanvas;

        } else {
            GLJPanel glPanel = new GLJPanel();
            glPanel.addGLEventListener(myGLListener);
            myComponent = glPanel;
            return glPanel;

        }
    }
}

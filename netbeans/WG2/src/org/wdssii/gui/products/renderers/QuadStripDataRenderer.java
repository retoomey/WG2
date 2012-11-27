package org.wdssii.gui.products.renderers;

import gov.nasa.worldwind.render.DrawContext;
import java.nio.FloatBuffer;
import java.util.Iterator;
import javax.media.opengl.GL;
import org.wdssii.storage.Array1DOpenGL;
import org.wdssii.storage.GrowList;

/**
 * Quad Strip Data Renderer draws a product as a series of quad strips. It
 * implements a data readout with a special color buffer
 *
 * @author Robert Toomey
 */
public class QuadStripDataRenderer extends ProductRenderer {

    /**
     * Offsets for the quad strips
     */
    protected GrowList<Integer> myOffsets;
    /**
     * Verts for the quads
     */
    protected Array1DOpenGL verts;
    /**
     * Corresponding colors
     */
    protected Array1DOpenGL colors;
    /**
     * Colors as readout information
     */
    protected Array1DOpenGL readout;

    public QuadStripDataRenderer(boolean asBackgroundJob) {
        super(asBackgroundJob);
    }

    /**
     *
     * @param dc Draw context in opengl for drawing our radial set
     */
    @Override
    public void draw(DrawContext dc) {
        drawData(dc, false);
    }

    /**
     *
     * @param dc Draw context in opengl for drawing our radial set
     */
    public void drawData(DrawContext dc, boolean readoutMode) {
        if (isCreated() && (verts != null) && (colors != null)) {
            GL gl = dc.getGL();

            boolean attribsPushed = false;
            try {
                Object lock1 = verts.getBufferLock();
                Object lock2 = readoutMode ? readout.getBufferLock() : colors.getBufferLock();
                synchronized (lock1) {
                    synchronized (lock2) {

                        gl.glPushAttrib(GL.GL_DEPTH_BUFFER_BIT | GL.GL_LIGHTING_BIT
                                | GL.GL_COLOR_BUFFER_BIT
                                | GL.GL_ENABLE_BIT
                                | GL.GL_TEXTURE_BIT | GL.GL_TRANSFORM_BIT
                                | GL.GL_VIEWPORT_BIT | GL.GL_CURRENT_BIT);

                        gl.glDisable(GL.GL_LIGHTING);
                        gl.glDisable(GL.GL_TEXTURE_2D);

                        if (readoutMode) {
                            gl.glDisable(GL.GL_DEPTH_TEST);

                        } else {
                            gl.glEnable(GL.GL_DEPTH_TEST);
                        }
                        gl.glShadeModel(GL.GL_FLAT);
                        gl.glPushClientAttrib(GL.GL_CLIENT_VERTEX_ARRAY_BIT
                                | GL.GL_CLIENT_PIXEL_STORE_BIT);
                        gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
                        gl.glEnableClientState(GL.GL_COLOR_ARRAY);
                        attribsPushed = true;
                        FloatBuffer z = verts.getRawBuffer();
                        FloatBuffer c = readoutMode ? readout.getRawBuffer() : colors.getRawBuffer();

                        // Only render if there is data to render
                        if ((z != null) && (z.capacity() > 0)) {
                            gl.glVertexPointer(3, GL.GL_FLOAT, 0, z.rewind());

                            // Isn't this color kinda wasteful really?  We have 4 floats per color,
                            // or 4 bytes * 4 = 16 bytes, when GL only stores 4 bytes per color lol
                            // We should use GL.GL_BYTE and convert ourselves to it, will save memory...
                            gl.glColorPointer(4, GL.GL_UNSIGNED_BYTE, 0, c.rewind());

                            Iterator<Integer> itr = myOffsets.iterator();
                            if (itr.hasNext()) {
                                Integer now = itr.next();
                                while (itr.hasNext()) {
                                    Integer plus1 = itr.next();
                                    if (plus1 != null) {
                                        int start_index = now;
                                        int end_index = plus1;
                                        int run_indices = end_index - start_index;
                                        int start_vertex = start_index / 3;
                                        int run_vertices = run_indices / 3;
                                        gl.glDrawArrays(GL.GL_QUAD_STRIP, start_vertex,
                                                run_vertices);
                                        now = plus1;
                                    }
                                }
                            }

                        }
                    }
                }
            } finally {
                if (attribsPushed) {
                    gl.glPopClientAttrib();
                    gl.glPopAttrib();
                }
            }
        }
    }

    @Override
    public boolean canOverlayOtherData() {
        return false;
    }
}

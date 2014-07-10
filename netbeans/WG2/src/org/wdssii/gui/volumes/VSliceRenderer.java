package org.wdssii.gui.volumes;

import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.airspaces.Geometry;

import java.nio.Buffer;

import javax.media.opengl.GL;
import org.wdssii.gui.GLWorld;

import org.wdssii.gui.products.VolumeSlice3DOutput;
import org.wdssii.gui.worldwind.GLWorldWW;

/** This draws the vslice using an airspace Polygon from WorldWind
 * FIXME: move into products so that subclasses of products can render differently...
 * FIXME: Uses DrawContext from worldwind, might be better to pass raw opengl so if we need
 * to port to another platform we can
 * @author Robert Toomey
 *
 */
public class VSliceRenderer {

    public VSliceRenderer() {
        super();
    }

    /** Special render our vslice */
    public void drawVSlice(GLWorld w, VolumeSlice3DOutput vslice) {
        // We don't use reference center with our vslice, helps with speed quite a bit.
        final DrawContext dc = ((GLWorldWW)(w)).getDC(); // hack

        if (dc.isPickingMode()) {  // With picking mode, just fill us in with full square for picking.
            drawGeometry(dc, vslice.getFillIndexGeometry(), vslice.getVertexGeometry());
        } else {
            GL gl = dc.getGL();

            gl.glShadeModel(GL.GL_FLAT);
            gl.glDisable(GL.GL_CULL_FACE);
            gl.glDisable(GL.GL_LIGHTING);

            gl.glPushClientAttrib(GL.GL_CLIENT_VERTEX_ARRAY_BIT
                    | GL.GL_CLIENT_PIXEL_STORE_BIT);
            gl.glEnableClientState(GL.GL_COLOR_ARRAY);
            gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
            gl.glDisableClientState(GL.GL_NORMAL_ARRAY);

            gl.glShadeModel(GL.GL_FLAT);
            gl.glDisable(GL.GL_CULL_FACE);
            gl.glDisable(GL.GL_LIGHTING);

            if (vslice.hasVSliceData()) {
                vslice.setupColorPointer(gl);
                vslice.setupVertexPointer(gl);
                vslice.drawElementBuffer(gl);
            } else {
                // Fill in with color (no vslice data, but still want something
                // visible....
                gl.glDisableClientState(GL.GL_COLOR_ARRAY);
                dc.getGL().glColor4f(1.0f, 0.0f, 0.0f, 0.50f);
                drawGeometry(dc, vslice.getFillIndexGeometry(), vslice.getVertexGeometry());
            }

            // Border outline from superclass (How to color it?) FIXME: Make a subdivison based legend
            // drawGeometry(dc, vslice.getOutlineIndexGeometry(), vslice.getVertexGeometry());
            gl.glPopClientAttrib();
        }
    }

    private void drawGeometry(DrawContext dc, Geometry elementGeom, Geometry vertexGeom) {
        int mode, count, type;
        Buffer elementBuffer;

        mode = elementGeom.getMode(Geometry.ELEMENT);
        count = elementGeom.getCount(Geometry.ELEMENT);
        type = elementGeom.getGLType(Geometry.ELEMENT);
        elementBuffer = elementGeom.getBuffer(Geometry.ELEMENT);

        GL gl = dc.getGL();

        int minElementIndex, maxElementIndex;
        int size, glType, stride;
        Buffer vertexBuffer; //, normalBuffer;

        size = vertexGeom.getSize(Geometry.VERTEX);
        glType = vertexGeom.getGLType(Geometry.VERTEX);
        stride = vertexGeom.getStride(Geometry.VERTEX);
        vertexBuffer = vertexGeom.getBuffer(Geometry.VERTEX);
        gl.glVertexPointer(size, glType, stride, vertexBuffer);

        // On some hardware, using glDrawRangeElements allows vertex data to be prefetched. We know the minimum and
        // maximum index values that are valid in elementBuffer (they are 0 and vertexCount-1), so it's harmless
        // to use this approach and allow the hardware to optimize.
        minElementIndex = 0;
        maxElementIndex = vertexGeom.getCount(Geometry.VERTEX) - 1;
        gl.glDrawRangeElements(mode, minElementIndex, maxElementIndex, count, type, elementBuffer);
    }
}

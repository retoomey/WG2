package org.wdssii.gui.volumes;

import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.airspaces.Geometry;

import java.nio.Buffer;

import javax.media.opengl.GL;

import org.wdssii.gui.products.VolumeSlice3DOutput;

/** This draws the vslice using an airspace Polygon from WorldWind
 * FIXME: move into products so that subclasses of products can render differently...
 * FIXME: Uses DrawContext from worldwind, might be better to pass raw opengl so if we need
 * to port to another platform we can
 * @author Robert Toomey
 *
 */
public class VSliceRenderer// extends AirspaceRenderer 
{

    private LLHAreaSlice mySlice;
    protected static final int ELEMENT = 1;
    protected static final int VERTEX = 2;
    protected static final int NORMAL = 3;

    public VSliceRenderer(LLHAreaSlice theSlice) {
        super();
        mySlice = theSlice;
    }

    /** Special render our vslice */
    public void drawVSlice(DrawContext dc, VolumeSlice3DOutput geom) {
        // We don't use reference center with our vslice, helps with speed quite a bit.

        if (dc.isPickingMode()) {  // With picking mode, just fill us in with full square for picking.
            drawGeometry(dc, geom.getFillIndexGeometry(), geom.getVertexGeometry());
        } else {
            // Draw vslice geometry
            drawVSliceGeometry(dc, geom);

            //	geom.getVSliceFillIndexGeometry(), geom.getVSliceVertexGeometry());		

            // Border outline from superclass (How to color it?) FIXME: Make a subdivison based legend
            //	dc.getGL().glColor3f(1.0f, 0.0f, 0.0f);
            drawGeometry(dc, geom.getOutlineIndexGeometry(), geom.getVertexGeometry());
        }
    }

    public void drawVSliceGeometry(DrawContext dc, VolumeSlice3DOutput vslice) {
        GL gl = dc.getGL();

        gl.glEnableClientState(GL.GL_COLOR_ARRAY);
        gl.glShadeModel(GL.GL_FLAT);
        gl.glDisable(GL.GL_CULL_FACE);
        gl.glDisable(GL.GL_LIGHTING);

        vslice.setupColorPointer(gl);
        vslice.setupVertexPointer(gl);
        if (!dc.isPickingMode()) {
            gl.glDisableClientState(GL.GL_NORMAL_ARRAY);
        }

        vslice.drawElementBuffer(gl);

        gl.glDisableClientState(GL.GL_COLOR_ARRAY);
    }

    public void drawGeometry(DrawContext dc, Geometry elementGeom, Geometry vertexGeom) {
        int mode, count, type;
        Buffer elementBuffer;

        mode = elementGeom.getMode(ELEMENT);
        count = elementGeom.getCount(ELEMENT);
        type = elementGeom.getGLType(ELEMENT);
        elementBuffer = elementGeom.getBuffer(ELEMENT);

        this.drawGeometry(dc, mode, count, type, elementBuffer, vertexGeom);
    }

    public void drawGeometry(DrawContext dc, int mode, int count, int type, Buffer elementBuffer, Geometry geom) {
        GL gl = dc.getGL();

        int minElementIndex, maxElementIndex;
        int size, glType, stride;
        Buffer vertexBuffer; //, normalBuffer;

        size = geom.getSize(VERTEX);
        glType = geom.getGLType(VERTEX);
        stride = geom.getStride(VERTEX);
        vertexBuffer = geom.getBuffer(VERTEX);
        gl.glVertexPointer(size, glType, stride, vertexBuffer);

        // On some hardware, using glDrawRangeElements allows vertex data to be prefetched. We know the minimum and
        // maximum index values that are valid in elementBuffer (they are 0 and vertexCount-1), so it's harmless
        // to use this approach and allow the hardware to optimize.
        minElementIndex = 0;
        maxElementIndex = geom.getCount(VERTEX) - 1;
        gl.glDrawRangeElements(mode, minElementIndex, maxElementIndex, count, type, elementBuffer);
    }
}

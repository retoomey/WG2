package org.wdssii.gui;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;
import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import javax.media.opengl.GL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.core.WdssiiJob;
import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;
import org.wdssii.core.WdssiiJob.WdssiiJobStatus;
import org.wdssii.geom.Location;
import org.wdssii.gui.features.PolarGridFeature.PolarGridMemento;
import org.wdssii.storage.Array1Dfloat;
import org.wdssii.storage.Array1DfloatAsNodes;
import org.wdssii.util.RadialUtil;

/**
 *
 * @author Robert Toomey
 *
 * Uses geotools to create/render a shapefile map in a worldwind window.
 * Currently only looking at JTS MultiPolygon (Polygon) objects and creating
 * line strips from them. @todo Add other types, clean up code, etc...
 *
 */
public class PolarGridRenderer {

    private static Logger log = LoggerFactory.getLogger(PolarGridRenderer.class);
    /**
     * Offset array, one per Polygon
     */
    private ArrayList<Integer> myOffsets;
    /**
     * Verts for the map. Single for now (monster maps may fail)
     */
    protected Array1Dfloat polygonData;
    /**
     * The current center location of this PolarGrid
     */
    protected LatLon myCenter = LatLon.fromDegrees(35.3331, -97.2778);
    /**
     * The current elevation in degrees of this PolarGrid
     */
    protected double myElevationDegrees = 0.5d;
    private boolean tryToLoad = true;

    public PolarGridRenderer() {
    }
    /**
     * The worker job if we are threading
     */
    private backgroundRender myWorker = null;
    /**
     * Volatile because renderer job creates the data in one thread, but drawn
     * in another. myCreated set to true by worker thread after the buffers are
     * created (draw allowed)
     */
    private volatile boolean myCreated = false;

    /**
     * Is this map fully created?
     */
    public synchronized boolean isCreated() {
        return myCreated;
    }

    /**
     * Get if this map if fully created
     */
    public synchronized void setIsCreated() {
        myCreated = true;
    }

    /**
     * Job for creating in the background any rendering
     */
    public class backgroundRender extends WdssiiJob {

        public DrawContext dc;
        // FIXME: these params should be contained...
        public LatLon center;
        public double elevDeg;

        public backgroundRender(String jobName, DrawContext aDc, LatLon c,
                double elevDegrees) {
            super(jobName);
            dc = aDc;
            center = c;
            elevDeg = elevDegrees;
        }

        @Override
        public WdssiiJobStatus run(WdssiiJobMonitor monitor) {
            return create(dc, center, elevDeg, monitor);
        }
    }

    public void setupIfNeeded(DrawContext dc) {
        if (tryToLoad) {
            init(dc, myCenter, myElevationDegrees);
            tryToLoad = false;
        }
    }

    public void init(DrawContext dc, LatLon center, double elevDegrees) {

        // FIXME: handle background flag?
        if (myWorker == null) {
            myWorker = new backgroundRender("Job", dc, center, elevDegrees);
            myWorker.schedule();
        }
    }

    /**
     * Draw the product in the current dc. FIXME: Shared code with map renderer
     * right now.....do we merge some of these classes or make util functions?
     */
    public void draw(DrawContext dc, PolarGridMemento m) {
        if (isCreated() && (polygonData != null)) {
            GL gl = dc.getGL();
            Color line = m.getLineColor();
            final float r = line.getRed() / 255.0f;
            final float g = line.getGreen() / 255.0f;
            final float b = line.getBlue() / 255.0f;
            final float a = line.getAlpha() / 255.0f;
            boolean attribsPushed = false;
            try {
                Object lock1 = polygonData.getBufferLock();
                synchronized (lock1) {

                    gl.glPushAttrib(GL.GL_DEPTH_BUFFER_BIT | GL.GL_LIGHTING_BIT
                            | GL.GL_COLOR_BUFFER_BIT
                            | GL.GL_ENABLE_BIT
                            | GL.GL_TEXTURE_BIT | GL.GL_TRANSFORM_BIT
                            | GL.GL_VIEWPORT_BIT | GL.GL_CURRENT_BIT
                            | GL.GL_LINE_BIT);

                    gl.glDisable(GL.GL_LIGHTING);
                    gl.glDisable(GL.GL_TEXTURE_2D);
                    gl.glDisable(GL.GL_DEPTH_TEST);
                    gl.glShadeModel(GL.GL_FLAT);
                    gl.glPushClientAttrib(GL.GL_CLIENT_VERTEX_ARRAY_BIT
                            | GL.GL_CLIENT_PIXEL_STORE_BIT);
                    gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
                    // gl.glEnableClientState(GL.GL_COLOR_ARRAY);
                    attribsPushed = true;
                    FloatBuffer z = polygonData.getRawBuffer();

                    // Only render if there is data to render
                    if ((z != null) && (z.capacity() > 0)) {
                        gl.glColor4f(r, g, b, a);
                        gl.glLineWidth(m.getLineThickness());
                        gl.glVertexPointer(3, GL.GL_FLOAT, 0, z.rewind());
                        int size = myOffsets.size();
                        if (size > 1) {
                            for (int i = 0; i < size - 1; i++) {
                                int start_index = myOffsets.get(i);
                                int end_index = myOffsets.get(i + 1);
                                int run_indices = end_index - start_index;
                                int start_vertex = start_index / 3;
                                int run_vertices = run_indices / 3;
                                gl.glDrawArrays(GL.GL_LINE_LOOP, start_vertex,
                                        run_vertices);
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

    /**
     * Pick an object in the current dc at point
     */
    public void doPick(DrawContext dc, java.awt.Point pickPoint) {
    }

    /**
     * Do the work of generating the OpenGL stuff
     */
    public WdssiiJobStatus create(DrawContext dc, LatLon center, double elevDeg, WdssiiJobMonitor monitor) {

        Globe myGlobe = dc.getGlobe();
        myOffsets = new ArrayList<Integer>();
        myOffsets.add(0);  // Just one for now
        int idx = 0;
        Location ourCenter = new Location(myCenter.getLatitude().degrees,
                myCenter.getLongitude().degrees, 0);

        int myMaxR = 500; // firstgate+(numgates*gatewidth)....for a radialset
        int ringApart = 50;
        int numCircles;
        if (ringApart > myMaxR) {
            numCircles = 1;
        } else {
            numCircles = (int) (0.5 + ((float) myMaxR) / ringApart);
        }
        final double sinElevAngle = Math.sin(Math.toRadians(myElevationDegrees));
        final double cosElevAngle = Math.cos(Math.toRadians(myElevationDegrees));
        final int numSegments = 10;

        Location l = new Location(35, 35, 10);
        double circleHeightKms = 0;
        
        int maxDegree = 360; // Make sure no remainder for numSegs...
        int degStep = 6;
        int numSegs = maxDegree/degStep;
        
         // Allocate memory...
        polygonData = new Array1DfloatAsNodes(numCircles * numSegs * 3, 0.0f);
        
        int rangeMeters = ringApart;
        for (int c = 0; c < numCircles; c++) {

            for (int d = 0; d < maxDegree; d+=degStep) {
                double sinAzimuthRAD = Math.sin(Math.toRadians(d));
                double cosAzimuthRAD = Math.cos(Math.toRadians(d));
                RadialUtil.getAzRanElLocation(l, ourCenter,
                        sinAzimuthRAD, cosAzimuthRAD, rangeMeters,
                        sinElevAngle, cosElevAngle);
                Vec4 point = myGlobe.computePointFromPosition(
                        Angle.fromDegrees(l.getLatitude()),
                        Angle.fromDegrees(l.getLongitude()),
                        l.getHeightKms()*1000.0);
                polygonData.set(idx++, (float) point.x);
                polygonData.set(idx++, (float) point.y);
                polygonData.set(idx++, (float) point.z);

            }
            myOffsets.add(idx);
            rangeMeters += ringApart;
        }
        myCreated = true;
        return WdssiiJobStatus.OK_STATUS;
    }
}
